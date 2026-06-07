package com.caimanproject.debtor.core.port.in.command;

import lombok.Builder;

import java.util.List;

@Builder
public record CreateDebtorCommand(
    String name,
    String notes,
    Boolean notificationsEnabled,
    List<CreateDebtorContactCommand> contacts
) {

}
