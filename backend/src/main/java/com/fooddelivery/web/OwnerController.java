package com.fooddelivery.web;

import com.fooddelivery.dto.Dtos.RestaurantResponse;
import com.fooddelivery.security.CurrentUser;
import com.fooddelivery.service.RestaurantService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/owner")
@RequiredArgsConstructor
public class OwnerController {
    private final RestaurantService service;

    @GetMapping("/restaurants")
    @PreAuthorize("hasRole('RESTAURANT')")
    public List<RestaurantResponse> myRestaurants(Authentication auth) {
        return service.mine(CurrentUser.id(auth));
    }
}
