package com.marketplace.audit;

import com.marketplace.cart.entity.*;
import com.marketplace.cart.repository.*;
import com.marketplace.category.entity.Category;
import com.marketplace.category.repository.CategoryRepository;
import com.marketplace.category.service.CategoryService;
import com.marketplace.common.exception.BusinessRuleException;
import com.marketplace.delivery.entity.StoreDeliveryZone;
import com.marketplace.delivery.repository.StoreDeliveryZoneRepository;
import com.marketplace.delivery.repository.DeliveryRegionRepository;
import com.marketplace.inventory.entity.Inventory;
import com.marketplace.inventory.repository.InventoryRepository;
import com.marketplace.order.dto.request.*;
import com.marketplace.order.entity.*;
import com.marketplace.order.repository.OrderRepository;
import com.marketplace.order.service.OrderCreationService;
import com.marketplace.product.entity.*;
import com.marketplace.product.repository.*;
import com.marketplace.product.service.*;
import com.marketplace.product.dto.request.CreateProductVariantRequest;
import com.marketplace.security.jwt.JwtTokenProvider;
import com.marketplace.storage.exception.StorageOperationException;
import com.marketplace.seller.entity.*;
import com.marketplace.seller.repository.SellerRepository;
import com.marketplace.store.entity.Store;
import com.marketplace.store.repository.StoreRepository;
import com.marketplace.user.entity.*;
import com.marketplace.user.repository.*;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.jdbc.core.JdbcTemplate;
import com.marketplace.storage.service.ObjectStorageService;
import com.marketplace.storage.service.StorageCleanupService;
import com.marketplace.storage.repository.StorageDeletionTaskRepository;
import com.marketplace.storage.exception.StorageOperationException;
import com.marketplace.cart.service.CartService;
import com.marketplace.order.service.SellerOrderService;
import java.util.concurrent.atomic.AtomicReference;
import static org.mockito.Mockito.doThrow;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Regression coverage for the six audit findings, using actual JWT and PostgreSQL.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class BackendAuditRegressionTest {
    @Container @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");
    static final Path IMAGE_ROOT = Path.of(System.getProperty("java.io.tmpdir"), "marketplace-audit-" + UUID.randomUUID());
    @DynamicPropertySource
    static void storage(DynamicPropertyRegistry properties) {
        properties.add("storage.mode", () -> "local");
        properties.add("storage.local-directory", IMAGE_ROOT::toString);
    }
    @Autowired MockMvc mvc;
    @Autowired JwtTokenProvider jwt;
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired SellerRepository sellers;
    @Autowired StoreRepository stores;
    @Autowired CategoryRepository categories;
    @Autowired CategoryService categoryService;
    @Autowired ProductRepository products;
    @Autowired ProductVariantRepository variants;
    @Autowired ProductImageRepository images;
    @Autowired ProductVariantService variantService;
    @Autowired ProductImageService imageService;
    @Autowired InventoryRepository inventory;
    @Autowired CartRepository carts;
    @Autowired CartItemRepository cartItems;
    @Autowired OrderRepository orders;
    @Autowired OrderCreationService checkout;
    @Autowired DeliveryRegionRepository regions;
    @Autowired StoreDeliveryZoneRepository zones;
    @Autowired PlatformTransactionManager transactions;
    @Autowired com.fasterxml.jackson.databind.ObjectMapper json;
    @Autowired CartService cartService;
    @Autowired SellerOrderService sellerOrderService;
    @Autowired com.marketplace.order.service.OrderCancellationService cancellations;
    @Autowired StorageCleanupService cleanup;
    @Autowired StorageDeletionTaskRepository deletionTasks;
    @Autowired JdbcTemplate jdbc;
    @MockitoSpyBean ObjectStorageService objectStorage;
    String owner;
    String outsider;
    UUID productId, categoryId, sellerId, foreignOrderId, cartItemId;

    @BeforeEach
    void seed() {
        owner = "audit-" + UUID.randomUUID().toString().substring(0, 8);
        outsider = owner + "-other";
        new TransactionTemplate(transactions).executeWithoutResult(tx -> {
            User user = new User(owner, "hash");
            user.addRole(roles.findByName(RoleName.CUSTOMER).orElseThrow());
            user.addRole(roles.findByName(RoleName.SELLER).orElseThrow());
            users.save(user);
            User other = new User(outsider, "hash");
            other.addRole(roles.findByName(RoleName.CUSTOMER).orElseThrow());
            other.addRole(roles.findByName(RoleName.SELLER).orElseThrow());
            users.save(other);
            Seller otherSeller = new Seller();
            otherSeller.setUser(other); otherSeller.setStatus(SellerStatus.APPROVED);
            sellers.save(otherSeller);
            Store otherStore = new Store();
            otherStore.setSeller(otherSeller); otherStore.setName("Other store");
            otherStore.setSlug(outsider); otherStore.setApproved(true); stores.save(otherStore);
            Seller seller = new Seller();
            seller.setUser(user); seller.setStatus(SellerStatus.APPROVED);
            sellers.save(seller); sellerId = seller.getId();
            Store store = new Store();
            store.setSeller(seller); store.setName("Audit store"); store.setSlug(owner); store.setApproved(true);
            stores.save(store);
            Category category = new Category();
            category.setName("Audit category"); category.setSlug(owner);
            categories.save(category); categoryId = category.getId();
            Product product = new Product();
            product.setStore(store); product.setCategory(category);
            product.setName("Audit product"); product.setSlug(owner);
            product.setStatus(ProductStatus.PUBLISHED); product.setPrice(new BigDecimal("100000.00"));
            products.save(product); productId = product.getId();
            Inventory stock = new Inventory();
            stock.setProduct(product); stock.setQuantity(10); inventory.save(stock);
            Cart cart = new Cart(); cart.setUser(user);
            CartItem item = new CartItem(); item.setCart(cart); item.setProduct(product); item.setQuantity(1);
            cart.getItems().add(item); carts.saveAndFlush(cart); cartItemId = item.getId();
            StoreDeliveryZone zone = new StoreDeliveryZone();
            zone.setStore(store); zone.setRegion(regions.findById("TASHKENT_CITY").orElseThrow());
            zone.setDeliveryFee(BigDecimal.ZERO); zone.setMinDeliveryDays(1); zone.setMaxDeliveryDays(2);
            zones.save(zone);
            Order foreign = new Order(); foreign.setBuyer(other); foreign.setStatus(OrderStatus.PENDING);
            foreign.setTotalAmount(BigDecimal.TEN); foreign.setSubtotalAmount(BigDecimal.TEN);
            orders.save(foreign); foreignOrderId = foreign.getId();
        });
    }

    private String bearer(String username) { return "Bearer " + jwt.generateToken(username); }
    private CreateOrderRequest request() {
        return new CreateOrderRequest(null, new DeliveryDetails("Audit", "+998900000000",
                "TASHKENT_CITY", "Tashkent", "Test address"));
    }

    @Test void anonymousCannotReadOrders() throws Exception {
        mvc.perform(get("/api/v1/orders")).andExpect(status().isUnauthorized());
    }
    @Test void customerCannotReadAdminStatistics() throws Exception {
        mvc.perform(get("/api/v1/admin/statistics").header("Authorization", bearer(outsider)))
                .andExpect(status().isForbidden());
    }
    @Test void invalidJwtCannotReadOrders() throws Exception {
        mvc.perform(get("/api/v1/orders").header("Authorization", "Bearer invalid.token.value"))
                .andExpect(status().isUnauthorized());
    }
    @Test void blockedUserCannotReuseValidAccessToken() throws Exception {
        String access = bearer(owner);
        new TransactionTemplate(transactions).executeWithoutResult(tx ->
                users.findByUsername(owner).orElseThrow().changeStatus(UserStatus.BLOCKED));
        mvc.perform(get("/api/v1/orders").header("Authorization", access))
                .andExpect(status().isUnauthorized());
    }
    @Test void httpJourneyFromRegistrationToDeliveryKeepsOwnershipAndHistory() throws Exception {
        String buyer = owner + "-buyer";
        String credentials = json.writeValueAsString(java.util.Map.of("username", buyer, "password", "Audit12345!"));
        mvc.perform(post("/api/v1/auth/register").contentType("application/json").content(credentials))
                .andExpect(status().isCreated());
        var login = json.readTree(mvc.perform(post("/api/v1/auth/login").contentType("application/json").content(credentials))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        String refreshRequest = json.writeValueAsString(java.util.Map.of("refreshToken", login.path("refreshToken").asText()));
        var rotated = json.readTree(mvc.perform(post("/api/v1/auth/refresh").contentType("application/json").content(refreshRequest))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        mvc.perform(post("/api/v1/auth/refresh").contentType("application/json").content(refreshRequest))
                .andExpect(status().isUnauthorized());
        String customer = "Bearer " + rotated.path("accessToken").asText();
        mvc.perform(post("/api/v1/cart/items").header("Authorization", customer).contentType("application/json")
                .content(json.writeValueAsString(java.util.Map.of("productId", productId, "quantity", 1))))
                .andExpect(status().isOk());
        String checkoutJson = json.writeValueAsString(request());
        var order = json.readTree(mvc.perform(post("/api/v1/orders").header("Authorization", customer)
                .header("Idempotency-Key", "http-journey").contentType("application/json").content(checkoutJson))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        String orderId = order.path("orderId").asText();
        String partId = order.path("sellerOrders").get(0).path("sellerOrderId").asText();
        mvc.perform(post("/api/v1/orders").header("Authorization", customer)
                .header("Idempotency-Key", "http-journey").contentType("application/json").content(checkoutJson))
                .andExpect(status().isOk()).andExpect(jsonPath("$.orderId").value(orderId));
        mvc.perform(get("/api/v1/orders/" + orderId).header("Authorization", bearer(outsider)))
                .andExpect(status().isNotFound());
        String partRoute = "/api/v1/seller/orders/" + partId;
        mvc.perform(patch(partRoute + "/confirm").header("Authorization", customer))
                .andExpect(status().isForbidden());
        mvc.perform(patch(partRoute + "/confirm").header("Authorization", bearer(outsider)))
                .andExpect(status().isNotFound());
        mvc.perform(patch(partRoute + "/confirm").header("Authorization", bearer(owner)))
                .andExpect(status().isOk());
        String shipmentRoute = partRoute + "/shipment";
        var shipment = json.readTree(mvc.perform(get(shipmentRoute).header("Authorization", bearer(owner)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        for (String next : new String[]{"PACKING", "READY_FOR_DISPATCH"}) {
            mvc.perform(patch(shipmentRoute).header("Authorization", bearer(owner)).contentType("application/json")
                    .content(json.writeValueAsString(java.util.Map.of("targetStatus", next))))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.status").value(next));
        }
        mvc.perform(post(shipmentRoute + "/dispatch").header("Authorization", bearer(owner))
                .contentType("application/json").content("{\"carrierName\":\"Audit carrier\",\"trackingNumber\":\"AUDIT-1\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("IN_TRANSIT"));
        for (int i=0; i<2; i++) {
            mvc.perform(post(shipmentRoute + "/confirm-delivery").header("Authorization", bearer(owner)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("DELIVERED"));
        }
        mvc.perform(get("/api/v1/orders/" + orderId).header("Authorization", customer))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("DELIVERED"))
                .andExpect(jsonPath("$.totalAmount").value(100000));
        mvc.perform(get("/api/v1/orders/" + orderId + "/shipments/" + shipment.path("id").asText() + "/history")
                .header("Authorization", customer)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(5));
        mvc.perform(get("/api/v1/cart").header("Authorization", customer))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(0));
        mvc.perform(get(shipmentRoute + "/history").header("Authorization", bearer(owner)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(5));
    }
    @Test void sellerCannotReadAnotherStoresProduct() throws Exception {
        mvc.perform(get("/api/v1/products/me/" + productId).header("Authorization", bearer(outsider)))
                .andExpect(status().isNotFound());
    }
    @Test void foreignCustomerShipmentAccessMustReturn403() throws Exception {
        mvc.perform(get("/api/v1/orders/" + foreignOrderId + "/shipments").header("Authorization", bearer(owner)))
                .andExpect(status().isForbidden());
    }
    @Test void checkoutMustRejectCategoryDisabledAfterCartWasFilled() {
        categoryService.deactivateCategory(categoryId);
        assertThatThrownBy(() -> checkout.createOrder(owner, "audit-inactive-category", request()))
                .isInstanceOf(BusinessRuleException.class);
    }
    @Test void validProductAndVariantNamesMustFitOrderSnapshot() {
        new TransactionTemplate(transactions).executeWithoutResult(tx ->
                products.findById(productId).orElseThrow().setName("P".repeat(200)));
        var variant = variantService.create(owner, productId,
                new CreateProductVariantRequest("V".repeat(120), owner + "-SKU", new BigDecimal("100000.00")));
        variantService.addStock(owner, productId, variant.id(), 10);
        new TransactionTemplate(transactions).executeWithoutResult(tx ->
                cartItems.findById(cartItemId).orElseThrow().setVariant(variants.findById(variant.id()).orElseThrow()));
        var order = checkout.createOrder(owner, "audit-long-name", request());
        assertThat(order.sellerOrders().getFirst().items().getFirst().productName())
                .isEqualTo("P".repeat(200) + " — " + "V".repeat(120));
    }
    @Test void variantPriceWithThreeDecimalsMustBeRejected() throws Exception {
        mvc.perform(post("/api/v1/products/me/" + productId + "/variants")
                .header("Authorization", bearer(owner)).contentType("application/json")
                .content("{\"name\":\"Variant\",\"sku\":\"" + owner + "-DEC\",\"price\":100.005}"))
                .andExpect(status().isBadRequest());
    }
    @Test void suspendedSellerMustNotCreateVariants() throws Exception {
        new TransactionTemplate(transactions).executeWithoutResult(tx ->
                sellers.findById(sellerId).orElseThrow().setStatus(SellerStatus.SUSPENDED));
        mvc.perform(post("/api/v1/products/me/" + productId + "/variants")
                .header("Authorization", bearer(owner)).contentType("application/json")
                .content("{\"name\":\"Variant\",\"sku\":\"" + owner + "-SUSP\",\"price\":100}"))
                .andExpect(status().isUnprocessableEntity());
    }
    @Test void imageMustRemainReadableWhenDatabaseDeletionRollsBack() {
        byte[] bytes = java.util.Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+a7foAAAAASUVORK5CYII=");
        var uploaded = imageService.uploadImage(owner, productId,
                new MockMultipartFile("file", "audit.png", "image/png", bytes));
        String key = images.findById(uploaded.id()).orElseThrow().getObjectKey();
        new TransactionTemplate(transactions).executeWithoutResult(tx -> {
            imageService.deleteImage(owner, productId, uploaded.id());
            tx.setRollbackOnly();
        });
        assertThat(images.findById(uploaded.id())).isPresent();
        assertThat(Files.exists(IMAGE_ROOT.resolve(key))).as("Rollback must not lose the image file").isTrue();
        assertThat(deletionTasks.findByObjectKey(key)).isEmpty();
    }

    @Test void inactiveCategoryCannotBeAddedOrUpdatedInCart() {
        categoryService.deactivateCategory(categoryId);
        assertThatThrownBy(() -> cartService.addItem(owner, productId, 1)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> cartService.updateItem(owner, cartItemId, 2)).isInstanceOf(BusinessRuleException.class);
        assertThat(cartItems.findById(cartItemId).orElseThrow().getQuantity()).isEqualTo(1);
    }

    @Test void invalidVariantPriceCannotBeUpdatedOrSavedThroughService() throws Exception {
        var variant = variantService.create(owner, productId,
                new CreateProductVariantRequest("Variant", owner + "-PRICE", new BigDecimal("100.00")));
        for (String price : new String[]{"100.005", "100000000000000000.00"}) {
            mvc.perform(put("/api/v1/products/me/" + productId + "/variants/" + variant.id())
                    .header("Authorization", bearer(owner)).contentType("application/json")
                    .content("{\"name\":\"Variant\",\"active\":true,\"price\":" + price + "}"))
                    .andExpect(status().isBadRequest());
            assertThatThrownBy(() -> variantService.create(owner, productId,
                    new CreateProductVariantRequest("Bad price", owner + "-BAD", new BigDecimal(price))))
                    .isInstanceOf(BusinessRuleException.class);
        }
        assertThat(variants.findById(variant.id()).orElseThrow().getPrice()).isEqualByComparingTo("100.00");
    }

    @Test void suspendedSellerCanReadVariantsButCannotEditThemOrStock() {
        var variant = variantService.create(owner, productId,
                new CreateProductVariantRequest("Variant", owner + "-STOCK", new BigDecimal("100.00")));
        variantService.addStock(owner, productId, variant.id(), 10);
        new TransactionTemplate(transactions).executeWithoutResult(tx ->
                sellers.findById(sellerId).orElseThrow().setStatus(SellerStatus.SUSPENDED));
        assertThat(variantService.myVariants(owner, productId)).hasSize(1);
        assertThatThrownBy(() -> variantService.addStock(owner, productId, variant.id(), 1))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> variantService.removeStock(owner, productId, variant.id(), 1))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> variantService.update(owner, productId, variant.id(),
                new com.marketplace.product.dto.request.UpdateProductVariantRequest("Changed", BigDecimal.TEN, true)))
                .isInstanceOf(BusinessRuleException.class);
        assertThat(variants.findById(variant.id()).orElseThrow().getQuantity()).isEqualTo(10);
    }

    @Test void inactiveOrUnapprovedStoreCannotCreateVariant() {
        for (boolean active : new boolean[]{false, true}) {
            new TransactionTemplate(transactions).executeWithoutResult(tx -> {
                var store = products.findById(productId).orElseThrow().getStore();
                store.setActive(active); store.setApproved(!active);
            });
            assertThatThrownBy(() -> variantService.create(owner, productId,
                    new CreateProductVariantRequest("Variant", owner + "-STORE", BigDecimal.TEN)))
                    .isInstanceOf(BusinessRuleException.class);
        }
    }

    @Test void suspensionDoesNotPreventReleasingExistingVariantReservation() {
        var variant = variantService.create(owner, productId,
                new CreateProductVariantRequest("Variant", owner + "-RELEASE", BigDecimal.TEN));
        variantService.addStock(owner, productId, variant.id(), 10);
        new TransactionTemplate(transactions).executeWithoutResult(tx ->
                cartItems.findById(cartItemId).orElseThrow().setVariant(variants.findById(variant.id()).orElseThrow()));
        var order = checkout.createOrder(owner, "before-suspension", request());
        new TransactionTemplate(transactions).executeWithoutResult(tx ->
                sellers.findById(sellerId).orElseThrow().setStatus(SellerStatus.SUSPENDED));
        cancellations.cancelMyOrder(owner, order.orderId());
        var stock = variants.findById(variant.id()).orElseThrow();
        assertThat(stock.getQuantity()).isEqualTo(10);
        assertThat(stock.getReservedQuantity()).isZero();
    }

    @Test void foreignShipmentWritesAndHistoryReturnUniform403() throws Exception {
        var order = checkout.createOrder(owner, "foreign-shipment", request());
        UUID partId = order.sellerOrders().getFirst().sellerOrderId();
        sellerOrderService.confirmSellerOrder(owner, partId);
        String sellerRoute = "/api/v1/seller/orders/" + partId + "/shipment";
        mvc.perform(patch(sellerRoute).header("Authorization", bearer(outsider))
                .contentType("application/json").content("{\"targetStatus\":\"PACKING\"}"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("FORBIDDEN_OPERATION"));
        mvc.perform(get(sellerRoute + "/history").header("Authorization", bearer(outsider)))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("FORBIDDEN_OPERATION"));
        mvc.perform(get(sellerRoute).header("Authorization", bearer(owner)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CREATED"));
    }

    private com.marketplace.product.dto.response.ProductImageResponse uploadImage() {
        byte[] bytes = java.util.Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+a7foAAAAASUVORK5CYII=");
        return imageService.uploadImage(owner, productId, new MockMultipartFile("file", "audit.png", "image/png", bytes));
    }

    @Test void committedImageDeletionRemovesFileAndTask() {
        var image = uploadImage();
        String key = images.findById(image.id()).orElseThrow().getObjectKey();
        imageService.deleteImage(owner, productId, image.id());
        assertThat(images.findById(image.id())).isEmpty();
        assertThat(Files.exists(IMAGE_ROOT.resolve(key))).isFalse();
        assertThat(deletionTasks.findByObjectKey(key)).isEmpty();
    }

    @Test void rolledBackUploadRemovesUnreferencedFileAfterMethodReturns() {
        AtomicReference<UUID> imageId = new AtomicReference<>();
        AtomicReference<String> objectKey = new AtomicReference<>();
        new TransactionTemplate(transactions).executeWithoutResult(tx -> {
            var image = uploadImage();
            imageId.set(image.id());
            objectKey.set(images.findById(image.id()).orElseThrow().getObjectKey());
            assertThat(Files.exists(IMAGE_ROOT.resolve(objectKey.get()))).isTrue();
            tx.setRollbackOnly();
        });
        assertThat(images.findById(imageId.get())).isEmpty();
        assertThat(Files.exists(IMAGE_ROOT.resolve(objectKey.get()))).isFalse();
    }

    @Test void failedObjectDeletionIsPersistedAndRetried() {
        var image = uploadImage();
        String key = images.findById(image.id()).orElseThrow().getObjectKey();
        doThrow(new StorageOperationException("Temporary storage failure", new java.io.IOException("offline")))
                .doCallRealMethod().when(objectStorage).delete(key);
        imageService.deleteImage(owner, productId, image.id());
        assertThat(images.findById(image.id())).isEmpty();
        assertThat(Files.exists(IMAGE_ROOT.resolve(key))).isTrue();
        var task = deletionTasks.findByObjectKey(key).orElseThrow();
        assertThat(task.getAttempts()).isEqualTo(1);
        jdbc.update("UPDATE storage_deletion_tasks SET next_attempt_at = ? WHERE id = ?",
                java.sql.Timestamp.from(java.time.Instant.now().minusSeconds(1)), task.getId());
        cleanup.retryPendingDeletions();
        assertThat(deletionTasks.findByObjectKey(key)).isEmpty();
        assertThat(Files.exists(IMAGE_ROOT.resolve(key))).isFalse();
    }
}
