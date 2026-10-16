package com.fooddelivery.dto;

import com.fooddelivery.domain.OrderStatus;
import com.fooddelivery.domain.Role;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class Dtos {
    private Dtos() {}

    public record RegisterRequest(
            @NotBlank @Size(max = 120) String name,
            @NotBlank @Email @Size(max = 255) String email,
            @NotBlank @Size(min = 8, max = 100) String password,
            @NotNull Role role) {}

    public record LoginRequest(@NotBlank String email, @NotBlank String password) {}

    public record AuthResponse(String token, String name, Role role) {}

    public record RestaurantRequest(
            @NotBlank @Size(max = 160) String name,
            @Size(max = 1000) String description,
            @NotBlank @Size(max = 80) String city) {}

    public record RestaurantResponse(UUID id, String name, String description, String city) {}

    public record MenuItemRequest(
            @NotBlank @Size(max = 160) String name,
            @Size(max = 500) String description,
            @Min(1) int pricePaise,
            Boolean available) {}

    public record MenuItemResponse(UUID id, String name, String description, int pricePaise, boolean available) {}

    public record OrderLine(@NotNull UUID menuItemId, @Min(1) @Max(50) int quantity) {}

    public record PlaceOrderRequest(
            @NotNull UUID restaurantId,
            @NotEmpty @Size(max = 50) List<@Valid OrderLine> items) {}

    public record StatusUpdateRequest(@NotNull OrderStatus status) {}

    public record OrderItemResponse(UUID menuItemId, String name, int unitPricePaise, int quantity) {}

    public record OrderResponse(
            UUID id, UUID restaurantId, String restaurantName, OrderStatus status,
            long totalPaise, Instant createdAt, List<OrderItemResponse> items) {}

    public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {}

    public record ErrorResponse(String code, String message, Map<String, String> fields) {}
}
