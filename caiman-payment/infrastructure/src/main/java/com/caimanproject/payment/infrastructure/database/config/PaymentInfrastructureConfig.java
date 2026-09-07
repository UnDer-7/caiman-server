package com.caimanproject.payment.infrastructure.database.config;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = "com.caimanproject.payment.infrastructure.database.repository")
@EntityScan(basePackages = "com.caimanproject.payment.infrastructure.database.entity")
public class PaymentInfrastructureConfig {}
