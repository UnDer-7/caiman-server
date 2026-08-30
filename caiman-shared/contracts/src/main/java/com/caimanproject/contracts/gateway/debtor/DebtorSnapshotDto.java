package com.caimanproject.contracts.gateway.debtor;

import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder(toBuilder = true)
public record DebtorSnapshotDto(
        UUID id, String name, Boolean notificationsEnabled, List<DebtorContactSnapshotDto> contacts) {}
