package com.caimanproject.debtor.core.port.in.command;

import com.caimanproject.debtor.core.domain.types.ContactType;
import lombok.Builder;

@Builder
public record CreateDebtorContactCommand(ContactType contactType, String contactValue, Integer priority) {}
