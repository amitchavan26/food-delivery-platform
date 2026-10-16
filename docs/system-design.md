# System design (v1)

## Problem
Customers want to order food from local restaurants; restaurants want a simple way to receive and manage orders.

## Roles
- CUSTOMER: browse, order, view own orders, cancel before acceptance
- RESTAURANT: own one or more restaurants, manage menu, move orders through statuses
- ADMIN: reserved for later (not creatable via signup)

## Core flows
1. Register / login (JWT)
2. Restaurant owner creates restaurant and menu items
3. Customer browses restaurants (filter by city), views menu, places order
4. Owner accepts/rejects, then PREPARING -> READY -> OUT_FOR_DELIVERY -> DELIVERED
5. Customer views order status

## Order status machine
PLACED -> ACCEPTED | REJECTED | CANCELLED(customer only)
ACCEPTED -> PREPARING -> READY -> OUT_FOR_DELIVERY -> DELIVERED

## Non-functional
- Search/list endpoints paginated, p95 < 300 ms at expected load
- Passwords hashed (BCrypt), JWT access tokens (60 min)
- Server is the source of truth for validation, pricing, permissions

## Out of scope for v1
Delivery partner app and live tracking, admin panel, coupons, reviews, real payments, multi-city logic.

## Open questions
- Payment: Razorpay test mode in milestone 3
- Delivery assignment: simple status change for now
