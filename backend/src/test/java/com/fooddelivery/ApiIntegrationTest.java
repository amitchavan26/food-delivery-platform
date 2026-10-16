package com.fooddelivery;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end API tests against a real PostgreSQL (started by Testcontainers).
 * Nothing is mocked: real security, real validation, real database.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class ApiIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void cleanDatabase() {
        jdbc.execute("TRUNCATE order_items, orders, menu_items, restaurants, users CASCADE");
    }

    // ---------- helpers ----------

    private ResultActions send(HttpMethod method, String url, String token, Object body) throws Exception {
        MockHttpServletRequestBuilder req = MockMvcRequestBuilders.request(method, url);
        if (token != null) req.header("Authorization", "Bearer " + token);
        if (body != null) req.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
        return mvc.perform(req);
    }

    private String read(ResultActions r, String path) throws Exception {
        return JsonPath.read(r.andReturn().getResponse().getContentAsString(), path).toString();
    }

    private String register(String email, String role) throws Exception {
        ResultActions r = send(HttpMethod.POST, "/api/auth/register", null,
                Map.of("name", "Test User", "email", email, "password", "password123", "role", role))
                .andExpect(status().isCreated());
        return read(r, "$.token");
    }

    private String createRestaurant(String ownerToken, String name) throws Exception {
        ResultActions r = send(HttpMethod.POST, "/api/restaurants", ownerToken,
                Map.of("name", name, "city", "Pune", "description", "test"))
                .andExpect(status().isCreated());
        return read(r, "$.id");
    }

    private String addItem(String ownerToken, String restaurantId, String name, int paise, boolean available) throws Exception {
        ResultActions r = send(HttpMethod.POST, "/api/restaurants/" + restaurantId + "/menu", ownerToken,
                Map.of("name", name, "pricePaise", paise, "available", available))
                .andExpect(status().isCreated());
        return read(r, "$.id");
    }

    private ResultActions placeOrder(String customerToken, String restaurantId, String itemId, int qty) throws Exception {
        return send(HttpMethod.POST, "/api/orders", customerToken,
                Map.of("restaurantId", restaurantId, "items", List.of(Map.of("menuItemId", itemId, "quantity", qty))));
    }

    private void setStatus(String ownerToken, String orderId, String status, int expectedHttp) throws Exception {
        send(HttpMethod.PATCH, "/api/orders/" + orderId + "/status", ownerToken, Map.of("status", status))
                .andExpect(status().is(expectedHttp));
    }

    // ---------- authentication & validation ----------

    @Test
    void protectedEndpointsReturn401WithoutToken() throws Exception {
        send(HttpMethod.GET, "/api/orders/mine", null, null).andExpect(status().isUnauthorized());
        send(HttpMethod.POST, "/api/orders", null, Map.of()).andExpect(status().isUnauthorized());
    }

    @Test
    void restaurantListingIsPublic() throws Exception {
        send(HttpMethod.GET, "/api/restaurants", null, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void invalidSignupReturnsStandardValidationError() throws Exception {
        send(HttpMethod.POST, "/api/auth/register", null,
                Map.of("name", "X", "email", "not-an-email", "password", "short", "role", "CUSTOMER"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fields.email").exists())
                .andExpect(jsonPath("$.fields.password").exists());
    }

    @Test
    void cannotSignUpAsAdmin() throws Exception {
        send(HttpMethod.POST, "/api/auth/register", null,
                Map.of("name", "Evil", "email", "evil@test.com", "password", "password123", "role", "ADMIN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ROLE"));
    }

    @Test
    void wrongPasswordAndUnknownUserGiveTheSameError() throws Exception {
        register("real@test.com", "CUSTOMER");
        String wrongPassword = send(HttpMethod.POST, "/api/auth/login", null,
                Map.of("email", "real@test.com", "password", "wrong-password"))
                .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();
        String unknownUser = send(HttpMethod.POST, "/api/auth/login", null,
                Map.of("email", "nobody@test.com", "password", "password123"))
                .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();
        org.junit.jupiter.api.Assertions.assertEquals(wrongPassword, unknownUser);
    }

    @Test
    void duplicateEmailIsRejected() throws Exception {
        register("dup@test.com", "CUSTOMER");
        send(HttpMethod.POST, "/api/auth/register", null,
                Map.of("name", "Again", "email", "DUP@test.com", "password", "password123", "role", "CUSTOMER"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_TAKEN"));
    }

    // ---------- roles & ownership ----------

    @Test
    void customerCannotCreateRestaurant() throws Exception {
        String customer = register("c@test.com", "CUSTOMER");
        send(HttpMethod.POST, "/api/restaurants", customer, Map.of("name", "Nope", "city", "Pune"))
                .andExpect(status().isForbidden());
    }

    @Test
    void ownerCannotTouchAnotherOwnersRestaurant() throws Exception {
        String ownerA = register("a@test.com", "RESTAURANT");
        String ownerB = register("b@test.com", "RESTAURANT");
        String restaurantA = createRestaurant(ownerA, "A's place");
        String itemA = addItem(ownerA, restaurantA, "Dish", 1000, true);

        send(HttpMethod.POST, "/api/restaurants/" + restaurantA + "/menu", ownerB,
                Map.of("name", "Hacked", "pricePaise", 1)).andExpect(status().isNotFound());
        send(HttpMethod.PUT, "/api/restaurants/" + restaurantA + "/menu/" + itemA, ownerB,
                Map.of("name", "Hacked", "pricePaise", 1)).andExpect(status().isNotFound());
        send(HttpMethod.GET, "/api/restaurants/" + restaurantA + "/orders", ownerB, null)
                .andExpect(status().isNotFound());
    }

    @Test
    void customerCannotListRestaurantOrders() throws Exception {
        String owner = register("o@test.com", "RESTAURANT");
        String customer = register("c@test.com", "CUSTOMER");
        String restaurant = createRestaurant(owner, "Place");
        send(HttpMethod.GET, "/api/restaurants/" + restaurant + "/orders", customer, null)
                .andExpect(status().isForbidden());
    }

    // ---------- placing orders ----------

    @Test
    void orderTotalIsComputedFromDatabasePrices() throws Exception {
        String owner = register("o@test.com", "RESTAURANT");
        String customer = register("c@test.com", "CUSTOMER");
        String restaurant = createRestaurant(owner, "Place");
        String item = addItem(owner, restaurant, "Naan", 4500, true);

        placeOrder(customer, restaurant, item, 3)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PLACED"))
                .andExpect(jsonPath("$.totalPaise").value(13500))
                .andExpect(jsonPath("$.items[0].unitPricePaise").value(4500));
    }

    @Test
    void unavailableItemCannotBeOrdered() throws Exception {
        String owner = register("o@test.com", "RESTAURANT");
        String customer = register("c@test.com", "CUSTOMER");
        String restaurant = createRestaurant(owner, "Place");
        String item = addItem(owner, restaurant, "Sold out", 1000, false);

        placeOrder(customer, restaurant, item, 1)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ITEM_UNAVAILABLE"));
    }

    @Test
    void itemFromAnotherRestaurantCannotBeOrdered() throws Exception {
        String owner = register("o@test.com", "RESTAURANT");
        String customer = register("c@test.com", "CUSTOMER");
        String restaurant1 = createRestaurant(owner, "One");
        String restaurant2 = createRestaurant(owner, "Two");
        String itemFrom2 = addItem(owner, restaurant2, "Dish", 1000, true);

        placeOrder(customer, restaurant1, itemFrom2, 1)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("WRONG_RESTAURANT"));
    }

    @Test
    void invalidQuantityIsRejected() throws Exception {
        String owner = register("o@test.com", "RESTAURANT");
        String customer = register("c@test.com", "CUSTOMER");
        String restaurant = createRestaurant(owner, "Place");
        String item = addItem(owner, restaurant, "Dish", 1000, true);

        placeOrder(customer, restaurant, item, 0).andExpect(status().isBadRequest());
        placeOrder(customer, restaurant, item, 51).andExpect(status().isBadRequest());
    }

    @Test
    void ownerCannotPlaceOrders() throws Exception {
        String owner = register("o@test.com", "RESTAURANT");
        String restaurant = createRestaurant(owner, "Place");
        String item = addItem(owner, restaurant, "Dish", 1000, true);
        placeOrder(owner, restaurant, item, 1).andExpect(status().isForbidden());
    }

    // ---------- who can see an order ----------

    @Test
    void onlyTheCustomerAndTheRestaurantOwnerCanSeeAnOrder() throws Exception {
        String owner = register("o@test.com", "RESTAURANT");
        String otherOwner = register("o2@test.com", "RESTAURANT");
        String customer = register("c@test.com", "CUSTOMER");
        String stranger = register("c2@test.com", "CUSTOMER");
        String restaurant = createRestaurant(owner, "Place");
        String item = addItem(owner, restaurant, "Dish", 1000, true);
        String orderId = read(placeOrder(customer, restaurant, item, 1), "$.id");

        send(HttpMethod.GET, "/api/orders/" + orderId, customer, null).andExpect(status().isOk());
        send(HttpMethod.GET, "/api/orders/" + orderId, owner, null).andExpect(status().isOk());
        send(HttpMethod.GET, "/api/orders/" + orderId, stranger, null).andExpect(status().isNotFound());
        send(HttpMethod.GET, "/api/orders/" + orderId, otherOwner, null).andExpect(status().isNotFound());
    }

    @Test
    void customersOnlySeeTheirOwnOrdersInTheList() throws Exception {
        String owner = register("o@test.com", "RESTAURANT");
        String customer = register("c@test.com", "CUSTOMER");
        String stranger = register("c2@test.com", "CUSTOMER");
        String restaurant = createRestaurant(owner, "Place");
        String item = addItem(owner, restaurant, "Dish", 1000, true);
        placeOrder(customer, restaurant, item, 1).andExpect(status().isCreated());

        send(HttpMethod.GET, "/api/orders/mine", customer, null).andExpect(jsonPath("$.length()").value(1));
        send(HttpMethod.GET, "/api/orders/mine", stranger, null).andExpect(jsonPath("$.length()").value(0));
    }

    // ---------- status flow ----------

    @Test
    void ownerMovesOrderThroughTheAllowedFlowAndCannotSkipSteps() throws Exception {
        String owner = register("o@test.com", "RESTAURANT");
        String customer = register("c@test.com", "CUSTOMER");
        String restaurant = createRestaurant(owner, "Place");
        String item = addItem(owner, restaurant, "Dish", 1000, true);
        String orderId = read(placeOrder(customer, restaurant, item, 1), "$.id");

        setStatus(owner, orderId, "DELIVERED", 409);   // cannot skip ahead
        setStatus(owner, orderId, "ACCEPTED", 200);
        setStatus(owner, orderId, "PLACED", 409);      // cannot go backwards
        setStatus(owner, orderId, "PREPARING", 200);
        setStatus(owner, orderId, "READY", 200);
        setStatus(owner, orderId, "OUT_FOR_DELIVERY", 200);
        setStatus(owner, orderId, "DELIVERED", 200);
        setStatus(owner, orderId, "READY", 409);       // delivered is final
    }

    @Test
    void customerAndOtherOwnersCannotChangeOrderStatus() throws Exception {
        String owner = register("o@test.com", "RESTAURANT");
        String otherOwner = register("o2@test.com", "RESTAURANT");
        String customer = register("c@test.com", "CUSTOMER");
        String restaurant = createRestaurant(owner, "Place");
        String item = addItem(owner, restaurant, "Dish", 1000, true);
        String orderId = read(placeOrder(customer, restaurant, item, 1), "$.id");

        setStatus(customer, orderId, "ACCEPTED", 403);
        setStatus(otherOwner, orderId, "ACCEPTED", 404);
    }

    @Test
    void customerCanCancelOnlyBeforeTheRestaurantAccepts() throws Exception {
        String owner = register("o@test.com", "RESTAURANT");
        String customer = register("c@test.com", "CUSTOMER");
        String stranger = register("c2@test.com", "CUSTOMER");
        String restaurant = createRestaurant(owner, "Place");
        String item = addItem(owner, restaurant, "Dish", 1000, true);

        String first = read(placeOrder(customer, restaurant, item, 1), "$.id");
        send(HttpMethod.POST, "/api/orders/" + first + "/cancel", stranger, null).andExpect(status().isNotFound());
        send(HttpMethod.POST, "/api/orders/" + first + "/cancel", customer, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        String second = read(placeOrder(customer, restaurant, item, 1), "$.id");
        setStatus(owner, second, "ACCEPTED", 200);
        send(HttpMethod.POST, "/api/orders/" + second + "/cancel", customer, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_TRANSITION"));
    }
}
