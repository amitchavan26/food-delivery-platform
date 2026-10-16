package com.fooddelivery;

import com.fooddelivery.domain.OrderStatus;
import org.junit.jupiter.api.Test;

import static com.fooddelivery.domain.OrderStatus.*;
import static org.junit.jupiter.api.Assertions.*;

class OrderStatusTest {
    @Test
    void happyPathIsAllowedStepByStep() {
        assertTrue(PLACED.canMoveTo(ACCEPTED));
        assertTrue(ACCEPTED.canMoveTo(PREPARING));
        assertTrue(PREPARING.canMoveTo(READY));
        assertTrue(READY.canMoveTo(OUT_FOR_DELIVERY));
        assertTrue(OUT_FOR_DELIVERY.canMoveTo(DELIVERED));
    }

    @Test
    void cannotSkipStepsOrGoBackwards() {
        assertFalse(PLACED.canMoveTo(DELIVERED));
        assertFalse(READY.canMoveTo(PREPARING));
    }

    @Test
    void customerCancelOnlyBeforeAcceptance() {
        assertTrue(PLACED.canMoveTo(CANCELLED));
        assertFalse(ACCEPTED.canMoveTo(CANCELLED));
    }

    @Test
    void finishedOrdersAreFinal() {
        for (OrderStatus s : new OrderStatus[]{DELIVERED, CANCELLED, REJECTED}) {
            assertTrue(s.next().isEmpty());
        }
    }
}
