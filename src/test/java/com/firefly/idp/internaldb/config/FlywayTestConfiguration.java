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

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationInitializer;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;

/**
 * Test configuration for Flyway database migrations.
 * Ensures migrations execute BEFORE R2DBC repositories are initialized.
 */
@Configuration
public class FlywayTestConfiguration {

    /**
     * Create a dedicated JDBC DataSource for Flyway migrations.
     * This is separate from R2DBC configuration.
     */
    @Bean(name = "flywayDataSource")
    public DataSource flywayDataSource(Environment env) {
        String url = env.getProperty("spring.datasource.url");
        String username = env.getProperty("spring.datasource.username");
        String password = env.getProperty("spring.datasource.password");
        String driverClassName = env.getProperty("spring.datasource.driver-class-name");
        
        return DataSourceBuilder.create()
                .url(url)
                .username(username)
                .password(password)
                .driverClassName(driverClassName)
                .build();
    }

    /**
     * Configure Flyway to use the dedicated DataSource.
     */
    @Bean
    public Flyway flyway(DataSource flywayDataSource) {
        Flyway flyway = Flyway.configure()
                .dataSource(flywayDataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .cleanDisabled(false)
                .load();
        return flyway;
    }

    /**
     * Initializer that executes Flyway migrations.
     * This bean is created early to ensure schema exists before R2DBC.
     */
    @Bean
    public FlywayMigrationInitializer flywayInitializer(Flyway flyway) {
        return new FlywayMigrationInitializer(flyway);
    }
}
