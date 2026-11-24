package com.ems.config;

import com.ems.domain.Role;
import com.ems.domain.User;
import com.ems.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {
    @Bean
    CommandLineRunner initializeAdmin(UserRepository users, PasswordEncoder passwordEncoder) {
        return args -> {
            if (users.count() == 0) {
                users.save(User.builder()
                        .username("admin")
                        .email("admin@ems.local")
                        .password(passwordEncoder.encode("Admin123!"))
                        .role(Role.ADMIN)
                        .enabled(true)
                        .build());
            }
        };
    }
}
