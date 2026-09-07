package com.caimanproject.contracts.gateway.debtor;

import lombok.Builder;

@Builder(toBuilder = true)
public record DebtorContactSnapshotDto(String contactType, String contactValue, Integer priority) {}
