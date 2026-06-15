package com.caimanproject.debtor.core.port.in.command;

import java.util.List;
import lombok.Builder;

@Builder
public record CreateDebtorCommand(
        String name, String notes, Boolean notificationsEnabled, List<CreateDebtorContactCommand> contacts) {}
