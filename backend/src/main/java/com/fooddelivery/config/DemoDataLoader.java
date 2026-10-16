package com.fooddelivery.config;

import com.fooddelivery.domain.*;
import com.fooddelivery.repository.MenuItemRepository;
import com.fooddelivery.repository.RestaurantRepository;
import com.fooddelivery.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Dev profile only: creates two demo logins and one restaurant on an empty database. */
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DemoDataLoader implements CommandLineRunner {
    private final UserRepository users;
    private final RestaurantRepository restaurants;
    private final MenuItemRepository menuItems;
    private final PasswordEncoder encoder;

    @Override
    public void run(String... args) {
        if (users.count() > 0) return;
        User owner = user("Demo Owner", "owner@demo.com", Role.RESTAURANT);
        user("Demo Customer", "customer@demo.com", Role.CUSTOMER);

        Restaurant r = new Restaurant();
        r.setOwner(owner);
        r.setName("Pune Spice Kitchen");
        r.setDescription("North Indian and street food favourites");
        r.setCity("Pune");
        restaurants.save(r);

        item(r, "Paneer Butter Masala", "Creamy tomato gravy", 24000);
        item(r, "Butter Naan", "Soft tandoor bread", 4500);
        item(r, "Masala Dosa", "Crisp dosa with potato filling", 12000);
        item(r, "Masala Chai", "Fresh and strong", 3000);
    }

    private User user(String name, String email, Role role) {
        User u = new User();
        u.setName(name);
        u.setEmail(email);
        u.setRole(role);
        u.setPasswordHash(encoder.encode("password123"));
        return users.save(u);
    }

    private void item(Restaurant r, String name, String desc, int paise) {
        MenuItem m = new MenuItem();
        m.setRestaurant(r);
        m.setName(name);
        m.setDescription(desc);
        m.setPricePaise(paise);
        menuItems.save(m);
    }
}
