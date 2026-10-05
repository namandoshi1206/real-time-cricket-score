package com.cricket.service;

import com.cricket.dto.AuthResponse;
import com.cricket.dto.RegisterRequest;
import com.cricket.entity.RoleType;
import com.cricket.entity.User;
import com.cricket.exception.DuplicateResourceException;
import com.cricket.exception.ResourceNotFoundException;
import com.cricket.repository.RoleRepository;
import com.cricket.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, RoleRepository roleRepository,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String username = request.username().trim();
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByUsername(username) || userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Username or email is already registered");
        }
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.getRoles().add(roleRepository.findByName(RoleType.VIEWER)
                .orElseThrow(() -> new ResourceNotFoundException("Viewer role is not initialized")));
        userRepository.save(user);
        return new AuthResponse(username, Set.of(RoleType.VIEWER.name()));
    }

    public AuthResponse currentUser(String username) {
        User user = userRepository.findWithRolesByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return new AuthResponse(user.getUsername(), user.getRoles().stream()
                .map(role -> role.getName().name()).collect(java.util.stream.Collectors.toUnmodifiableSet()));
    }
}