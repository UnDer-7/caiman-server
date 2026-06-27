package com.caimanproject.jpa;

import java.time.Instant;
import org.hibernate.event.spi.PreInsertEvent;
import org.hibernate.event.spi.PreInsertEventListener;
import org.hibernate.event.spi.PreUpdateEvent;
import org.hibernate.event.spi.PreUpdateEventListener;

public class AuditHibernateListener implements PreInsertEventListener, PreUpdateEventListener {

    @Override
    public boolean onPreInsert(PreInsertEvent event) {
        if (event.getEntity() instanceof AuditableEntity entity) {
            Instant now = Instant.now();
            AuditEmbeddable audit = AuditEmbeddable.builder().createdAt(now).updatedAt(now).build();
            entity.setAudit(audit);
            setState(event.getState(), event.getPersister().getPropertyNames(), "audit", audit);
        }
        return false;
    }

    @Override
    public boolean onPreUpdate(PreUpdateEvent event) {
        if (event.getEntity() instanceof AuditableEntity entity) {
            Instant now = Instant.now();
            AuditEmbeddable audit = entity.getAudit();
            if (audit != null) {
                audit.setUpdatedAt(now);
            } else {
                audit = AuditEmbeddable.builder().createdAt(now).updatedAt(now).build();
                entity.setAudit(audit);
            }
            setState(event.getState(), event.getPersister().getPropertyNames(), "audit", audit);
        }
        return false;
    }

    private void setState(Object[] state, String[] names, String property, Object value) {
        for (int i = 0; i < names.length; i++) {
            if (names[i].equals(property)) {
                state[i] = value;
                return;
            }
        }
    }
}
