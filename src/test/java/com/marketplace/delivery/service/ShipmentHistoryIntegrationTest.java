package com.marketplace.delivery.service;

import com.marketplace.delivery.entity.*;
import com.marketplace.delivery.repository.ShipmentRepository;
import com.marketplace.delivery.repository.ShipmentStatusHistoryRepository;
import com.marketplace.order.entity.Order;
import com.marketplace.order.entity.OrderStatus;
import com.marketplace.order.entity.SellerOrder;
import com.marketplace.order.entity.SellerOrderStatus;
import com.marketplace.order.repository.OrderRepository;
import com.marketplace.order.repository.SellerOrderRepository;
import com.marketplace.seller.entity.Seller;
import com.marketplace.seller.repository.SellerRepository;
import com.marketplace.store.entity.Store;
import com.marketplace.store.repository.StoreRepository;
import com.marketplace.user.entity.User;
import com.marketplace.user.repository.UserRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import com.marketplace.order.repository.OrderStatusHistoryRepository;
import com.marketplace.order.service.OrderCancellationService;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.url=jdbc:h2:mem:shipment-history;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE"
})
@ActiveProfiles("test")
class ShipmentHistoryIntegrationTest {
    @Autowired ShipmentService shipments;
    @Autowired ShipmentHistoryService history;
    @Autowired ShipmentRepository shipmentRepository;
    @MockitoSpyBean ShipmentStatusHistoryRepository historyRepository;
    @Autowired UserRepository users;
    @Autowired SellerRepository sellers;
    @Autowired StoreRepository stores;
    @Autowired OrderRepository orders;
    @Autowired SellerOrderRepository sellerOrders;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired OrderCancellationService cancellations;
    @MockitoSpyBean OrderStatusHistoryRepository orderHistoryRepository;

    @DynamicPropertySource
    static void postgresIfConfigured(DynamicPropertyRegistry properties) {
        String url = System.getenv("SHIPMENT_TEST_DATABASE_URL");
        if (url != null) {
            properties.add("spring.datasource.url", () -> url);
            properties.add("spring.datasource.username", () -> System.getenv("SHIPMENT_TEST_DATABASE_USER"));
            properties.add("spring.datasource.password", () -> System.getenv("SHIPMENT_TEST_DATABASE_PASSWORD"));
            properties.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
            properties.add("spring.flyway.enabled", () -> true);
            properties.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        }
    }

    private String username;
    private String otherUsername;
    private UUID sellerOrderId;
    private UUID orderId;

