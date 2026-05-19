package com.kpiso.api.modules.user;

import com.kpiso.api.modules.user.dto.DeleteUserRequest;
import com.kpiso.api.modules.user.dto.UpdateUserRequest;
import com.kpiso.api.testsupport.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User existingUser;

    @BeforeEach
    void setUp() {
        existingUser = TestFixtures.user("old-user", "old@email.com", "current-hash");
    }

    @Test
    void updateUserShouldUpdateProfileAndPassword() {
        UUID userId = existingUser.getId();
        UpdateUserRequest request = UpdateUserRequest.builder()
                .username("new-user")
                .email("new@email.com")
                .profilePictureUrl("https://cdn.example.com/avatar.png")
                .password("new-secret")
                .currentPassword("current-secret")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(userRepository.existsByUsername(request.getUsername())).thenReturn(false);
        when(passwordEncoder.matches("current-secret", "current-hash")).thenReturn(true);
        when(passwordEncoder.encode("new-secret")).thenReturn("new-hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User updatedUser = userService.updateUser(userId, request);

        assertEquals("new-user", updatedUser.getUsername());
        assertEquals("new@email.com", updatedUser.getEmail());
        assertEquals("https://cdn.example.com/avatar.png", updatedUser.getProfilePictureUrl());
        assertEquals("new-hash", updatedUser.getPassword());
        verify(userRepository).save(existingUser);
    }

    @Test
    void updateUserShouldFailWhenUserDoesNotExist() {
        UUID userId = UUID.randomUUID();
        UpdateUserRequest request = UpdateUserRequest.builder()
                .username("new-user")
                .email("new@email.com")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> userService.updateUser(userId, request));

        assertEquals("El usuario especificado no existe", exception.getMessage());
    }

    @Test
    void updateUserShouldFailWhenEmailAlreadyExists() {
        UUID userId = existingUser.getId();
        UpdateUserRequest request = UpdateUserRequest.builder()
                .username("new-user")
                .email("duplicate@email.com")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> userService.updateUser(userId, request));

        assertEquals("El email ya está en uso por otra cuenta", exception.getMessage());
    }

    @Test
    void updateUserShouldFailWhenUsernameAlreadyExists() {
        UUID userId = existingUser.getId();
        UpdateUserRequest request = UpdateUserRequest.builder()
                .username("duplicate-user")
                .email("new@email.com")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(userRepository.existsByUsername(request.getUsername())).thenReturn(true);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> userService.updateUser(userId, request));

        assertEquals("El nombre de usuario ya está en uso", exception.getMessage());
    }

    @Test
    void updateUserShouldRequireCurrentPasswordBeforeChangingPassword() {
        UUID userId = existingUser.getId();
        UpdateUserRequest request = UpdateUserRequest.builder()
                .username("new-user")
                .email("new@email.com")
                .password("new-secret")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(userRepository.existsByUsername(request.getUsername())).thenReturn(false);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> userService.updateUser(userId, request));

        assertEquals("Debes proporcionar tu contraseña actual para cambiarla", exception.getMessage());
    }

    @Test
    void updateUserShouldRejectIncorrectCurrentPassword() {
        UUID userId = existingUser.getId();
        UpdateUserRequest request = UpdateUserRequest.builder()
                .username("new-user")
                .email("new@email.com")
                .password("new-secret")
                .currentPassword("wrong-secret")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(userRepository.existsByUsername(request.getUsername())).thenReturn(false);
        when(passwordEncoder.matches("wrong-secret", "current-hash")).thenReturn(false);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> userService.updateUser(userId, request));

        assertEquals("La contraseña actual es incorrecta", exception.getMessage());
    }

    @Test
    void deleteUserShouldSoftDeleteAndObfuscateUniqueFields() {
        UUID userId = existingUser.getId();
        DeleteUserRequest request = new DeleteUserRequest();
        request.setPassword("current-secret");

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("current-secret", "current-hash")).thenReturn(true);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.deleteUser(userId, request);

        assertNotNull(existingUser.getDeletedAt());
        assertTrue(existingUser.getEmail().startsWith("old@email.com-deleted-"));
        assertTrue(existingUser.getUsername().startsWith("old-user-deleted-"));
        verify(userRepository).save(existingUser);
    }

    @Test
    void deleteUserShouldFailWhenPasswordIsMissing() {
        UUID userId = existingUser.getId();
        DeleteUserRequest request = new DeleteUserRequest();

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> userService.deleteUser(userId, request));

        assertEquals("Debes proporcionar tu contraseña para eliminar la cuenta", exception.getMessage());
    }

    @Test
    void deleteUserShouldFailWhenPasswordIsIncorrect() {
        UUID userId = existingUser.getId();
        DeleteUserRequest request = new DeleteUserRequest();
        request.setPassword("wrong-secret");

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches(eq("wrong-secret"), eq("current-hash"))).thenReturn(false);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> userService.deleteUser(userId, request));

        assertEquals("La contraseña es incorrecta", exception.getMessage());
    }

    @Test
    void deleteUserShouldFailWhenUserDoesNotExist() {
        DeleteUserRequest request = new DeleteUserRequest();
        request.setPassword("current-secret");

        when(userRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> userService.deleteUser(UUID.randomUUID(), request));

        assertEquals("El usuario especificado no existe", exception.getMessage());
    }
}