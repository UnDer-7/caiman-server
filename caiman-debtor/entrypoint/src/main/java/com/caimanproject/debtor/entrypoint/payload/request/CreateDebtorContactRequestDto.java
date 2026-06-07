package com.caimanproject.debtor.entrypoint.payload.request;

import com.caimanproject.debtor.core.domain.types.ContactType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record CreateDebtorContactRequestDto(
    @NotNull
    ContactType contactType,

    @NotBlank
    @Size(max = 500)
    String contactValue,

    @Positive
    Integer priority
) {

    public CreateDebtorContactRequestDto {
        if (priority == null) {
            priority = 1;
        }
    }
}
