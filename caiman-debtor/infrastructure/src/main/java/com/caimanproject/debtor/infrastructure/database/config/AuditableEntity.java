package com.caimanproject.debtor.infrastructure.database.config;

import com.caimanproject.debtor.infrastructure.database.entity.AuditEmbeddable;

public interface AuditableEntity {

    AuditEmbeddable getAudit();

    void setAudit(AuditEmbeddable audit);
}
