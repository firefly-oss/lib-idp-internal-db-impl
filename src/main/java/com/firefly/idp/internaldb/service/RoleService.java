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

import com.firefly.idp.internaldb.domain.Role;
import com.firefly.idp.internaldb.domain.UserRole;
import com.firefly.idp.internaldb.repository.RoleRepository;
import com.firefly.idp.internaldb.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service for role management operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RoleService {

    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;

    /**
     * Create a new role.
     *
     * @param name the role name
     * @param description the role description
     * @return a Mono emitting the created role
     */
    public Mono<Role> createRole(String name, String description) {
        log.debug("Creating role: {}", name);

        return roleRepository.existsByName(name)
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new RuntimeException("Role already exists: " + name));
                    }

                    LocalDateTime now = LocalDateTime.now();
                    Role role = Role.builder()
                            .id(UUID.randomUUID())
                            .name(name)
                            .description(description)
                            .createdAt(now)
                            .updatedAt(now)
                            .build();

                    return roleRepository.save(role)
                            .doOnNext(savedRole -> savedRole.markAsNotNew());
                });
    }

    /**
     * Create multiple roles.
     *
     * @param roleNames the list of role names to create
     * @return a Flux emitting the created roles
     */
    public Flux<Role> createRoles(List<String> roleNames) {
        return Flux.fromIterable(roleNames)
                .flatMap(roleName -> createRole(roleName, null))
                .onErrorContinue((error, item) -> 
                    log.warn("Failed to create role {}: {}", item, error.getMessage()));
    }

    /**
     * Get a role by name.
     *
     * @param name the role name
     * @return a Mono emitting the role if found
     */
    public Mono<Role> getRoleByName(String name) {
        return roleRepository.findByName(name);
    }

    /**
     * Get all roles.
     *
     * @return a Flux emitting all roles
     */
    public Flux<Role> getAllRoles() {
        return roleRepository.findAll();
    }

    /**
     * Get all roles for a user.
     *
     * @param userId the user ID
     * @return a Flux emitting the role names
     */
    public Flux<String> getUserRoles(UUID userId) {
        return userRoleRepository.findByUserId(userId)
                .flatMap(userRole -> roleRepository.findById(userRole.getRoleId()))
                .map(Role::getName);
    }

    /**
     * Assign a role to a user.
     *
     * @param userId the user ID
     * @param roleName the role name
     * @return a Mono signaling completion
     */
    public Mono<Void> assignRoleToUser(UUID userId, String roleName) {
        log.debug("Assigning role {} to user {}", roleName, userId);

        return roleRepository.findByName(roleName)
                .switchIfEmpty(Mono.error(new RuntimeException("Role not found: " + roleName)))
                .flatMap(role -> {
                    // Check if already assigned
                    return userRoleRepository.findByUserIdAndRoleId(userId, role.getId())
                            .hasElement()
                            .flatMap(exists -> {
                                if (exists) {
                                    log.debug("Role {} already assigned to user {}", roleName, userId);
                                    return Mono.empty();
                                }

                                UserRole userRole = UserRole.builder()
                                        .id(UUID.randomUUID())
                                        .userId(userId)
                                        .roleId(role.getId())
                                        .assignedAt(LocalDateTime.now())
                                        .build();

                                return userRoleRepository.save(userRole)
                                        .doOnNext(saved -> saved.markAsNotNew())
                                        .then();
                            });
                });
    }

    /**
     * Assign multiple roles to a user.
     *
     * @param userId the user ID
     * @param roleNames the list of role names
     * @return a Mono signaling completion
     */
    public Mono<Void> assignRolesToUser(UUID userId, List<String> roleNames) {
        return Flux.fromIterable(roleNames)
                .flatMap(roleName -> assignRoleToUser(userId, roleName))
                .then();
    }

    /**
     * Remove a role from a user.
     *
     * @param userId the user ID
     * @param roleName the role name
     * @return a Mono signaling completion
     */
    public Mono<Void> removeRoleFromUser(UUID userId, String roleName) {
        log.debug("Removing role {} from user {}", roleName, userId);

        return roleRepository.findByName(roleName)
                .switchIfEmpty(Mono.error(new RuntimeException("Role not found: " + roleName)))
                .flatMap(role -> userRoleRepository.deleteByUserIdAndRoleId(userId, role.getId()))
                .then();
    }

    /**
     * Remove multiple roles from a user.
     *
     * @param userId the user ID
     * @param roleNames the list of role names
     * @return a Mono signaling completion
     */
    public Mono<Void> removeRolesFromUser(UUID userId, List<String> roleNames) {
        return Flux.fromIterable(roleNames)
                .flatMap(roleName -> removeRoleFromUser(userId, roleName))
                .then();
    }

    /**
     * Delete a role.
     *
     * @param roleName the role name
     * @return a Mono signaling completion
     */
    public Mono<Void> deleteRole(String roleName) {
        log.debug("Deleting role: {}", roleName);

        return roleRepository.findByName(roleName)
                .switchIfEmpty(Mono.error(new RuntimeException("Role not found: " + roleName)))
                .flatMap(role -> {
                    // First delete all user-role assignments
                    return userRoleRepository.findByRoleId(role.getId())
                            .flatMap(userRole -> userRoleRepository.delete(userRole))
                            .then(roleRepository.delete(role));
                });
    }
}
