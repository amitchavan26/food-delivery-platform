package com.fooddelivery.repository;

import com.fooddelivery.domain.Restaurant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RestaurantRepository extends JpaRepository<Restaurant, UUID> {
    Page<Restaurant> findByCityIgnoreCase(String city, Pageable pageable);

    List<Restaurant> findByOwnerIdOrderByNameAsc(UUID ownerId);
}
