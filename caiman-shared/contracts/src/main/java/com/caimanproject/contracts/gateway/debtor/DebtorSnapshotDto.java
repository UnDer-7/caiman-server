package com.caimanproject.contracts.gateway.debtor;

import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder(toBuilder = true)
public record DebtorSnapshotDto(
        UUID id, String name, Boolean notificationsEnabled, List<DebtorContactSnapshotDto> contacts) {}
