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

import com.firefly.idp.internaldb.domain.RefreshToken;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Reactive repository for RefreshToken entity operations.
 */
@Repository
public interface RefreshTokenRepository extends R2dbcRepository<RefreshToken, UUID> {

    /**
     * Find a refresh token by its JTI.
     *
     * @param tokenJti the token JTI
     * @return a Mono emitting the refresh token if found
     */
    Mono<RefreshToken> findByTokenJti(String tokenJti);

    /**
     * Find a refresh token by its hash.
     *
     * @param tokenHash the hashed token value
     * @return a Mono emitting the refresh token if found
     */
    Mono<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Find all refresh tokens for a user.
     *
     * @param userId the user ID
     * @return a Flux emitting all refresh tokens for the user
     */
    Flux<RefreshToken> findByUserId(UUID userId);

    /**
     * Find all refresh tokens for a session.
     *
     * @param sessionId the session ID
     * @return a Flux emitting all refresh tokens for the session
     */
    Flux<RefreshToken> findBySessionId(UUID sessionId);

    /**
     * Delete all refresh tokens for a user.
     *
     * @param userId the user ID
     * @return a Mono emitting the number of deleted records
     */
    @Modifying
    @Query("DELETE FROM idp_refresh_tokens WHERE user_id = :userId")
    Mono<Integer> deleteByUserId(UUID userId);

    /**
     * Delete all refresh tokens for a session.
     *
     * @param sessionId the session ID
     * @return a Mono emitting the number of deleted records
     */
    @Modifying
    @Query("DELETE FROM idp_refresh_tokens WHERE session_id = :sessionId")
    Mono<Integer> deleteBySessionId(UUID sessionId);
}
