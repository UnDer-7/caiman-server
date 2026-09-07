package com.caimanproject.contracts.event;

import java.time.Instant;

public record AuditEventDto(Instant createdAt, Instant updatedAt) {}
