package com.fooddelivery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class FoodDeliveryApplication {
    public static void main(String[] args) {
        // Servers should run in UTC. This also avoids the "Asia/Calcutta" name that PostgreSQL 16 rejects.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(FoodDeliveryApplication.class, args);
    }
}
