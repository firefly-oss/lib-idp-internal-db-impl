/*
 * Copyright 2025 Firefly Software Solutions Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.firefly.idp.internaldb.service;

import com.firefly.idp.dtos.CreateUserRequest;
import com.firefly.idp.dtos.UpdateUserRequest;
import com.firefly.idp.internaldb.domain.User;
import com.firefly.idp.internaldb.repository.RefreshTokenRepository;
import com.firefly.idp.internaldb.repository.SessionRepository;
import com.firefly.idp.internaldb.repository.UserRepository;
import com.firefly.idp.internaldb.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service for user management operations (CRUD).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserManagementService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final SessionRepository sessionRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Create a new user.
     *
     * @param request the create user request
     * @return a Mono emitting the created user
     */
    public Mono<User> createUser(CreateUserRequest request) {
        log.debug("Creating user: {}", request.getUsername());

        return userRepository.existsByUsername(request.getUsername())
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new RuntimeException("Username already exists"));
                    }

                    if (request.getEmail() != null) {
                        return userRepository.existsByEmail(request.getEmail())
                                .flatMap(emailExists -> {
                                    if (emailExists) {
                                        return Mono.error(new RuntimeException("Email already exists"));
                                    }
                                    return createUserEntity(request);
                                });
                    }

                    return createUserEntity(request);
                });
    }

    private Mono<User> createUserEntity(CreateUserRequest request) {
        LocalDateTime now = LocalDateTime.now();
        User user = User.builder()
                .id(UUID.randomUUID())
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getGivenName())
                .lastName(request.getFamilyName())
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .mfaEnabled(false)
                .createdAt(now)
                .updatedAt(now)
                .build();

        return userRepository.save(user)
                .doOnNext(savedUser -> savedUser.markAsNotNew());
    }

    /**
     * Get a user by ID.
     *
     * @param userId the user ID
     * @return a Mono emitting the user if found
     */
    public Mono<User> getUserById(UUID userId) {
        return userRepository.findById(userId)
                .doOnNext(user -> user.markAsNotNew());
    }

    /**
     * Get a user by username.
     *
     * @param username the username
     * @return a Mono emitting the user if found
     */
    public Mono<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .doOnNext(user -> user.markAsNotNew());
    }

    /**
     * Update a user.
     *
     * @param request the update user request
     * @return a Mono emitting the updated user
     */
    public Mono<User> updateUser(UpdateUserRequest request) {
        log.debug("Updating user: {}", request.getUserId());

        UUID userId = UUID.fromString(request.getUserId());

        return userRepository.findById(userId)
                .doOnNext(user -> user.markAsNotNew())
                .switchIfEmpty(Mono.error(new RuntimeException("User not found")))
                .flatMap(user -> {
                    if (request.getEmail() != null) {
                        user.setEmail(request.getEmail());
                    }
                    if (request.getGivenName() != null) {  // DTO uses givenName
                        user.setFirstName(request.getGivenName());
                    }
                    if (request.getFamilyName() != null) {  // DTO uses familyName
                        user.setLastName(request.getFamilyName());
                    }
                    if (request.getEnabled() != null) {
                        user.setEnabled(request.getEnabled());
                    }

                    user.setUpdatedAt(LocalDateTime.now());

                    return userRepository.save(user);
                });
    }

    /**
     * Delete a user.
     *
     * @param userId the user ID
     * @return a Mono signaling completion
     */
    public Mono<Void> deleteUser(UUID userId) {
        log.debug("Deleting user: {}", userId);

        return userRepository.findById(userId)
                .switchIfEmpty(Mono.error(new RuntimeException("User not found")))
                .flatMap(user -> {
                    // Delete all related data
                    return refreshTokenRepository.deleteByUserId(userId)
                            .then(sessionRepository.deleteByUserId(userId))
                            .then(userRoleRepository.deleteByUserId(userId))
                            .then(userRepository.delete(user));
                });
    }

    /**
     * Change a user's password.
     *
     * @param userId the user ID
     * @param oldPassword the old password (for verification)
     * @param newPassword the new password
     * @return a Mono signaling completion
     */
    public Mono<Void> changePassword(UUID userId, String oldPassword, String newPassword) {
        log.debug("Changing password for user: {}", userId);

        return userRepository.findById(userId)
                .doOnNext(user -> user.markAsNotNew())
                .switchIfEmpty(Mono.error(new RuntimeException("User not found")))
                .flatMap(user -> {
                    if (oldPassword != null && !passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
                        return Mono.error(new RuntimeException("Invalid old password"));
                    }

                    user.setPasswordHash(passwordEncoder.encode(newPassword));
                    user.setUpdatedAt(LocalDateTime.now());

                    return userRepository.save(user).then();
                });
    }

    /**
     * Reset a user's password (admin operation - no old password required).
     *
     * @param username the username
     * @param newPassword the new password
     * @return a Mono signaling completion
     */
    public Mono<Void> resetPassword(String username, String newPassword) {
        log.debug("Resetting password for user: {}", username);

        return userRepository.findByUsername(username)
                .doOnNext(user -> user.markAsNotNew())
                .switchIfEmpty(Mono.error(new RuntimeException("User not found")))
                .flatMap(user -> {
                    user.setPasswordHash(passwordEncoder.encode(newPassword));
                    user.setUpdatedAt(LocalDateTime.now());
                    return userRepository.save(user).then();
                });
    }

    /**
     * Enable or disable a user account.
     *
     * @param userId the user ID
     * @param enabled true to enable, false to disable
     * @return a Mono signaling completion
     */
    public Mono<Void> setUserEnabled(UUID userId, boolean enabled) {
        log.debug("Setting user {} enabled status to: {}", userId, enabled);

        return userRepository.findById(userId)
                .doOnNext(user -> user.markAsNotNew())
                .switchIfEmpty(Mono.error(new RuntimeException("User not found")))
                .flatMap(user -> {
                    user.setEnabled(enabled);
                    user.setUpdatedAt(LocalDateTime.now());
                    return userRepository.save(user).then();
                });
    }

    /**
     * Lock or unlock a user account.
     *
     * @param userId the user ID
     * @param locked true to lock, false to unlock
     * @return a Mono signaling completion
     */
    public Mono<Void> setUserLocked(UUID userId, boolean locked) {
        log.debug("Setting user {} locked status to: {}", userId, locked);

        return userRepository.findById(userId)
                .doOnNext(user -> user.markAsNotNew())
                .switchIfEmpty(Mono.error(new RuntimeException("User not found")))
                .flatMap(user -> {
                    user.setAccountNonLocked(!locked);
                    user.setUpdatedAt(LocalDateTime.now());
                    return userRepository.save(user).then();
                });
    }
}
