package com.fooddelivery.repository;

import com.fooddelivery.domain.CustomerOrder;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<CustomerOrder, UUID> {
    @EntityGraph(attributePaths = "restaurant")
    List<CustomerOrder> findByCustomerIdOrderByCreatedAtDesc(UUID customerId, Pageable pageable);

    @EntityGraph(attributePaths = "restaurant")
    List<CustomerOrder> findByRestaurantIdOrderByCreatedAtDesc(UUID restaurantId, Pageable pageable);

    @EntityGraph(attributePaths = "restaurant")
    Optional<CustomerOrder> findWithRestaurantById(UUID id);
}
