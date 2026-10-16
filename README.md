# Food Delivery App

Spring Boot + PostgreSQL backend and a React (Vite + Tailwind) frontend. See `docs/system-design.md` and `CLAUDE.md`.

## Run the backend
Requirements: Java 17, Maven, Docker.

```
docker compose up -d
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```
The `dev` profile uses a local-only JWT secret and creates demo data:
- owner@demo.com / password123 (RESTAURANT)
- customer@demo.com / password123 (CUSTOMER)

In production set `JWT_SECRET` (32+ chars), `DB_URL`, `DB_USER`, `DB_PASSWORD`, `CORS_ORIGINS`. The app refuses to start without a valid `JWT_SECRET`.

## API summary
| Method | Path | Who |
|---|---|---|
| POST | /api/auth/register, /api/auth/login | public |
| GET | /api/restaurants?city=&page=&size= | public |
| GET | /api/restaurants/{id}, /api/restaurants/{id}/menu | public |
| POST | /api/restaurants | RESTAURANT |
| POST/PUT | /api/restaurants/{id}/menu[/{itemId}] | owner |
| GET | /api/owner/restaurants | RESTAURANT (own restaurants) |
| GET | /api/restaurants/{id}/orders | owner |
| POST | /api/orders | CUSTOMER |
| GET | /api/orders/mine | CUSTOMER |
| GET | /api/orders/{id} | the customer or the restaurant owner |
| POST | /api/orders/{id}/cancel | CUSTOMER (only while PLACED) |
| PATCH | /api/orders/{id}/status | owner |

## Try it (curl)
```
curl -X POST localhost:8080/api/auth/login -H "Content-Type: application/json" \
  -d '{"email":"customer@demo.com","password":"password123"}'
curl localhost:8080/api/restaurants
```

## Run the frontend
Requirements: Node.js 20+ (`winget install OpenJS.NodeJS.LTS`).
```
cd frontend
npm install
npm run dev
```
Open http://localhost:5173. The dev server forwards `/api` calls to the backend on port 8080, so the backend must be running.

Demo flow: log in as customer@demo.com, order from Pune Spice Kitchen, then log in as owner@demo.com in another browser window and move the order through its statuses on the Dashboard.
