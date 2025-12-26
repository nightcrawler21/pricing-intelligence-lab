package com.example.pricinglab.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * JPA configuration enabling auditing and transaction management.
 *
 * Auditing is used to automatically populate created/updated timestamps
 * on entities extending BaseAuditableEntity.
 */
@Configuration
@EnableJpaAuditing
@EnableJpaRepositories(basePackages = "com.example.pricinglab")
@EnableTransactionManagement
public class JpaConfig {
    // Configuration is annotation-driven
}
