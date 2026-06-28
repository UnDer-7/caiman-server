package com.caimanproject.debtor.infrastructure.config;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = "com.caimanproject.debtor.infrastructure.database.repository")
@EntityScan(basePackages = "com.caimanproject.debtor.infrastructure.database.entity")
public class DebtorInfrastructureConfig {}
