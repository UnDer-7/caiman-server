package com.caimanproject.billing.infrastructure.database.config;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = "com.caimanproject.billing.infrastructure.database.repository")
@EntityScan(basePackages = "com.caimanproject.billing.infrastructure.database.entity")
public class BillingInfrastructureConfig {}
