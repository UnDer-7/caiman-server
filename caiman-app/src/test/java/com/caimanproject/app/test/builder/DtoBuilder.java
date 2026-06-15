package com.caimanproject.app.test.builder;

import com.caimanproject.debtor.core.domain.types.ContactType;
import com.caimanproject.debtor.entrypoint.payload.request.CreateDebtorContactRequestDto;
import com.caimanproject.debtor.entrypoint.payload.request.CreateDebtorRequestDto;
import java.util.List;

public final class DtoBuilder {

    private DtoBuilder() {
        throw new IllegalStateException("Utility class");
    }

    public static CreateDebtorRequestDto.CreateDebtorRequestDtoBuilder buildCreateDebtorRequestDto() {
        return CreateDebtorRequestDto.builder()
                .name("John Doe")
                .notes("Lorem ipsum dolor sit amet")
                .notificationsEnabled(true)
                .contacts(List.of(buildCreateDebtorContactRequestDto().build()));
    }

    public static CreateDebtorContactRequestDto.CreateDebtorContactRequestDtoBuilder
            buildCreateDebtorContactRequestDto() {
        return CreateDebtorContactRequestDto.builder()
                .contactType(ContactType.EMAIL)
                .contactValue("johndoe@example.com")
                .priority(1);
    }
}
