package com.caimanproject.contracts.event;

import java.util.UUID;

public record MessageEvent<T>(Metadata metadata, T payload) {

    public static record Metadata(UUID correlationId, String eventType, String idempotencyId) {}
}
