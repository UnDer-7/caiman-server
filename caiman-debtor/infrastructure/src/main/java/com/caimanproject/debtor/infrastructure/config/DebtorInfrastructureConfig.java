package com.caimanproject.debtor.infrastructure.config;

import com.caimanproject.debtor.infrastructure.database.config.AuditHibernateListener;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.internal.SessionFactoryImpl;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableJpaRepositories(basePackages = "com.caimanproject.debtor.infrastructure.database.repository")
@EntityScan(basePackages = "com.caimanproject.debtor.infrastructure.database.entity")
public class DebtorInfrastructureConfig {

    private final EntityManagerFactory entityManagerFactory;

    @PostConstruct
    public void registerAuditListeners() {
        EventListenerRegistry registry = entityManagerFactory
            .unwrap(SessionFactoryImpl.class)
            .getServiceRegistry()
            .getService(EventListenerRegistry.class);

        AuditHibernateListener listener = new AuditHibernateListener();
        registry.appendListeners(EventType.PRE_INSERT, listener);
        registry.appendListeners(EventType.PRE_UPDATE, listener);
    }
}
