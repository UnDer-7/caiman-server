package com.caimanproject.debtor.entrypoint.payload.request;

import com.caimanproject.debtor.core.domain.types.ContactType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record CreateDebtorContactRequestDto(
    @NotNull
    ContactType contactType,

    @NotBlank
    String contactValue,

    Integer priority
) {

    public CreateDebtorContactRequestDto {
        if (priority == null) {
            priority = 1;
        }
    }
}
