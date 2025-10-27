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
 * User entity for internal database IDP implementation.
 * Represents a user account with authentication credentials and profile information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("idp_users")
public class User implements Persistable<UUID> {

    @Id
    @Column("id")
    private UUID id;

    @Column("username")
    private String username;

    @Column("email")
    private String email;

    @Column("password_hash")
    private String passwordHash;

    @Column("first_name")
    private String firstName;

    @Column("last_name")
    private String lastName;

    @Column("enabled")
    @Builder.Default
    private Boolean enabled = true;

    @Column("account_non_expired")
    @Builder.Default
    private Boolean accountNonExpired = true;

    @Column("account_non_locked")
    @Builder.Default
    private Boolean accountNonLocked = true;

    @Column("credentials_non_expired")
    @Builder.Default
    private Boolean credentialsNonExpired = true;

    @Column("mfa_enabled")
    @Builder.Default
    private Boolean mfaEnabled = false;

    @Column("mfa_secret")
    private String mfaSecret;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("updated_at")
    private LocalDateTime updatedAt;

    @Column("last_login_at")
    private LocalDateTime lastLoginAt;

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
