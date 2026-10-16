package com.fooddelivery.service;

import com.fooddelivery.domain.Role;
import com.fooddelivery.domain.User;
import com.fooddelivery.dto.Dtos.*;
import com.fooddelivery.repository.UserRepository;
import com.fooddelivery.security.JwtService;
import com.fooddelivery.web.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (req.role() == Role.ADMIN) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ROLE", "Role not allowed for signup");
        }
        String email = req.email().trim().toLowerCase();
        if (users.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "EMAIL_TAKEN", "Email already registered");
        }
        User u = new User();
        u.setEmail(email);
        u.setName(req.name().trim());
        u.setRole(req.role());
        u.setPasswordHash(encoder.encode(req.password()));
        users.save(u);
        return new AuthResponse(jwt.issue(u), u.getName(), u.getRole());
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        // Same message for "no such user" and "wrong password" so emails can't be enumerated.
        ApiException bad = new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid email or password");
        User u = users.findByEmail(req.email().trim().toLowerCase()).orElseThrow(() -> bad);
        if (!encoder.matches(req.password(), u.getPasswordHash())) throw bad;
        return new AuthResponse(jwt.issue(u), u.getName(), u.getRole());
    }
}
