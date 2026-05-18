package com.kpiso.api.modules.user;

import com.kpiso.api.modules.user.dto.UpdateUserRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User updateUser(UUID userId, UpdateUserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("El usuario especificado no existe"));

        if (!user.getEmail().equals(request.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("El email ya está en uso por otra cuenta");
        }
        if (!user.getUsername().equals(request.getUsername()) && userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("El nombre de usuario ya está en uso");
        }

        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            if (request.getCurrentPassword() == null || request.getCurrentPassword().trim().isEmpty()) {
                throw new IllegalArgumentException("Debes proporcionar tu contraseña actual para cambiarla");
            }
            if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
                throw new IllegalArgumentException("La contraseña actual es incorrecta");
            }
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setProfilePictureUrl(request.getProfilePictureUrl());

        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(UUID userId, com.kpiso.api.modules.user.dto.DeleteUserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("El usuario especificado no existe"));

        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            throw new IllegalArgumentException("Debes proporcionar tu contraseña para eliminar la cuenta");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("La contraseña es incorrecta");
        }

        user.setDeletedAt(java.time.LocalDateTime.now());
        // Ofuscar datos únicos para permitir re-registro
        String suffix = "-deleted-" + UUID.randomUUID().toString().substring(0, 8);
        user.setEmail(user.getEmail() + suffix);
        user.setUsername(user.getUsername() + suffix);

        userRepository.save(user);
    }
}