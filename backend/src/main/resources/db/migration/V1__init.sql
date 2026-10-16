CREATE TABLE users (
  id            UUID PRIMARY KEY,
  email         VARCHAR(255) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  name          VARCHAR(120) NOT NULL,
  role          VARCHAR(20)  NOT NULL CHECK (role IN ('CUSTOMER','RESTAURANT','ADMIN')),
  created_at    TIMESTAMPTZ  NOT NULL,
  updated_at    TIMESTAMPTZ  NOT NULL
);

CREATE TABLE restaurants (
  id          UUID PRIMARY KEY,
  owner_id    UUID NOT NULL REFERENCES users(id),
  name        VARCHAR(160) NOT NULL,
  description VARCHAR(1000),
  city        VARCHAR(80)  NOT NULL,
  created_at  TIMESTAMPTZ  NOT NULL,
  updated_at  TIMESTAMPTZ  NOT NULL
);
CREATE INDEX idx_restaurants_owner ON restaurants(owner_id);
CREATE INDEX idx_restaurants_city  ON restaurants(lower(city));

CREATE TABLE menu_items (
  id            UUID PRIMARY KEY,
  restaurant_id UUID NOT NULL REFERENCES restaurants(id),
  name          VARCHAR(160) NOT NULL,
  description   VARCHAR(500),
  price_paise   INTEGER NOT NULL CHECK (price_paise > 0),
  available     BOOLEAN NOT NULL DEFAULT TRUE,
  created_at    TIMESTAMPTZ NOT NULL,
  updated_at    TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_menu_items_restaurant ON menu_items(restaurant_id);

CREATE TABLE orders (
  id            UUID PRIMARY KEY,
  customer_id   UUID NOT NULL REFERENCES users(id),
  restaurant_id UUID NOT NULL REFERENCES restaurants(id),
  status        VARCHAR(30) NOT NULL,
  total_paise   BIGINT NOT NULL CHECK (total_paise > 0),
  created_at    TIMESTAMPTZ NOT NULL,
  updated_at    TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_orders_customer   ON orders(customer_id, created_at DESC);
CREATE INDEX idx_orders_restaurant ON orders(restaurant_id, created_at DESC);

CREATE TABLE order_items (
  id               UUID PRIMARY KEY,
  order_id         UUID NOT NULL REFERENCES orders(id),
  menu_item_id     UUID NOT NULL REFERENCES menu_items(id),
  name             VARCHAR(160) NOT NULL,
  unit_price_paise INTEGER NOT NULL CHECK (unit_price_paise > 0),
  quantity         INTEGER NOT NULL CHECK (quantity > 0),
  created_at       TIMESTAMPTZ NOT NULL,
  updated_at       TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_order_items_order ON order_items(order_id);
