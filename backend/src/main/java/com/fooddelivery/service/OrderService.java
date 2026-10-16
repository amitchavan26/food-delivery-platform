package com.fooddelivery.service;

import com.fooddelivery.domain.*;
import com.fooddelivery.dto.Dtos.*;
import com.fooddelivery.repository.MenuItemRepository;
import com.fooddelivery.repository.OrderRepository;
import com.fooddelivery.repository.RestaurantRepository;
import com.fooddelivery.repository.UserRepository;
import com.fooddelivery.web.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orders;
    private final RestaurantRepository restaurants;
    private final MenuItemRepository menuItems;
    private final UserRepository users;
    private final RestaurantService restaurantService;

    @Transactional
    public OrderResponse place(UUID customerId, PlaceOrderRequest req) {
        Restaurant restaurant = restaurants.findById(req.restaurantId())
                .orElseThrow(() -> ApiException.notFound("Restaurant"));

        Map<UUID, Integer> quantities = new LinkedHashMap<>();
        for (OrderLine line : req.items()) quantities.merge(line.menuItemId(), line.quantity(), Integer::sum);

        List<MenuItem> found = menuItems.findAllById(quantities.keySet());
        if (found.size() != quantities.size()) throw bad("UNKNOWN_ITEM", "One or more items do not exist");

        CustomerOrder order = new CustomerOrder();
        order.setCustomer(users.getReferenceById(customerId));
        order.setRestaurant(restaurant);

        long total = 0;
        for (MenuItem m : found) {
            if (!m.getRestaurant().getId().equals(restaurant.getId()))
                throw bad("WRONG_RESTAURANT", "Item does not belong to this restaurant");
            if (!m.isAvailable()) throw bad("ITEM_UNAVAILABLE", m.getName() + " is unavailable");
            int qty = quantities.get(m.getId());
            if (qty > 50) throw bad("QUANTITY_TOO_LARGE", "Maximum 50 of one item");

            OrderItem oi = new OrderItem();
            oi.setMenuItemId(m.getId());
            oi.setName(m.getName());
            oi.setUnitPricePaise(m.getPricePaise()); // price always comes from the DB, never from the client
            oi.setQuantity(qty);
            order.addItem(oi);
            total += (long) m.getPricePaise() * qty;
        }
        order.setTotalPaise(total);
        return toDto(orders.save(order));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> mine(UUID customerId, int page, int size) {
        return orders.findByCustomerIdOrderByCreatedAtDesc(customerId, pageable(page, size))
                .stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> forRestaurant(UUID ownerId, UUID restaurantId, int page, int size) {
        restaurantService.requireOwned(ownerId, restaurantId);
        return orders.findByRestaurantIdOrderByCreatedAtDesc(restaurantId, pageable(page, size))
                .stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse get(UUID userId, UUID orderId) {
        CustomerOrder o = orders.findWithRestaurantById(orderId).orElseThrow(() -> ApiException.notFound("Order"));
        boolean isCustomer = o.getCustomer().getId().equals(userId);
        boolean isOwner = o.getRestaurant().getOwner().getId().equals(userId);
        if (!isCustomer && !isOwner) throw ApiException.notFound("Order");
        return toDto(o);
    }

    @Transactional
    public OrderResponse updateStatus(UUID ownerId, UUID orderId, OrderStatus target) {
        CustomerOrder o = orders.findWithRestaurantById(orderId).orElseThrow(() -> ApiException.notFound("Order"));
        if (!o.getRestaurant().getOwner().getId().equals(ownerId)) throw ApiException.notFound("Order");
        if (target == OrderStatus.CANCELLED || !o.getStatus().canMoveTo(target)) {
            throw new ApiException(HttpStatus.CONFLICT, "INVALID_TRANSITION",
                    "Cannot move order from " + o.getStatus() + " to " + target);
        }
        o.setStatus(target);
        return toDto(o);
    }

    @Transactional
    public OrderResponse cancel(UUID customerId, UUID orderId) {
        CustomerOrder o = orders.findWithRestaurantById(orderId).orElseThrow(() -> ApiException.notFound("Order"));
        if (!o.getCustomer().getId().equals(customerId)) throw ApiException.notFound("Order");
        if (!o.getStatus().canMoveTo(OrderStatus.CANCELLED)) {
            throw new ApiException(HttpStatus.CONFLICT, "INVALID_TRANSITION",
                    "Order can only be cancelled before the restaurant accepts it");
        }
        o.setStatus(OrderStatus.CANCELLED);
        return toDto(o);
    }

    private Pageable pageable(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50));
    }

    private ApiException bad(String code, String msg) {
        return new ApiException(HttpStatus.BAD_REQUEST, code, msg);
    }

    private OrderResponse toDto(CustomerOrder o) {
        return new OrderResponse(o.getId(), o.getRestaurant().getId(), o.getRestaurant().getName(),
                o.getStatus(), o.getTotalPaise(), o.getCreatedAt(),
                o.getItems().stream()
                        .map(i -> new OrderItemResponse(i.getMenuItemId(), i.getName(), i.getUnitPricePaise(), i.getQuantity()))
                        .toList());
    }
}
