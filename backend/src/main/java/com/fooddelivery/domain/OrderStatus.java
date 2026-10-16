package com.fooddelivery.domain;

import java.util.Set;

public enum OrderStatus {
    PLACED, ACCEPTED, REJECTED, PREPARING, READY, OUT_FOR_DELIVERY, DELIVERED, CANCELLED;

    /** The state machine for an order. Everything not listed here is illegal. */
    public Set<OrderStatus> next() {
        return switch (this) {
            case PLACED -> Set.of(ACCEPTED, REJECTED, CANCELLED);
            case ACCEPTED -> Set.of(PREPARING);
            case PREPARING -> Set.of(READY);
            case READY -> Set.of(OUT_FOR_DELIVERY);
            case OUT_FOR_DELIVERY -> Set.of(DELIVERED);
            case REJECTED, DELIVERED, CANCELLED -> Set.of();
        };
    }

    public boolean canMoveTo(OrderStatus target) {
        return next().contains(target);
    }
}
