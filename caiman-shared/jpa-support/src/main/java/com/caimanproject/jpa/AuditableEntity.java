package com.caimanproject.jpa;

public interface AuditableEntity {

    AuditEmbeddable getAudit();

    void setAudit(AuditEmbeddable audit);
}
