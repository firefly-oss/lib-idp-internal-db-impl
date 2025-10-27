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

package com.firefly.idp.internaldb.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * RefreshToken entity for internal database IDP implementation.
 * Represents a refresh token that can be used to obtain new access tokens.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("idp_refresh_tokens")
public class RefreshToken implements Persistable<UUID> {

    @Id
    @Column("id")
    private UUID id;

    @Column("user_id")
    private UUID userId;

    @Column("token_jti")
    private String tokenJti;

    @Column("token_hash")
    private String tokenHash;

    @Column("session_id")
    private UUID sessionId;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("expires_at")
    private LocalDateTime expiresAt;

    @Column("revoked")
    @Builder.Default
    private Boolean revoked = false;

    @Column("revoked_at")
    private LocalDateTime revokedAt;

    @Column("last_used_at")
    private LocalDateTime lastUsedAt;

    @Transient
    private Boolean isNewEntity;

    @Override
    public boolean isNew() {
        // If explicitly set to false, it's not new
        // Otherwise (null or true), it's new
        return isNewEntity == null || isNewEntity;
    }

    public void markAsNotNew() {
        this.isNewEntity = false;
    }
}
