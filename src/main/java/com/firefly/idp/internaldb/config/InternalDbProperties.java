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

package com.firefly.idp.internaldb.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for Internal Database IDP implementation.
 *
 * <p>Configuration Example:</p>
 * <pre>
 * firefly:
 *   security-center:
 *     idp:
 *       internal-db:
 *         jwt:
 *           secret: your-secret-key-min-256-bits
 *           issuer: firefly-idp
 *           access-token-expiration: 900000  # 15 minutes
 *           refresh-token-expiration: 604800000  # 7 days
 * </pre>
 */
@Data
@Validated
@ConfigurationProperties(prefix = "firefly.security-center.idp.internal-db")
public class InternalDbProperties {

    /**
     * JWT configuration
     */
    @NotNull
    private JwtConfig jwt = new JwtConfig();

    @Data
    public static class JwtConfig {
        
        /**
         * JWT secret key (must be at least 256 bits / 32 bytes)
         */
        @NotBlank(message = "JWT secret is required")
        private String secret;

        /**
         * JWT issuer claim
         */
        @NotBlank(message = "JWT issuer is required")
        private String issuer = "firefly-idp-internal-db";

        /**
         * Access token expiration time in milliseconds
         * Default: 15 minutes
         */
        private Long accessTokenExpiration = 900000L;

        /**
         * Refresh token expiration time in milliseconds
         * Default: 7 days
         */
        private Long refreshTokenExpiration = 604800000L;
    }
}