    @BeforeEach
    void fixtures() {
        username = "shipment-" + UUID.randomUUID().toString().substring(0, 8);
        otherUsername = "other-" + UUID.randomUUID().toString().substring(0, 8);
        new TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
            User owner = users.save(new User(username, "hash"));
            users.save(new User(otherUsername, "hash"));
            Seller seller = new Seller();
            seller.setUser(owner);
            sellers.save(seller);
            Store store = new Store();
            store.setSeller(seller);
            store.setName("History store");
            store.setSlug(username);
            stores.save(store);
            Order order = new Order();
            order.setBuyer(owner);
            order.setStatus(OrderStatus.CONFIRMED);
            order.setTotalAmount(BigDecimal.TEN);
            order.setSubtotalAmount(BigDecimal.TEN);
            orders.save(order);
            orderId = order.getId();
            SellerOrder part = new SellerOrder();
            part.setOrder(order);
            part.setStore(store);
            part.setStatus(SellerOrderStatus.CONFIRMED);
            part.setTotalAmount(BigDecimal.TEN);
            sellerOrders.save(part);
            sellerOrderId = part.getId();
        });
    }

    @Test
    void recordsFullLifecycleAndIgnoresRepeatedRequests() {
        Shipment shipment = shipments.createShipment(username, sellerOrderId);
        shipments.createShipment(username, sellerOrderId);
        shipments.updatePreparationStatus(username, sellerOrderId, ShipmentStatus.PACKING);
        shipments.updatePreparationStatus(username, sellerOrderId, ShipmentStatus.PACKING);
        shipments.updatePreparationStatus(username, sellerOrderId, ShipmentStatus.READY_FOR_DISPATCH);
        shipments.dispatchShipment(username, sellerOrderId, "Carrier", "TRACK-1");
        assertThat(orders.findById(orderId).orElseThrow().getStatus()).isEqualTo(OrderStatus.IN_DELIVERY);
        shipments.dispatchShipment(username, sellerOrderId, "Carrier", "TRACK-1");
        shipments.confirmDelivery(username, sellerOrderId);
        assertThat(orders.findById(orderId).orElseThrow().getStatus()).isEqualTo(OrderStatus.DELIVERED);
        shipments.confirmDelivery(username, sellerOrderId);

        var events = history.getSellerHistory(username, sellerOrderId);
        assertThat(events).extracting(e -> e.toStatus()).containsExactly(
                ShipmentStatus.CREATED, ShipmentStatus.PACKING, ShipmentStatus.READY_FOR_DISPATCH,
                ShipmentStatus.IN_TRANSIT, ShipmentStatus.DELIVERED);
        assertThat(events.getFirst().fromStatus()).isNull();
        assertThat(events.getLast().fromStatus()).isEqualTo(ShipmentStatus.IN_TRANSIT);
        assertThat(events).allSatisfy(e -> {
            assertThat(e.actorType()).isEqualTo(ShipmentActorType.SELLER);
            assertThat(e.actorUsername()).isEqualTo(username);
            assertThat(e.changedAt()).isNotNull();
            assertThat(e.shipmentId()).isEqualTo(shipment.getId());
        });
        assertThat(history.getCustomerHistory(username, orderId, shipment.getId())).hasSize(5);
    }

    @Test
    void failedDeliveryRecordsReasonOnce() {
        shipments.createShipment(username, sellerOrderId);
        shipments.updatePreparationStatus(username, sellerOrderId, ShipmentStatus.PACKING);
        shipments.updatePreparationStatus(username, sellerOrderId, ShipmentStatus.READY_FOR_DISPATCH);
        shipments.dispatchShipment(username, sellerOrderId, null, null);
        shipments.markDeliveryFailed(username, sellerOrderId, "  Recipient unavailable  ");
        shipments.markDeliveryFailed(username, sellerOrderId, "Recipient unavailable");
        var events = history.getSellerHistory(username, sellerOrderId);
        assertThat(events).hasSize(5);
        assertThat(events.getLast().details()).isEqualTo("Recipient unavailable");
        assertThat(orders.findById(orderId).orElseThrow().getStatus()).isEqualTo(OrderStatus.DELIVERY_FAILED);
    }

    @Test
    void historyFailureRollsBackStatusAndVersion() {
        Shipment shipment = shipments.createShipment(username, sellerOrderId);
        Long originalVersion = shipment.getVersion();
        doThrow(new DataIntegrityViolationException("History unavailable"))
                .when(historyRepository).save(any(ShipmentStatusHistory.class));

        assertThatThrownBy(() -> shipments.updatePreparationStatus(username, sellerOrderId, ShipmentStatus.PACKING))
                .isInstanceOf(DataIntegrityViolationException.class);

        Shipment unchanged = shipmentRepository.findById(shipment.getId()).orElseThrow();
        assertThat(unchanged.getStatus()).isEqualTo(ShipmentStatus.CREATED);
        assertThat(unchanged.getVersion()).isEqualTo(originalVersion);
        assertThat(history.getSellerHistory(username, sellerOrderId)).hasSize(1);
    }

    @Test
    void historyFailureRollsBackShipmentCreation() {
        doThrow(new DataIntegrityViolationException("History unavailable"))
                .when(historyRepository).save(any(ShipmentStatusHistory.class));
        assertThatThrownBy(() -> shipments.createShipment(username, sellerOrderId))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(shipmentRepository.findBySellerOrder_Id(sellerOrderId)).isEmpty();
    }

    @Test
    void foreignSellerAndCustomerCannotReadHistory() {
        Shipment shipment = shipments.createShipment(username, sellerOrderId);
        assertThatThrownBy(() -> history.getSellerHistory(otherUsername, sellerOrderId))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> history.getCustomerHistory(otherUsername, orderId, shipment.getId()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void recordRequiresExistingTransaction() {
        assertThatThrownBy(() -> history.record(new Shipment(), null, ShipmentStatus.CREATED,
                ShipmentActorType.SYSTEM, null, null))
                .isInstanceOf(IllegalTransactionStateException.class);
    }

    @Test
    void aggregateHistoryFailureRollsBackShipmentHistoryAndOrder() {
        Shipment shipment = shipments.createShipment(username, sellerOrderId);
        shipments.updatePreparationStatus(username, sellerOrderId, ShipmentStatus.PACKING);
        shipments.updatePreparationStatus(username, sellerOrderId, ShipmentStatus.READY_FOR_DISPATCH);
        doThrow(new DataIntegrityViolationException("Order history unavailable"))
                .when(orderHistoryRepository).save(any());
        assertThatThrownBy(() -> shipments.dispatchShipment(username, sellerOrderId, "Carrier", "TRACK"))
                .isInstanceOf(DataIntegrityViolationException.class);
        Shipment unchanged = shipmentRepository.findById(shipment.getId()).orElseThrow();
        assertThat(unchanged.getStatus()).isEqualTo(ShipmentStatus.READY_FOR_DISPATCH);
        assertThat(unchanged.getShippedAt()).isNull();
        assertThat(history.getSellerHistory(username, sellerOrderId)).hasSize(3);
        assertThat(orders.findById(orderId).orElseThrow().getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void deliveredPartDoesNotCompleteOrderWithPendingPartAndExpiryFinishesRemainingWork() {
        Part second = addPart(SellerOrderStatus.PENDING);
        toTransit(username, sellerOrderId);
        shipments.confirmDelivery(username, sellerOrderId);
        assertThat(orders.findById(orderId).orElseThrow().getStatus()).isEqualTo(OrderStatus.PARTIALLY_DELIVERED);
        new TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
            Order order = orders.findById(orderId).orElseThrow();
            order.setExpiresAt(Instant.now().minusSeconds(60));
        });
        cancellations.expireOrder(orderId);
        assertThat(sellerOrders.findById(second.id()).orElseThrow().getStatus()).isEqualTo(SellerOrderStatus.CANCELLED);
        assertThat(orders.findById(orderId).orElseThrow().getStatus()).isEqualTo(OrderStatus.DELIVERED);
    }

    @Test
    void parallelDeliveriesOfDifferentStoresProduceOneCompletedOrder() throws Exception {
        Part second = addPart(SellerOrderStatus.CONFIRMED);
        toTransit(username, sellerOrderId);
        toTransit(second.username(), second.id());
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> { start.await(); return shipments.confirmDelivery(username, sellerOrderId); });
            var other = executor.submit(() -> { start.await(); return shipments.confirmDelivery(second.username(), second.id()); });
            start.countDown();
            first.get(15, TimeUnit.SECONDS);
            other.get(15, TimeUnit.SECONDS);
        }
        assertThat(orders.findById(orderId).orElseThrow().getStatus()).isEqualTo(OrderStatus.DELIVERED);
        assertThat(history.getSellerHistory(username, sellerOrderId)).hasSize(5);
        assertThat(history.getSellerHistory(second.username(), second.id())).hasSize(5);
    }

    private void toTransit(String owner, UUID partId) {
        shipments.createShipment(owner, partId);
        shipments.updatePreparationStatus(owner, partId, ShipmentStatus.PACKING);
        shipments.updatePreparationStatus(owner, partId, ShipmentStatus.READY_FOR_DISPATCH);
        shipments.dispatchShipment(owner, partId, null, null);
    }

    private Part addPart(SellerOrderStatus status) {
        return new TransactionTemplate(transactionManager).execute(tx -> {
            String owner = username + "-second";
            User user = users.save(new User(owner, "hash"));
            Seller seller = new Seller();
            seller.setUser(user);
            sellers.save(seller);
            Store store = new Store();
            store.setSeller(seller); store.setName("Second store"); store.setSlug(owner);
            stores.save(store);
            Order order = orders.findById(orderId).orElseThrow();
            order.setTotalAmount(BigDecimal.valueOf(20));
            order.setSubtotalAmount(BigDecimal.valueOf(20));
            SellerOrder part = new SellerOrder();
            part.setOrder(order); part.setStore(store); part.setStatus(status); part.setTotalAmount(BigDecimal.TEN);
            sellerOrders.save(part);
            return new Part(owner, part.getId());
        });
    }

    private record Part(String username, UUID id) {}
}
