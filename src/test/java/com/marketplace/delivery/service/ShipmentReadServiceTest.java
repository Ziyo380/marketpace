package com.marketplace.delivery.service;

import com.marketplace.delivery.entity.Shipment;
import com.marketplace.delivery.repository.ShipmentRepository;
import com.marketplace.order.entity.Order;
import com.marketplace.order.entity.SellerOrder;
import com.marketplace.order.repository.OrderRepository;
import com.marketplace.order.repository.SellerOrderRepository;
import com.marketplace.seller.entity.Seller;
import com.marketplace.store.entity.Store;
import com.marketplace.user.entity.User;
import com.marketplace.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShipmentReadServiceTest {
    @Mock ShipmentRepository shipmentRepository;
    @Mock SellerOrderRepository sellerOrderRepository;
    @Mock OrderRepository orderRepository;
    @Mock UserRepository userRepository;
    @InjectMocks ShipmentService shipmentService;

    @Test
    void sellerCannotReadAnotherStoresShipment() {
        UUID sellerOrderId = UUID.randomUUID();
        User owner = user();
        User otherSeller = user();
        Seller seller = new Seller();
        seller.setUser(owner);
        Store store = new Store();
        store.setSeller(seller);
        SellerOrder sellerOrder = new SellerOrder();
        sellerOrder.setStore(store);
        Shipment shipment = new Shipment();
        shipment.setSellerOrder(sellerOrder);

        when(shipmentRepository.findBySellerOrder_Id(sellerOrderId)).thenReturn(Optional.of(shipment));
        when(userRepository.findByUsername("other-seller")).thenReturn(Optional.of(otherSeller));

        assertThatThrownBy(() -> shipmentService.getSellerShipment("other-seller", sellerOrderId))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void customerCannotReadAnotherBuyersShipments() {
        UUID orderId = UUID.randomUUID();
        Order order = new Order();
        order.setBuyer(user());
        when(userRepository.findByUsername("customer")).thenReturn(Optional.of(user()));
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> shipmentService.getCustomerShipments("customer", orderId))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(shipmentRepository);
    }

    @Test
    void customerGetsEmptyListBeforeAnyShipmentExists() {
        UUID orderId = UUID.randomUUID();
        User buyer = user();
        Order order = new Order();
        order.setBuyer(buyer);
        when(userRepository.findByUsername("customer")).thenReturn(Optional.of(buyer));
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(shipmentRepository.findAllBySellerOrder_Order_Id(orderId)).thenReturn(List.of());

        assertThat(shipmentService.getCustomerShipments("customer", orderId)).isEmpty();
    }

    private User user() {
        User user = new User("test", "hash");
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        return user;
    }
}
