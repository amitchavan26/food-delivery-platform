package com.fooddelivery.service;

import com.fooddelivery.domain.MenuItem;
import com.fooddelivery.domain.Restaurant;
import com.fooddelivery.dto.Dtos.*;
import com.fooddelivery.repository.MenuItemRepository;
import com.fooddelivery.repository.RestaurantRepository;
import com.fooddelivery.repository.UserRepository;
import com.fooddelivery.web.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RestaurantService {
    private final RestaurantRepository restaurants;
    private final MenuItemRepository menuItems;
    private final UserRepository users;

    @Transactional(readOnly = true)
    public PageResponse<RestaurantResponse> list(String city, int page, int size) {
        Pageable p = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50), Sort.by("name"));
        Page<Restaurant> result = (city == null || city.isBlank())
                ? restaurants.findAll(p)
                : restaurants.findByCityIgnoreCase(city.trim(), p);
        return new PageResponse<>(
                result.getContent().stream().map(this::toDto).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public List<RestaurantResponse> mine(UUID ownerId) {
        return restaurants.findByOwnerIdOrderByNameAsc(ownerId).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public RestaurantResponse get(UUID id) {
        return toDto(restaurants.findById(id).orElseThrow(() -> ApiException.notFound("Restaurant")));
    }

    @Transactional(readOnly = true)
    public List<MenuItemResponse> menu(UUID restaurantId) {
        if (!restaurants.existsById(restaurantId)) throw ApiException.notFound("Restaurant");
        return menuItems.findByRestaurantIdOrderByNameAsc(restaurantId).stream().map(this::toDto).toList();
    }

    @Transactional
    public RestaurantResponse create(UUID ownerId, RestaurantRequest req) {
        Restaurant r = new Restaurant();
        r.setOwner(users.getReferenceById(ownerId));
        r.setName(req.name().trim());
        r.setDescription(req.description());
        r.setCity(req.city().trim());
        return toDto(restaurants.save(r));
    }

    @Transactional
    public MenuItemResponse addItem(UUID ownerId, UUID restaurantId, MenuItemRequest req) {
        Restaurant r = requireOwned(ownerId, restaurantId);
        MenuItem m = new MenuItem();
        m.setRestaurant(r);
        apply(m, req);
        return toDto(menuItems.save(m));
    }

    @Transactional
    public MenuItemResponse updateItem(UUID ownerId, UUID restaurantId, UUID itemId, MenuItemRequest req) {
        requireOwned(ownerId, restaurantId);
        MenuItem m = menuItems.findByIdAndRestaurantId(itemId, restaurantId)
                .orElseThrow(() -> ApiException.notFound("Menu item"));
        apply(m, req);
        return toDto(m);
    }

    /** Returns the restaurant only if this user owns it; otherwise 404 so other owners' IDs aren't revealed. */
    public Restaurant requireOwned(UUID ownerId, UUID restaurantId) {
        return restaurants.findById(restaurantId)
                .filter(r -> r.getOwner().getId().equals(ownerId))
                .orElseThrow(() -> ApiException.notFound("Restaurant"));
    }

    private void apply(MenuItem m, MenuItemRequest req) {
        m.setName(req.name().trim());
        m.setDescription(req.description());
        m.setPricePaise(req.pricePaise());
        if (req.available() != null) m.setAvailable(req.available());
    }

    private RestaurantResponse toDto(Restaurant r) {
        return new RestaurantResponse(r.getId(), r.getName(), r.getDescription(), r.getCity());
    }

    private MenuItemResponse toDto(MenuItem m) {
        return new MenuItemResponse(m.getId(), m.getName(), m.getDescription(), m.getPricePaise(), m.isAvailable());
    }
}
