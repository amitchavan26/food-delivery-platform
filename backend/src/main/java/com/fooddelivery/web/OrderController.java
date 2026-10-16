package com.fooddelivery.web;

import com.fooddelivery.dto.Dtos.*;
import com.fooddelivery.security.CurrentUser;
import com.fooddelivery.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class OrderController {
    private final OrderService service;

    @PostMapping("/api/orders")
    @PreAuthorize("hasRole('CUSTOMER')")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse place(Authentication auth, @Valid @RequestBody PlaceOrderRequest req) {
        return service.place(CurrentUser.id(auth), req);
    }

    @GetMapping("/api/orders/mine")
    @PreAuthorize("hasRole('CUSTOMER')")
    public List<OrderResponse> mine(Authentication auth, @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "20") int size) {
        return service.mine(CurrentUser.id(auth), page, size);
    }

    @GetMapping("/api/orders/{id}")
    public OrderResponse get(Authentication auth, @PathVariable UUID id) {
        return service.get(CurrentUser.id(auth), id);
    }

    @PostMapping("/api/orders/{id}/cancel")
    @PreAuthorize("hasRole('CUSTOMER')")
    public OrderResponse cancel(Authentication auth, @PathVariable UUID id) {
        return service.cancel(CurrentUser.id(auth), id);
    }

    @PatchMapping("/api/orders/{id}/status")
    @PreAuthorize("hasRole('RESTAURANT')")
    public OrderResponse updateStatus(Authentication auth, @PathVariable UUID id,
                                      @Valid @RequestBody StatusUpdateRequest req) {
        return service.updateStatus(CurrentUser.id(auth), id, req.status());
    }

    @GetMapping("/api/restaurants/{restaurantId}/orders")
    @PreAuthorize("hasRole('RESTAURANT')")
    public List<OrderResponse> forRestaurant(Authentication auth, @PathVariable UUID restaurantId,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size) {
        return service.forRestaurant(CurrentUser.id(auth), restaurantId, page, size);
    }
}
