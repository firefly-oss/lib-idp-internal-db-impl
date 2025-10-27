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

import com.firefly.idp.internaldb.domain.Session;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Reactive repository for Session entity operations.
 */
@Repository
public interface SessionRepository extends R2dbcRepository<Session, UUID> {

    /**
     * Find all sessions for a specific user.
     *
     * @param userId the user ID
     * @return a Flux emitting all sessions for the user
     */
    Flux<Session> findByUserId(UUID userId);

    /**
     * Find all active (non-revoked) sessions for a user.
     *
     * @param userId the user ID
     * @param revoked the revocation status (false for active)
     * @return a Flux emitting active sessions
     */
    Flux<Session> findByUserIdAndRevoked(UUID userId, Boolean revoked);

    /**
     * Find a session by access token JTI.
     *
     * @param accessTokenJti the JTI (JWT ID) from the access token
     * @return a Mono emitting the session if found
     */
    Mono<Session> findByAccessTokenJti(String accessTokenJti);

    /**
     * Delete all sessions for a user.
     *
     * @param userId the user ID
     * @return a Mono emitting the number of deleted records
     */
    @Modifying
    @Query("DELETE FROM idp_sessions WHERE user_id = :userId")
    Mono<Integer> deleteByUserId(UUID userId);
}
