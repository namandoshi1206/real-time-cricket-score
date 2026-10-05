package com.cricket.config;

import com.cricket.entity.RoleType;
import com.cricket.entity.User;
import com.cricket.repository.RoleRepository;
import com.cricket.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Order(2)
public class AdminUserInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final String username;
    private final String password;

    public AdminUserInitializer(UserRepository userRepository, RoleRepository roleRepository,
                                PasswordEncoder passwordEncoder,
                                @Value("${APP_ADMIN_USERNAME:}") String username,
                                @Value("${APP_ADMIN_PASSWORD:}") String password) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.username = username;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (username.isBlank() || password.isBlank() || userRepository.existsByUsername(username)) {
            return;
        }
        User admin = new User();
        admin.setUsername(username.trim());
        admin.setEmail(username.trim().toLowerCase() + "@local.invalid");
        admin.setPassword(passwordEncoder.encode(password));
        roleRepository.findByName(RoleType.ADMIN).ifPresent(admin.getRoles()::add);
        userRepository.save(admin);
    }
}