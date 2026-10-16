# Project: Food Delivery App

## What this product does
Connects customers and restaurants. Customers browse restaurants and menus and place orders;
restaurant owners manage menus and move orders through a status flow.

## Stack (do not change without asking)
- Backend: Java 17, Spring Boot 3.3 (modular monolith), Spring Security + JWT
- Database: PostgreSQL 16, Flyway migrations, Spring Data JPA
- Frontend: React 18 + Vite + Tailwind 4 + react-router (frontend/)
- Later: Redis cache, Razorpay (test mode)

## Rules
- Never commit secrets. Config comes from environment variables.
- Every schema change is a new Flyway migration in backend/src/main/resources/db/migration.
- Money is integer paise. Prices always come from the DB, never the client.
- Every endpoint: validate input, check auth, check ownership (return 404 for other users' resources).
- Business logic in service/, controllers stay thin.
- Run `mvn test` before calling a task done.

## Commands
- DB: docker compose up -d
- Run: cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=dev
- Test: cd backend && mvn test
