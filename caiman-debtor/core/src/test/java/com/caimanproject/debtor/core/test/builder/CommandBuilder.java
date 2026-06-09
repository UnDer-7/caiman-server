package com.caimanproject.debtor.core.test.builder;

import com.caimanproject.debtor.core.domain.types.ContactType;
import com.caimanproject.debtor.core.port.in.command.CreateDebtorCommand;
import com.caimanproject.debtor.core.port.in.command.CreateDebtorContactCommand;

import java.util.List;

public final class CommandBuilder {

    private  CommandBuilder() {
        throw new IllegalStateException("Utility class");
    }

    public static CreateDebtorCommand.CreateDebtorCommandBuilder buildCreateDebtorCommandFull() {
        return CreateDebtorCommand.builder()
            .name("John Doe")
            .notes("Lorem ipsum")
            .notificationsEnabled(true)
            .contacts(List.of(buildCreateDebtorContactCommand().build()));
    }

    public static CreateDebtorContactCommand.CreateDebtorContactCommandBuilder buildCreateDebtorContactCommand() {
        return CreateDebtorContactCommand.builder()
            .contactType(ContactType.EMAIL)
            .contactValue("johndoe@gmail.com")
            .priority(1);
    }
}
