package com.marketplace.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.marketplace.common.exception.BusinessRuleException;
import com.marketplace.common.exception.ConflictException;
import com.marketplace.common.exception.ResourceNotFoundException;
import com.marketplace.order.dto.request.CreateOrderRequest;
import com.marketplace.order.dto.request.DeliveryDetails;
import com.marketplace.order.dto.response.OrderResponse;
import com.marketplace.order.entity.OrderStatus;
import com.marketplace.order.entity.SellerOrderStatus;
import com.marketplace.product.dto.request.CreateProductVariantRequest;
import com.marketplace.product.service.ProductVariantService;
import com.marketplace.promotion.dto.request.CreatePromocodeRequest;
import com.marketplace.promotion.entity.DiscountType;
import com.marketplace.promotion.service.PromocodeService;
import com.marketplace.review.dto.request.CreateReviewRequest;
import com.marketplace.review.service.ReviewService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.PageRequest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class OrderIntegrationTest {

    private static final String BUYER_USERNAME = "order-buyer";

    private static final UUID BUYER_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID SELLER_USER_ID = UUID.fromString("10000000-0000-0000-0000-000000000002");
    private static final UUID SELLER_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final UUID STORE_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final UUID CATEGORY_ID = UUID.fromString("40000000-0000-0000-0000-000000000001");
    private static final UUID PRODUCT_ID = UUID.fromString("50000000-0000-0000-0000-000000000001");
    private static final UUID INVENTORY_ID = UUID.fromString("60000000-0000-0000-0000-000000000001");
    private static final UUID CART_ID = UUID.fromString("70000000-0000-0000-0000-000000000001");
    private static final UUID CART_ITEM_ID = UUID.fromString("80000000-0000-0000-0000-000000000001");
    private static final DeliveryDetails DELIVERY = new DeliveryDetails(
            "Test Buyer", "+998901234567", "TASHKENT_CITY", "Tashkent", "Amir Temur 10, apartment 5");

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private OrderCreationService orderCreationService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderCancellationService orderCancellationService;

    @Autowired
    private SellerOrderService sellerOrderService;

    @Autowired
    private OrderExpirationJob expirationJob;

    @Autowired
    private ProductVariantService productVariantService;

    @Autowired
    private PromocodeService promocodeService;

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private OrderResponse checkout(String username) {
        return checkout(username, null, null);
    }

    private OrderResponse checkout(String username, String key) {
        return checkout(username, key, null);
    }

    private OrderResponse checkout(String username, String key, String promocode) {
        return orderCreationService.createOrder(username, key, new CreateOrderRequest(promocode, DELIVERY));
    }

    @BeforeEach
    void setUp() {
        cleanDatabase();
        seedOrderData(10);
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
    }

    @Test
    void createsOrderReservesStockClearsCartAndKeepsPriceSnapshot() {
        OrderResponse created = checkout(BUYER_USERNAME);

        assertThat(created.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(created.totalAmount()).isEqualByComparingTo("1998.00");
        assertThat(created.currentTotalAmount()).isEqualByComparingTo("1998.00");
        assertThat(created.sellerOrders().get(0).discountAmount()).isEqualByComparingTo("0.00");
        assertThat(created.sellerOrders().get(0).payableAmount()).isEqualByComparingTo("1998.00");
        assertThat(created.createdAt()).isNotNull();
        assertThat(created.delivery().recipientName()).isEqualTo("Test Buyer");
        assertThat(created.delivery().addressLine()).isEqualTo("Amir Temur 10, apartment 5");
        assertThat(created.sellerOrders()).hasSize(1);
        assertThat(created.sellerOrders().get(0).delivery()).isEqualTo(created.delivery());
        var sellerView = sellerOrderService.getMySellerOrder("order-seller",
                created.sellerOrders().get(0).sellerOrderId());
        assertThat(sellerView.delivery()).isEqualTo(created.delivery());
        assertThatThrownBy(() -> sellerOrderService.getMySellerOrder(BUYER_USERNAME,
                created.sellerOrders().get(0).sellerOrderId()))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThat(created.sellerOrders().get(0).status()).isEqualTo(SellerOrderStatus.PENDING);
        assertThat(created.sellerOrders().get(0).items()).hasSize(1);

        assertThat(count("SELECT COUNT(*) FROM cart_items WHERE cart_id = ?", CART_ID)).isZero();
        assertThat(count("SELECT COUNT(*) FROM orders")).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM order_items")).isEqualTo(1);
        assertThat(integer("SELECT reserved_quantity FROM inventories WHERE product_id = ?", PRODUCT_ID))
                .isEqualTo(2);
        assertThat(count("SELECT COUNT(*) FROM order_status_history WHERE order_id = ?", created.orderId()))
                .isEqualTo(2);
        assertThat(count("SELECT COUNT(*) FROM inventory_movements WHERE inventory_id = ? AND type = 'RESERVED'", INVENTORY_ID))
                .isEqualTo(1);

        jdbcTemplate.update("UPDATE products SET price = ? WHERE id = ?", new BigDecimal("1500.00"), PRODUCT_ID);

        OrderResponse loaded = orderService.getMyOrder(BUYER_USERNAME, created.orderId());
        assertThat(loaded.delivery()).isEqualTo(created.delivery());
        assertThat(loaded.totalAmount()).isEqualByComparingTo("1998.00");
        assertThat(loaded.sellerOrders()).hasSize(1);
        assertThat(loaded.sellerOrders().get(0).items().get(0).productName()).isEqualTo("Order Product");
        assertThat(decimal("SELECT unit_price FROM order_items WHERE order_id = ?", created.orderId()))
                .isEqualByComparingTo("999.00");
    }

    @Test
    void variantCheckoutReservesChosenSkuAndRestoresItOnCancellation() {
        var variant = productVariantService.create("order-seller", PRODUCT_ID,
                new CreateProductVariantRequest("Blue XL", "ORDER-BLUE-XL", new BigDecimal("1200.00")));
        productVariantService.addStock("order-seller", PRODUCT_ID, variant.id(), 5);
        jdbcTemplate.update("UPDATE cart_items SET variant_id = ? WHERE id = ?", variant.id(), CART_ITEM_ID);

        OrderResponse created = checkout(BUYER_USERNAME);
        assertThat(created.totalAmount()).isEqualByComparingTo("2400.00");
        assertThat(created.sellerOrders().get(0).items().get(0).sku()).isEqualTo("ORDER-BLUE-XL");
        assertThat(created.sellerOrders().get(0).items().get(0).productName()).contains("Blue XL");
        assertThat(integer("SELECT reserved_quantity FROM product_variants WHERE id = ?", variant.id()))
                .isEqualTo(2);
        assertThat(integer("SELECT reserved_quantity FROM inventories WHERE product_id = ?", PRODUCT_ID))
                .isZero();
        assertThat(count("SELECT COUNT(*) FROM stock_reservations WHERE variant_id = ? AND inventory_id IS NULL", variant.id()))
                .isEqualTo(1);

        orderCancellationService.cancelMyOrder(BUYER_USERNAME, created.orderId());
        assertThat(integer("SELECT reserved_quantity FROM product_variants WHERE id = ?", variant.id()))
                .isZero();
        assertThat(integer("SELECT quantity FROM product_variants WHERE id = ?", variant.id()))
                .isEqualTo(5);
        assertThat(count("SELECT COUNT(*) FROM inventory_movements WHERE variant_id = ?", variant.id()))
                .isEqualTo(3);
    }

    @Test
    void promocodeDiscountIsAppliedOnceAndReleasedOnCancellation() {
        jdbcTemplate.update("UPDATE store_delivery_zones SET delivery_fee = 30.00 WHERE store_id = ?", STORE_ID);
        promocodeService.create(new CreatePromocodeRequest("SAVE10", DiscountType.PERCENT,
                new BigDecimal("10"), BigDecimal.ZERO, null, null, 1));

        OrderResponse created = checkout(BUYER_USERNAME, UUID.randomUUID().toString(), "SAVE10");
        assertThat(created.subtotalAmount()).isEqualByComparingTo("1998.00");
        assertThat(created.deliveryTotalAmount()).isEqualByComparingTo("30.00");
        assertThat(created.sellerOrders().get(0).deliveryFee()).isEqualByComparingTo("30.00");
        assertThat(created.discountAmount()).isEqualByComparingTo("199.80");
        assertThat(created.sellerOrders().get(0).discountAmount()).isEqualByComparingTo("199.80");
        assertThat(created.sellerOrders().get(0).payableAmount()).isEqualByComparingTo("1828.20");
        assertThat(created.totalAmount()).isEqualByComparingTo("1828.20");
        assertThat(created.currentTotalAmount()).isEqualByComparingTo("1828.20");
        OrderResponse reloaded = orderService.getMyOrder(BUYER_USERNAME, created.orderId());
        assertThat(reloaded.totalAmount()).isEqualByComparingTo("1828.20");
        assertThat(reloaded.sellerOrders().get(0).deliveryFee()).isEqualByComparingTo("30.00");
        assertThat(count("SELECT COUNT(*) FROM promocode_usages WHERE order_id = ?", created.orderId())).isEqualTo(1);
        assertThat(integer("SELECT used_count FROM promocodes WHERE code = 'SAVE10'")).isEqualTo(1);

        OrderResponse cancelled = orderCancellationService.cancelMyOrder(BUYER_USERNAME, created.orderId());
        assertThat(cancelled.totalAmount()).isEqualByComparingTo("1828.20");
        assertThat(cancelled.currentTotalAmount()).isEqualByComparingTo("0.00");
        assertThat(count("SELECT COUNT(*) FROM promocode_usages")).isZero();
        assertThat(integer("SELECT used_count FROM promocodes WHERE code = 'SAVE10'")).isZero();
    }

    @Test
    void confirmedBuyerCanReviewProductAfterModeration() {
        OrderResponse created = checkout(BUYER_USERNAME);
        sellerOrderService.confirmSellerOrder("order-seller", created.sellerOrders().get(0).sellerOrderId());

        var review = reviewService.create(BUYER_USERNAME,
                new CreateReviewRequest(PRODUCT_ID, 5, "Excellent"));
        assertThat(review.status().name()).isEqualTo("PENDING");
        assertThat(reviewService.publicReviews(PRODUCT_ID, PageRequest.of(0, 10)).getTotalElements()).isZero();
        reviewService.moderate(review.id(), true, "order-seller");
        assertThat(reviewService.publicReviews(PRODUCT_ID, PageRequest.of(0, 10)).getTotalElements()).isEqualTo(1);
        assertThat(reviewService.rating(PRODUCT_ID).averageRating()).isEqualTo(5.0);
    }

    @Test
    void rollsBackOrderAndReservationWhenStockIsInsufficient() {
        jdbcTemplate.update(
                "UPDATE inventories SET quantity = 1, reserved_quantity = 0 WHERE product_id = ?",
                PRODUCT_ID
        );

        assertThatThrownBy(() -> checkout(BUYER_USERNAME))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Amount exceeds the maximum allowed value");

        assertThat(count("SELECT COUNT(*) FROM orders")).isZero();
        assertThat(count("SELECT COUNT(*) FROM order_items")).isZero();
        assertThat(count("SELECT COUNT(*) FROM cart_items WHERE cart_id = ?", CART_ID)).isEqualTo(1);
        assertThat(integer("SELECT reserved_quantity FROM inventories WHERE product_id = ?", PRODUCT_ID))
                .isZero();
    }

    @Test
    void buyerCancellationClosesSellerPartAndReservationInOneTransaction() {
        OrderResponse created = checkout(BUYER_USERNAME);
        OrderResponse cancelled = orderCancellationService.cancelMyOrder(BUYER_USERNAME, created.orderId());

        assertThat(cancelled.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(cancelled.sellerOrders()).hasSize(1);
        assertThat(cancelled.sellerOrders().get(0).status()).isEqualTo(SellerOrderStatus.CANCELLED);
        assertThat(integer("SELECT reserved_quantity FROM inventories WHERE id = ?", INVENTORY_ID)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM stock_reservations", String.class))
                .isEqualTo("RELEASED");
        assertThat(count("SELECT COUNT(*) FROM stock_reservations WHERE closed_at IS NOT NULL"))
                .isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM order_status_history WHERE order_id = ? AND new_status = 'CANCELLED'", created.orderId()))
                .isEqualTo(2);
        assertThatThrownBy(() -> sellerOrderService.confirmSellerOrder("order-seller",
                created.sellerOrders().get(0).sellerOrderId())).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void sameIdempotencyKeyReturnsOriginalOrderWithoutReservingTwice() {
        OrderResponse first = checkout(BUYER_USERNAME, "checkout-1");
        OrderResponse second = checkout(BUYER_USERNAME, "checkout-1");

        assertThat(second.orderId()).isEqualTo(first.orderId());
        assertThat(count("SELECT COUNT(*) FROM orders")).isEqualTo(1);
        assertThat(integer("SELECT reserved_quantity FROM inventories WHERE id = ?", INVENTORY_ID))
                .isEqualTo(2);
        assertThatThrownBy(() -> orderCreationService.createOrder(BUYER_USERNAME, "checkout-1",
                new CreateOrderRequest(null, new DeliveryDetails(
                        "Test Buyer", "+998901234567", "TASHKENT_CITY", "Tashkent", "Different address"))))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> checkout(BUYER_USERNAME, "checkout-2"))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void sellerConfirmationClosesReservationAndUpdatesOrder() {
        OrderResponse created = checkout(BUYER_USERNAME);
        sellerOrderService.confirmSellerOrder("order-seller",
                created.sellerOrders().get(0).sellerOrderId());

        assertThat(orderService.getMyOrder(BUYER_USERNAME, created.orderId()).status())
                .isEqualTo(OrderStatus.CONFIRMED);
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM stock_reservations", String.class))
                .isEqualTo("CONFIRMED");
        assertThat(integer("SELECT quantity FROM inventories WHERE id = ?", INVENTORY_ID)).isEqualTo(8);
        assertThat(integer("SELECT reserved_quantity FROM inventories WHERE id = ?", INVENTORY_ID)).isZero();
    }

    @Test
    void expiredOrderReleasesHeldStock() {
        OrderResponse created = checkout(BUYER_USERNAME);
        jdbcTemplate.update("UPDATE orders SET expires_at = ? WHERE id = ?",
                java.sql.Timestamp.from(Instant.now().minusSeconds(60)), created.orderId());
        expirationJob.releaseExpiredReservations();

        assertThat(orderService.getMyOrder(BUYER_USERNAME, created.orderId()).status())
                .isEqualTo(OrderStatus.CANCELLED);
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM stock_reservations", String.class))
                .isEqualTo("RELEASED");
        assertThat(integer("SELECT reserved_quantity FROM inventories WHERE id = ?", INVENTORY_ID)).isZero();
    }

    @Test
    void simultaneousCheckoutOfOneCartCreatesOnlyOneOrder() throws Exception {
        try (var executor = Executors.newFixedThreadPool(2)) {
            CountDownLatch start = new CountDownLatch(1);
            Callable<Boolean> checkout = () -> {
                start.await();
                try {
                    checkout(BUYER_USERNAME);
                    return true;
                } catch (BusinessRuleException e) {
                    return false;
                }
            };
            Future<Boolean> first = executor.submit(checkout);
            Future<Boolean> second = executor.submit(checkout);
            start.countDown();
            assertThat((first.get() ? 1 : 0) + (second.get() ? 1 : 0)).isEqualTo(1);
        }
        assertThat(count("SELECT COUNT(*) FROM orders")).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM stock_reservations")).isEqualTo(1);
        assertThat(integer("SELECT reserved_quantity FROM inventories WHERE id = ?", INVENTORY_ID)).isEqualTo(2);
    }

    @Test
    void twoBuyersCannotReserveTheLastUnitTwice() throws Exception {
        UUID secondBuyerId = UUID.fromString("10000000-0000-0000-0000-000000000005");
        UUID secondCartId = UUID.fromString("70000000-0000-0000-0000-000000000005");
        jdbcTemplate.update("UPDATE inventories SET quantity = 1 WHERE id = ?", INVENTORY_ID);
        jdbcTemplate.update("UPDATE cart_items SET quantity = 1 WHERE id = ?", CART_ITEM_ID);
        jdbcTemplate.update("INSERT INTO users (id,username,password_hash,status,created_at,updated_at) "
                + "VALUES (?, 'second-buyer', 'not-used', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)", secondBuyerId);
        jdbcTemplate.update("INSERT INTO carts (id,user_id,created_at,updated_at) "
                + "VALUES (?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)", secondCartId, secondBuyerId);
        jdbcTemplate.update("INSERT INTO cart_items (id,cart_id,product_id,quantity,created_at,updated_at) "
                + "VALUES (?, ?, ?, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                UUID.randomUUID(), secondCartId, PRODUCT_ID);

        try (var executor = Executors.newFixedThreadPool(2)) {
            CountDownLatch start = new CountDownLatch(1);
            Callable<Boolean> firstCheckout = () -> checkoutLastUnit(start, BUYER_USERNAME);
            Callable<Boolean> secondCheckout = () -> checkoutLastUnit(start, "second-buyer");
            Future<Boolean> first = executor.submit(firstCheckout);
            Future<Boolean> second = executor.submit(secondCheckout);
            start.countDown();
            assertThat((first.get() ? 1 : 0) + (second.get() ? 1 : 0)).isEqualTo(1);
        }
        assertThat(count("SELECT COUNT(*) FROM orders")).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM stock_reservations WHERE status = 'ACTIVE'")).isEqualTo(1);
        assertThat(integer("SELECT quantity FROM inventories WHERE id = ?", INVENTORY_ID)).isEqualTo(1);
        assertThat(integer("SELECT reserved_quantity FROM inventories WHERE id = ?", INVENTORY_ID)).isEqualTo(1);
    }

    private boolean checkoutLastUnit(CountDownLatch start, String username) throws InterruptedException {
        start.await();
        try {
            checkout(username);
            return true;
        } catch (BusinessRuleException | OptimisticLockingFailureException expected) {
            return false;
        }
    }

    @Test
    void simultaneousSellerConfirmationAndBuyerCancellationRemainConsistent() throws Exception {
        OrderResponse created = checkout(BUYER_USERNAME);
        UUID sellerOrderId = created.sellerOrders().get(0).sellerOrderId();
        try (var executor = Executors.newFixedThreadPool(2)) {
            CountDownLatch start = new CountDownLatch(1);
            Callable<Boolean> confirm = () -> {
                start.await();
                try {
                    sellerOrderService.confirmSellerOrder("order-seller", sellerOrderId);
                    return true;
                } catch (BusinessRuleException e) {
                    return false;
                }
            };
            Callable<Boolean> cancel = () -> {
                start.await();
                try {
                    orderCancellationService.cancelMyOrder(BUYER_USERNAME, created.orderId());
                    return true;
                } catch (BusinessRuleException e) {
                    return false;
                }
            };
            Future<Boolean> first = executor.submit(confirm);
            Future<Boolean> second = executor.submit(cancel);
            start.countDown();
            assertThat((first.get() ? 1 : 0) + (second.get() ? 1 : 0)).isEqualTo(1);
        }
        OrderStatus status = orderService.getMyOrder(BUYER_USERNAME, created.orderId()).status();
        String reservationStatus = jdbcTemplate.queryForObject("SELECT status FROM stock_reservations", String.class);
        if (status == OrderStatus.CONFIRMED) {
            assertThat(reservationStatus).isEqualTo("CONFIRMED");
            assertThat(integer("SELECT quantity FROM inventories WHERE id = ?", INVENTORY_ID)).isEqualTo(8);
        } else {
            assertThat(status).isEqualTo(OrderStatus.CANCELLED);
            assertThat(reservationStatus).isEqualTo("RELEASED");
            assertThat(integer("SELECT quantity FROM inventories WHERE id = ?", INVENTORY_ID)).isEqualTo(10);
        }
        assertThat(integer("SELECT reserved_quantity FROM inventories WHERE id = ?", INVENTORY_ID)).isZero();
    }

    @Test
    void createsAndCancelsTwoStoreOrderWithoutOrphanedReservations() {
        jdbcTemplate.update("UPDATE store_delivery_zones SET delivery_fee = 20.00 WHERE store_id = ?", STORE_ID);
        UUID secondStoreId = seedSecondStoreToCart();
        jdbcTemplate.update("UPDATE store_delivery_zones SET delivery_fee = 30.00 WHERE store_id = ?", secondStoreId);

        OrderResponse created = checkout(BUYER_USERNAME);
        assertThat(created.sellerOrders()).hasSize(2);
        assertThat(created.subtotalAmount()).isEqualByComparingTo("2498.00");
        assertThat(created.deliveryTotalAmount()).isEqualByComparingTo("50.00");
        assertThat(created.totalAmount()).isEqualByComparingTo("2548.00");
        assertThat(created.currentTotalAmount()).isEqualByComparingTo("2548.00");
        for (var part : created.sellerOrders()) {
            assertThat(part.deliveryFee()).isEqualByComparingTo(
                    part.storeId().equals(STORE_ID) ? "20.00" : "30.00");
        }
        assertThat(count("SELECT COUNT(*) FROM stock_reservations WHERE status = 'ACTIVE'")).isEqualTo(2);

        OrderResponse cancelled = orderCancellationService.cancelMyOrder(BUYER_USERNAME, created.orderId());
        assertThat(cancelled.totalAmount()).isEqualByComparingTo("2548.00");
        assertThat(cancelled.currentTotalAmount()).isEqualByComparingTo("0.00");
        assertThat(cancelled.sellerOrders()).allMatch(part -> part.status() == SellerOrderStatus.CANCELLED);
        assertThat(count("SELECT COUNT(*) FROM stock_reservations WHERE status = 'RELEASED'")).isEqualTo(2);
        assertThat(count("SELECT COUNT(*) FROM inventories WHERE reserved_quantity <> 0")).isZero();
    }

    @Test
    void sellerCancellationKeepsCheckoutSnapshotAndReducesCurrentAmount() {
        jdbcTemplate.update("UPDATE store_delivery_zones SET delivery_fee = 20.00 WHERE store_id = ?", STORE_ID);
        UUID secondStoreId = seedSecondStoreToCart();
        jdbcTemplate.update("UPDATE store_delivery_zones SET delivery_fee = 30.00 WHERE store_id = ?", secondStoreId);
        promocodeService.create(new CreatePromocodeRequest("PART10", DiscountType.PERCENT,
                new BigDecimal("10"), BigDecimal.ZERO, null, null, 1));

        OrderResponse created = checkout(BUYER_USERNAME, UUID.randomUUID().toString(), "PART10");
        assertThat(created.subtotalAmount()).isEqualByComparingTo("2498.00");
        assertThat(created.discountAmount()).isEqualByComparingTo("249.80");
        assertThat(created.totalAmount()).isEqualByComparingTo("2298.20");
        var secondPart = created.sellerOrders().stream()
                .filter(part -> part.storeId().equals(secondStoreId))
                .findFirst().orElseThrow();
        assertThat(secondPart.discountAmount()).isEqualByComparingTo("50.00");
        assertThat(secondPart.payableAmount()).isEqualByComparingTo("480.00");

        sellerOrderService.cancelSellerOrder("second-seller", secondPart.sellerOrderId(), "Out of stock");
        OrderResponse remaining = orderService.getMyOrder(BUYER_USERNAME, created.orderId());
        assertThat(remaining.status()).isEqualTo(OrderStatus.PARTIALLY_CANCELLED);
        assertThat(remaining.totalAmount()).isEqualByComparingTo("2298.20");
        assertThat(remaining.currentTotalAmount()).isEqualByComparingTo("1818.20");
        assertThat(remaining.discountAmount()).isEqualByComparingTo("249.80");
        assertThat(integer("SELECT used_count FROM promocodes WHERE code = 'PART10'")).isEqualTo(1);
    }

    private UUID seedSecondStoreToCart() {
        UUID secondSellerId = UUID.fromString("20000000-0000-0000-0000-000000000002");
        UUID secondSellerUserId = UUID.fromString("10000000-0000-0000-0000-000000000003");
        UUID secondStoreId = UUID.fromString("30000000-0000-0000-0000-000000000002");
        UUID secondProductId = UUID.fromString("50000000-0000-0000-0000-000000000002");
        jdbcTemplate.update("INSERT INTO users (id,username,password_hash,status,created_at,updated_at) "
                + "VALUES (?, 'second-seller', 'not-used', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)", secondSellerUserId);
        jdbcTemplate.update("INSERT INTO sellers (id,user_id,status,created_at,updated_at) "
                + "VALUES (?, ?, 'APPROVED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)", secondSellerId, secondSellerUserId);
        jdbcTemplate.update("INSERT INTO stores (id,seller_id,name,slug,active,created_at,updated_at) "
                + "VALUES (?, ?, 'Second Store', 'second-store', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                secondStoreId, secondSellerId);
        seedDeliveryZone(secondStoreId);
        jdbcTemplate.update("INSERT INTO products (id,store_id,category_id,name,slug,status,price,created_at,updated_at) "
                + "VALUES (?, ?, ?, 'Second Product', 'second-product', 'PUBLISHED', 500.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                secondProductId, secondStoreId, CATEGORY_ID);
        jdbcTemplate.update("INSERT INTO inventories (id,product_id,quantity,reserved_quantity,version,created_at,updated_at) "
                + "VALUES (?, ?, 5, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                UUID.randomUUID(), secondProductId);
        jdbcTemplate.update("INSERT INTO cart_items (id,cart_id,product_id,quantity,created_at,updated_at) "
                + "VALUES (?, ?, ?, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                UUID.randomUUID(), CART_ID, secondProductId);
        return secondStoreId;
    }

    private void seedOrderData(int quantity) {
        jdbcTemplate.update("""
                INSERT INTO users (id, username, password_hash, status, created_at, updated_at)
                VALUES (?, ?, 'not-used', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, BUYER_ID, BUYER_USERNAME);
        jdbcTemplate.update("""
                INSERT INTO users (id, username, password_hash, status, created_at, updated_at)
                VALUES (?, 'order-seller', 'not-used', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, SELLER_USER_ID);
        jdbcTemplate.update("""
                INSERT INTO sellers (id, user_id, status, approved_at, created_at, updated_at)
                VALUES (?, ?, 'APPROVED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, SELLER_ID, SELLER_USER_ID);
        jdbcTemplate.update("""
                INSERT INTO stores (id, seller_id, name, slug, active, created_at, updated_at)
                VALUES (?, ?, 'Order Store', 'order-store', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, STORE_ID, SELLER_ID);
        seedDeliveryZone(STORE_ID);
        jdbcTemplate.update("""
                INSERT INTO categories (id, name, slug, active, created_at, updated_at)
                VALUES (?, 'Order Category', 'order-category', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, CATEGORY_ID);
        jdbcTemplate.update("""
                INSERT INTO products
                    (id, store_id, category_id, name, slug, status, created_at, updated_at, price)
                VALUES
                    (?, ?, ?, 'Order Product', 'order-product', 'PUBLISHED',
                     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 999.00)
                """, PRODUCT_ID, STORE_ID, CATEGORY_ID);
        jdbcTemplate.update("""
                INSERT INTO inventories
                    (id, product_id, quantity, reserved_quantity, created_at, updated_at, version)
                VALUES
                    (?, ?, ?, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
                """, INVENTORY_ID, PRODUCT_ID, quantity);
        jdbcTemplate.update("""
                INSERT INTO carts (id, user_id, created_at, updated_at)
                VALUES (?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, CART_ID, BUYER_ID);
        jdbcTemplate.update("""
                INSERT INTO cart_items (id, cart_id, product_id, quantity, created_at, updated_at)
                VALUES (?, ?, ?, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, CART_ITEM_ID, CART_ID, PRODUCT_ID);
    }

    private void seedDeliveryZone(UUID storeId) {
        jdbcTemplate.update("""
                INSERT INTO store_delivery_zones
                    (id, store_id, region_code, delivery_fee, min_delivery_days, max_delivery_days,
                     active, version, created_at, updated_at)
                VALUES (?, ?, 'TASHKENT_CITY', 0.00, 1, 2, TRUE, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, UUID.randomUUID(), storeId);
    }

    private void cleanDatabase() {
        jdbcTemplate.update("DELETE FROM shipment_status_history");
        jdbcTemplate.update("DELETE FROM shipments");
        jdbcTemplate.update("DELETE FROM promocode_usages");
        jdbcTemplate.update("DELETE FROM reviews");
        jdbcTemplate.update("DELETE FROM favorites");
        jdbcTemplate.update("DELETE FROM order_status_history");
        jdbcTemplate.update("DELETE FROM inventory_movements");
        jdbcTemplate.update("DELETE FROM stock_reservations");
        jdbcTemplate.update("DELETE FROM order_items");
        jdbcTemplate.update("DELETE FROM seller_orders");
        jdbcTemplate.update("DELETE FROM orders");
        jdbcTemplate.update("DELETE FROM promocodes");
        jdbcTemplate.update("DELETE FROM cart_items");
        jdbcTemplate.update("DELETE FROM carts");
        jdbcTemplate.update("DELETE FROM inventories");
        jdbcTemplate.update("DELETE FROM product_images");
        jdbcTemplate.update("DELETE FROM product_variants");
        jdbcTemplate.update("DELETE FROM products");
        jdbcTemplate.update("DELETE FROM store_delivery_zones");
        jdbcTemplate.update("DELETE FROM stores");
        jdbcTemplate.update("DELETE FROM sellers");
        jdbcTemplate.update("DELETE FROM categories");
        jdbcTemplate.update("DELETE FROM user_roles");
        jdbcTemplate.update("DELETE FROM admin_audit_logs");
        jdbcTemplate.update("DELETE FROM refresh_tokens");
        jdbcTemplate.update("DELETE FROM users");
    }

    private long count(String sql, Object... args) {
        Long result = jdbcTemplate.queryForObject(sql, Long.class, args);
        return result == null ? 0 : result;
    }

    private int integer(String sql, Object... args) {
        Integer result = jdbcTemplate.queryForObject(sql, Integer.class, args);
        return result == null ? 0 : result;
    }

    private BigDecimal decimal(String sql, Object... args) {
        return jdbcTemplate.queryForObject(sql, BigDecimal.class, args);
    }
}
