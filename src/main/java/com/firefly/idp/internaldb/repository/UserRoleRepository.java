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

package com.firefly.idp.internaldb.repository;

import com.firefly.idp.internaldb.domain.UserRole;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Reactive repository for UserRole entity operations.
 */
@Repository
public interface UserRoleRepository extends R2dbcRepository<UserRole, UUID> {

    /**
     * Find all user-role mappings for a specific user.
     *
     * @param userId the user ID
     * @return a Flux emitting all UserRole mappings for the user
     */
    Flux<UserRole> findByUserId(UUID userId);

    /**
     * Find all user-role mappings for a specific role.
     *
     * @param roleId the role ID
     * @return a Flux emitting all UserRole mappings for the role
     */
    Flux<UserRole> findByRoleId(UUID roleId);

    /**
     * Find a specific user-role mapping.
     *
     * @param userId the user ID
     * @param roleId the role ID
     * @return a Mono emitting the UserRole if found
     */
    Mono<UserRole> findByUserIdAndRoleId(UUID userId, UUID roleId);

    /**
     * Delete all role assignments for a user.
     *
     * @param userId the user ID
     * @return a Mono emitting the number of deleted records
     */
    @Modifying
    @Query("DELETE FROM idp_user_roles WHERE user_id = :userId")
    Mono<Integer> deleteByUserId(UUID userId);

    /**
     * Delete a specific user-role assignment.
     *
     * @param userId the user ID
     * @param roleId the role ID
     * @return a Mono emitting the number of deleted records
     */
    @Modifying
    @Query("DELETE FROM idp_user_roles WHERE user_id = :userId AND role_id = :roleId")
    Mono<Integer> deleteByUserIdAndRoleId(UUID userId, UUID roleId);
}
