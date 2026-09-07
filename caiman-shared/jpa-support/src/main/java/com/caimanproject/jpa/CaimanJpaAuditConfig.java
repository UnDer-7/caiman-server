package com.caimanproject.jpa;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.internal.SessionFactoryImpl;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class CaimanJpaAuditConfig {

    private final EntityManagerFactory entityManagerFactory;

    @PostConstruct
    public void registerAuditListener() {
        EventListenerRegistry registry = entityManagerFactory
                .unwrap(SessionFactoryImpl.class)
                .getServiceRegistry()
                .getService(EventListenerRegistry.class);

        AuditHibernateListener listener = new AuditHibernateListener();
        registry.appendListeners(EventType.PRE_INSERT, listener);
        registry.appendListeners(EventType.PRE_UPDATE, listener);
    }
}
