package com.fooddelivery.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
public class OrderItem extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private CustomerOrder order;

    @Column(name = "menu_item_id", nullable = false)
    private UUID menuItemId;

    /** Name and price are snapshotted so later menu edits don't rewrite history. */
    @Column(nullable = false)
    private String name;

    @Column(name = "unit_price_paise", nullable = false)
    private int unitPricePaise;

    @Column(nullable = false)
    private int quantity;
}
