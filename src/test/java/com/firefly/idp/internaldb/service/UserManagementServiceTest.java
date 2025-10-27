package com.firefly.idp.internaldb.service;

import com.firefly.idp.dtos.CreateUserRequest;
import com.firefly.idp.dtos.UpdateUserRequest;
import com.firefly.idp.internaldb.domain.User;
import com.firefly.idp.internaldb.repository.RefreshTokenRepository;
import com.firefly.idp.internaldb.repository.SessionRepository;
import com.firefly.idp.internaldb.repository.UserRepository;
import com.firefly.idp.internaldb.repository.UserRoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserManagementServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserManagementService userManagementService;

    private User testUser;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testUser = User.builder()
                .id(testUserId)
                .username("testuser")
                .email("test@example.com")
                .passwordHash("$2a$10$hashedPassword")
                .firstName("Test")
                .lastName("User")
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .mfaEnabled(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        testUser.markAsNotNew();
    }

    @Test
    void shouldCreateUser() {
        // Given
        CreateUserRequest request = CreateUserRequest.builder()
                .username("newuser")
                .password("Password123!")
                .email("new@example.com")
                .givenName("New")
                .familyName("User")
                .build();

        when(userRepository.existsByUsername("newuser")).thenReturn(Mono.just(false));
        when(userRepository.existsByEmail("new@example.com")).thenReturn(Mono.just(false));
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setCreatedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());
            return Mono.just(user);
        });

        // When & Then
        StepVerifier.create(userManagementService.createUser(request))
                .assertNext(user -> {
                    assertThat(user.getUsername()).isEqualTo("newuser");
                    assertThat(user.getEmail()).isEqualTo("new@example.com");
                    assertThat(user.getFirstName()).isEqualTo("New");
                    assertThat(user.getLastName()).isEqualTo("User");
                    assertThat(user.getEnabled()).isTrue();
                    assertThat(user.getCreatedAt()).isNotNull();
                })
                .verifyComplete();

        verify(passwordEncoder).encode("Password123!");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void shouldGetUserById() {
        // Given
        when(userRepository.findById(testUserId)).thenReturn(Mono.just(testUser));

        // When & Then
        StepVerifier.create(userManagementService.getUserById(testUserId))
                .assertNext(user -> {
                    assertThat(user.getId()).isEqualTo(testUserId);
                    assertThat(user.getUsername()).isEqualTo("testuser");
                })
                .verifyComplete();

        verify(userRepository).findById(testUserId);
    }

    @Test
    void shouldGetUserByUsername() {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(Mono.just(testUser));

        // When & Then
        StepVerifier.create(userManagementService.getUserByUsername("testuser"))
                .assertNext(user -> {
                    assertThat(user.getUsername()).isEqualTo("testuser");
                    assertThat(user.getEmail()).isEqualTo("test@example.com");
                })
                .verifyComplete();

        verify(userRepository).findByUsername("testuser");
    }

    @Test
    void shouldUpdateUser() {
        // Given
        UpdateUserRequest request = UpdateUserRequest.builder()
                .userId(testUserId.toString())
                .email("updated@example.com")
                .givenName("Updated")
                .familyName("Name")
                .enabled(false)
                .build();

        when(userRepository.findById(testUserId)).thenReturn(Mono.just(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        // When & Then
        StepVerifier.create(userManagementService.updateUser(request))
                .assertNext(user -> {
                    assertThat(user.getEmail()).isEqualTo("updated@example.com");
                    assertThat(user.getFirstName()).isEqualTo("Updated");
                    assertThat(user.getLastName()).isEqualTo("Name");
                    assertThat(user.getEnabled()).isFalse();
                })
                .verifyComplete();

        verify(userRepository).findById(testUserId);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void shouldChangePassword() {
        // Given
        String oldPassword = "OldPassword123!";
        String newPassword = "NewPassword456!";

        when(userRepository.findById(testUserId)).thenReturn(Mono.just(testUser));
        when(passwordEncoder.matches(oldPassword, testUser.getPasswordHash())).thenReturn(true);
        when(passwordEncoder.encode(newPassword)).thenReturn("$2a$10$newEncodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(Mono.just(testUser));

        // When & Then
        StepVerifier.create(userManagementService.changePassword(testUserId, oldPassword, newPassword))
                .verifyComplete();

        verify(userRepository).findById(testUserId);
        verify(passwordEncoder).matches(eq(oldPassword), anyString());
        verify(passwordEncoder).encode(newPassword);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void shouldFailChangePasswordWithInvalidOldPassword() {
        // Given
        String oldPassword = "WrongPassword";
        String newPassword = "NewPassword456!";

        when(userRepository.findById(testUserId)).thenReturn(Mono.just(testUser));
        when(passwordEncoder.matches(oldPassword, testUser.getPasswordHash())).thenReturn(false);

        // When & Then
        StepVerifier.create(userManagementService.changePassword(testUserId, oldPassword, newPassword))
                .expectErrorMessage("Invalid old password")
                .verify();

        verify(userRepository).findById(testUserId);
        verify(passwordEncoder).matches(oldPassword, testUser.getPasswordHash());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldResetPassword() {
        // Given
        String newPassword = "NewPassword456!";

        when(userRepository.findByUsername("testuser")).thenReturn(Mono.just(testUser));
        when(passwordEncoder.encode(newPassword)).thenReturn("$2a$10$newEncodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(Mono.just(testUser));

        // When & Then
        StepVerifier.create(userManagementService.resetPassword("testuser", newPassword))
                .verifyComplete();

        verify(userRepository).findByUsername("testuser");
        verify(passwordEncoder).encode(newPassword);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void shouldDeleteUser() {
        // Given
        when(userRepository.findById(testUserId)).thenReturn(Mono.just(testUser));
        when(refreshTokenRepository.deleteByUserId(testUserId)).thenReturn(Mono.empty());
        when(sessionRepository.deleteByUserId(testUserId)).thenReturn(Mono.empty());
        when(userRoleRepository.deleteByUserId(testUserId)).thenReturn(Mono.empty());
        when(userRepository.delete(testUser)).thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(userManagementService.deleteUser(testUserId))
                .verifyComplete();

        verify(userRepository).findById(testUserId);
        verify(refreshTokenRepository).deleteByUserId(testUserId);
        verify(sessionRepository).deleteByUserId(testUserId);
        verify(userRoleRepository).deleteByUserId(testUserId);
        verify(userRepository).delete(testUser);
    }

    @Test
    void shouldSetUserEnabled() {
        // Given
        when(userRepository.findById(testUserId)).thenReturn(Mono.just(testUser));
        when(userRepository.save(any(User.class))).thenReturn(Mono.just(testUser));

        // When & Then
        StepVerifier.create(userManagementService.setUserEnabled(testUserId, false))
                .verifyComplete();

        verify(userRepository).findById(testUserId);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void shouldSetUserLocked() {
        // Given
        when(userRepository.findById(testUserId)).thenReturn(Mono.just(testUser));
        when(userRepository.save(any(User.class))).thenReturn(Mono.just(testUser));

        // When & Then
        StepVerifier.create(userManagementService.setUserLocked(testUserId, true))
                .verifyComplete();

        verify(userRepository).findById(testUserId);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void shouldReturnErrorWhenUserNotFound() {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(userRepository.findById(nonExistentId)).thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(userManagementService.getUserById(nonExistentId))
                .verifyComplete(); // Empty mono

        verify(userRepository).findById(nonExistentId);
    }
}
