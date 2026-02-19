package com.inventory.factory.fixture;

import com.inventory.enums.Role;
import com.inventory.model.User;
import org.springframework.security.crypto.password.PasswordEncoder;

public final class UserFixture {

    private UserFixture() {}

    public static User createAdmin(PasswordEncoder passwordEncoder) {
        User admin = new User();
        admin.setEmail("admin@example.com");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setRole(Role.ADMIN);
        admin.setEnabled(true);
        return admin;
    }
}
