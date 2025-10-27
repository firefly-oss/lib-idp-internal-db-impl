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

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Spring configuration for Internal Database IDP Adapter.
 * 
 * <p>This configuration class:
 * <ul>
 *   <li>Enables Internal DB IDP configuration properties</li>
 *   <li>Scans the internal DB adapter package for components</li>
 *   <li>Enables R2DBC repositories</li>
 *   <li>Configures BCrypt password encoder</li>
 *   <li>Is automatically loaded when provider=internal-db</li>
 * </ul>
 */
@Configuration
@EnableConfigurationProperties(InternalDbProperties.class)
@EnableR2dbcRepositories(basePackages = "com.firefly.idp.internaldb.repository")
@ComponentScan(basePackages = "com.firefly.idp.internaldb")
@Slf4j
public class InternalDbIdpConfiguration {

    public InternalDbIdpConfiguration() {
        log.info("Internal Database IDP Adapter Configuration loaded");
    }

    /**
     * Configure BCrypt password encoder with strength 12.
     *
     * @return the password encoder bean
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        log.info("Configuring BCryptPasswordEncoder with strength 12");
        return new BCryptPasswordEncoder(12);
    }
}
