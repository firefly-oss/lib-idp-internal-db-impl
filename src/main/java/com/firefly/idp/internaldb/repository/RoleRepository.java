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

import com.firefly.idp.internaldb.domain.Role;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Reactive repository for Role entity operations.
 */
@Repository
public interface RoleRepository extends R2dbcRepository<Role, UUID> {

    /**
     * Find a role by name.
     *
     * @param name the role name
     * @return a Mono emitting the role if found
     */
    Mono<Role> findByName(String name);

    /**
     * Find roles by their names.
     *
     * @param names the role names (must be a Collection for IN operator)
     * @return a Flux emitting matching roles
     */
    Flux<Role> findByNameIn(java.util.Collection<String> names);

    /**
     * Check if a role exists by name.
     *
     * @param name the role name
     * @return a Mono emitting true if the role exists
     */
    Mono<Boolean> existsByName(String name);
}
