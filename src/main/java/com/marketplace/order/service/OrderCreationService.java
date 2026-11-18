package com.marketplace.order.service;

import com.marketplace.cart.entity.Cart;
import com.marketplace.cart.entity.CartItem;
import com.marketplace.cart.repository.CartRepository;
import com.marketplace.common.exception.BusinessRuleException;
import com.marketplace.common.exception.ConflictException;
import com.marketplace.common.exception.ResourceNotFoundException;
import com.marketplace.delivery.entity.StoreDeliveryZone;
import com.marketplace.delivery.service.DeliveryAvailabilityService;
import com.marketplace.inventory.service.StockReservationService;
import com.marketplace.order.dto.request.CreateOrderRequest;
import com.marketplace.order.dto.request.DeliveryDetails;
import com.marketplace.order.dto.response.OrderResponse;
import com.marketplace.order.entity.*;
import com.marketplace.order.mapper.OrderMapper;
import com.marketplace.order.repository.OrderRepository;
import com.marketplace.product.entity.Product;
import com.marketplace.product.repository.ProductVariantRepository;
import com.marketplace.promotion.service.PromocodeService;
import com.marketplace.product.entity.ProductStatus;
import com.marketplace.seller.entity.SellerStatus;
import com.marketplace.store.entity.Store;
import com.marketplace.user.entity.User;
import com.marketplace.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderCreationService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final StockReservationService  stockReservationService;
    private final OrderHistoryService orderHistory;
    private final PromocodeService promocodeService;
    private final ProductVariantRepository variantRepository;
    private final DeliveryAvailabilityService  deliveryAvailabilityService;
    @Value("${order.reservation-ttl:PT30M}")
    private Duration reservationTtl;

    @Transactional
    public OrderResponse createOrder(String username, String idempotencyKey, CreateOrderRequest request) {
        if (request == null || request.delivery() == null) {
            throw new BusinessRuleException("Delivery details are required");
        }
        DeliveryDetails delivery = normalizeDelivery(request.delivery());
        String promocode = request.promocode();
        String key = idempotencyKey == null ? null : idempotencyKey.trim();
        if (key != null && (key.isEmpty() || key.length() > 128)) {
            throw new BusinessRuleException("Idempotency-Key must contain 1 to 128 characters");
        }
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Cart cart = cartRepository.findByUserIdForUpdate(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cart with this user id " + user.getId() + " not found"
                ));

        if (key != null) {
            var previous = orderRepository.findByBuyer_IdAndIdempotencyKey(user.getId(), key);
            if (previous.isPresent()) {
                Order existing = previous.get();
                if (!Objects.equals(existing.getRecipientName(), delivery.recipientName())
                        || !Objects.equals(existing.getRecipientPhone(), delivery.recipientPhone())
                        || !Objects.equals(existing.getDeliveryRegion(), delivery.region())
                        || !Objects.equals(existing.getDeliveryCity(), delivery.city())
                        || !Objects.equals(existing.getDeliveryAddressLine(), delivery.addressLine())
                        || !Objects.equals(existing.getPromocode() == null ? null : existing.getPromocode().getCode(),
                                promocode == null || promocode.isBlank() ? null : promocode.trim().toUpperCase(Locale.ROOT))) {
                    throw new ConflictException("Idempotency-Key was already used with different checkout details");
                }
                return orderMapper.toOrderResponse(previous.get());
            }
        }

        if (cart.getItems().isEmpty()) {
            throw new BusinessRuleException("Cart items cannot be empty");
        }
        for (CartItem cartItem : cart.getItems()) {
            if (cartItem.getProduct().getStatus()!=ProductStatus.PUBLISHED){
                throw new BusinessRuleException("Product status must be PUBLISHED");
            }else if (!cartItem.getProduct().getStore().isActive()
                    || !cartItem.getProduct().getStore().isApproved()){
                throw new BusinessRuleException("Product store must be ACTIVE");
            }else if (cartItem.getProduct().getStore().getSeller().getStatus()!=SellerStatus.APPROVED){
                throw new BusinessRuleException("Store status must be APPROVED");
            }else if (cartItem.getQuantity()<=0){
                throw new BusinessRuleException("Quantity must be greater than 0");
            }else if (cartItem.getProduct().isModerationBlocked()) {
                throw new BusinessRuleException("Product is blocked");
            } else if (cartItem.getVariant() != null
                    && (!cartItem.getVariant().isActive()
                        || !cartItem.getVariant().getProduct().getId().equals(cartItem.getProduct().getId()))) {
                throw new BusinessRuleException("Variant is not available");
            } else if (cartItem.getVariant() == null
                    && variantRepository.existsByProduct_Id(cartItem.getProduct().getId())) {
                throw new BusinessRuleException("Select a variant for this product");
            } else if ((cartItem.getVariant() == null ? cartItem.getProduct().getPrice()
                    : cartItem.getVariant().getPrice()).compareTo(BigDecimal.ZERO)<=0){
                throw new BusinessRuleException("Price must be greater than 0");
            }
        }
        Map<UUID, List<CartItem>> groupedItems=cart.getItems()
                .stream()
                .collect(Collectors.groupingBy(cartItem -> cartItem.getProduct().getStore().getId()));
        Map<UUID, StoreDeliveryZone> deliveryZones=deliveryAvailabilityService.validateDelivery(delivery.region(),groupedItems.keySet());
        Order order=new Order();
        order.setBuyer(user);
        order.setRecipientName(delivery.recipientName());
        order.setRecipientPhone(delivery.recipientPhone());
        order.setDeliveryRegion(delivery.region());
        order.setDeliveryCity(delivery.city());
        order.setDeliveryAddressLine(delivery.addressLine());
        order.setStatus(OrderStatus.PENDING);
        order.setIdempotencyKey(key);
        order.setExpiresAt(Instant.now().plus(reservationTtl));
        BigDecimal subtotalAmount = BigDecimal.ZERO;
        BigDecimal deliveryTotalAmount = BigDecimal.ZERO;

        for (Map.Entry<UUID, List<CartItem>> entry : groupedItems.entrySet()) {
            UUID storeId = entry.getKey();
            StoreDeliveryZone zone = deliveryZones.get(storeId);
            if (zone == null) {
                throw new BusinessRuleException("Delivery zone not found for store");
            }
            List<CartItem> storeItems = entry.getValue();

            Store store=storeItems.get(0)
                    .getProduct()
                    .getStore();
            SellerOrder sellerOrder=new SellerOrder();
            sellerOrder.setOrder(order);
            sellerOrder.setStore(store);
            sellerOrder.setStatus(SellerOrderStatus.PENDING);
            sellerOrder.setDeliveryFee(zone.getDeliveryFee());
            sellerOrder.setMinDeliveryDays(zone.getMinDeliveryDays());
            sellerOrder.setMaxDeliveryDays(zone.getMaxDeliveryDays());
            deliveryTotalAmount = deliveryTotalAmount.add(zone.getDeliveryFee());
            sellerOrder.setTotalAmount(BigDecimal.ZERO);
            BigDecimal sellerTotal=BigDecimal.ZERO;

            for (CartItem cartItem : storeItems) {
                Product product=cartItem.getProduct();
                OrderItem orderItem=new OrderItem();
                orderItem.setOrder(order);
                orderItem.setSellerOrder(sellerOrder);
                orderItem.setProduct(product);
                orderItem.setVariant(cartItem.getVariant());
                orderItem.setSku(cartItem.getVariant() == null ? null : cartItem.getVariant().getSku());
                orderItem.setProductName(cartItem.getVariant() == null ? product.getName()
                        : product.getName() + " — " + cartItem.getVariant().getName());
                orderItem.setQuantity(cartItem.getQuantity());
                orderItem.setUnitPrice(cartItem.getVariant() == null ? product.getPrice()
                        : cartItem.getVariant().getPrice());

                BigDecimal unitPrice=cartItem.getVariant() == null ? product.getPrice()
                        : cartItem.getVariant().getPrice();
                orderItem.setUnitPrice(unitPrice);
                BigDecimal subtotal=unitPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity()));
                orderItem.setSubtotal(subtotal);
                sellerOrder.getItems().add(orderItem);
                sellerTotal=sellerTotal.add(subtotal);
            }
            sellerOrder.setTotalAmount(sellerTotal);
            order.getSellerOrders().add(sellerOrder);
            subtotalAmount = subtotalAmount.add(sellerTotal);
        }
        order.setSubtotalAmount(subtotalAmount);
        order.setDeliveryTotalAmount(deliveryTotalAmount);
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setTotalAmount(subtotalAmount.add(deliveryTotalAmount));
        Order savedOrder=orderRepository.saveAndFlush(order);

        if (promocode != null && !promocode.isBlank()) {
            promocodeService.apply(promocode, subtotalAmount, user, savedOrder);
            savedOrder.setTotalAmount(savedOrder.getSubtotalAmount()
                    .add(savedOrder.getDeliveryTotalAmount())
                    .subtract(savedOrder.getDiscountAmount()));
        }

        orderHistory.record(savedOrder, null, null, OrderStatus.PENDING.name(), "CUSTOMER", "Order created");
        for (SellerOrder part : savedOrder.getSellerOrders()) {
            orderHistory.record(savedOrder, part, null, SellerOrderStatus.PENDING.name(),
                    "CUSTOMER", "Seller order created");
        }

        for (SellerOrder sellerOrder : savedOrder.getSellerOrders()) {
            for (OrderItem orderItem : sellerOrder.getItems()) {
                stockReservationService.createReservation(orderItem);
            }
        }

        for (CartItem cartItem : cart.getItems()) {
            cartItem.setCart(null);
        }
        cart.getItems().clear();
        return orderMapper.toOrderResponse(savedOrder);
    }

    private DeliveryDetails normalizeDelivery(DeliveryDetails delivery) {
        String name = required(delivery.recipientName(), 100, "Recipient name");
        String phone = required(delivery.recipientPhone(), 30, "Recipient phone");
        String region = required(delivery.region(), 32, "Delivery region code");
        String city = required(delivery.city(), 100, "Delivery city");
        String address = required(delivery.addressLine(), 500, "Delivery address");
        if (!phone.matches("[+0-9() .-]{7,30}")) {
            throw new BusinessRuleException("Recipient phone has an invalid format");
        }
        return new DeliveryDetails(name, phone, region, city, address);
    }

    private String required(String value, int maxLength, String field) {
        if (value == null || value.isBlank()) {
            throw new BusinessRuleException(field + " is required");
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new BusinessRuleException(field + " is too long");
        }
        return normalized;
    }

}
