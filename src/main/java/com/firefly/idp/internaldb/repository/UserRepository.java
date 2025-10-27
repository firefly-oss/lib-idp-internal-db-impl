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

import com.firefly.idp.internaldb.domain.User;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Reactive repository for User entity operations.
 */
@Repository
public interface UserRepository extends R2dbcRepository<User, UUID> {

    /**
     * Find a user by username.
     *
     * @param username the username to search for
     * @return a Mono emitting the user if found
     */
    Mono<User> findByUsername(String username);

    /**
     * Find a user by email.
     *
     * @param email the email to search for
     * @return a Mono emitting the user if found
     */
    Mono<User> findByEmail(String email);

    /**
     * Check if a user exists by username.
     *
     * @param username the username to check
     * @return a Mono emitting true if the user exists
     */
    Mono<Boolean> existsByUsername(String username);

    /**
     * Check if a user exists by email.
     *
     * @param email the email to check
     * @return a Mono emitting true if the user exists
     */
    Mono<Boolean> existsByEmail(String email);
}
