package com.fooddelivery.web;

import com.fooddelivery.dto.Dtos.*;
import com.fooddelivery.security.CurrentUser;
import com.fooddelivery.service.RestaurantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/restaurants")
@RequiredArgsConstructor
public class RestaurantController {
    private final RestaurantService service;

    @GetMapping
    public PageResponse<RestaurantResponse> list(@RequestParam(required = false) String city,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "20") int size) {
        return service.list(city, page, size);
    }

    @GetMapping("/{id}")
    public RestaurantResponse get(@PathVariable UUID id) {
        return service.get(id);
    }

    @GetMapping("/{id}/menu")
    public List<MenuItemResponse> menu(@PathVariable UUID id) {
        return service.menu(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('RESTAURANT')")
    @ResponseStatus(HttpStatus.CREATED)
    public RestaurantResponse create(Authentication auth, @Valid @RequestBody RestaurantRequest req) {
        return service.create(CurrentUser.id(auth), req);
    }

    @PostMapping("/{id}/menu")
    @PreAuthorize("hasRole('RESTAURANT')")
    @ResponseStatus(HttpStatus.CREATED)
    public MenuItemResponse addItem(Authentication auth, @PathVariable UUID id,
                                    @Valid @RequestBody MenuItemRequest req) {
        return service.addItem(CurrentUser.id(auth), id, req);
    }

    @PutMapping("/{id}/menu/{itemId}")
    @PreAuthorize("hasRole('RESTAURANT')")
    public MenuItemResponse updateItem(Authentication auth, @PathVariable UUID id, @PathVariable UUID itemId,
                                       @Valid @RequestBody MenuItemRequest req) {
        return service.updateItem(CurrentUser.id(auth), id, itemId, req);
    }
}
