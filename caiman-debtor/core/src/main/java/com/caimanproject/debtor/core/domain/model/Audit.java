package com.caimanproject.debtor.core.domain.model;

import java.time.Instant;
import java.util.Optional;
import lombok.Builder;
import lombok.ToString;

@ToString
public class Audit {

    private final Instant createdAt;

    private final Instant updatedAt;

    @Builder
    public Audit(final Instant createdAt, final Instant updatedAt) {
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Audit() {
        this.createdAt = null;
        this.updatedAt = null;
    }

    public Optional<Instant> getCreatedAt() {
        return Optional.ofNullable(createdAt);
    }

    public Optional<Instant> getUpdatedAt() {
        return Optional.ofNullable(updatedAt);
    }
}
