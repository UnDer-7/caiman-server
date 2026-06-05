package com.caimanproject.debtor.entrypoint.payload.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

import java.util.Collections;
import java.util.List;

@Builder
public record CreateDebtorRequestDto(
    @NotBlank
    String name,

    String notes,

    Boolean notificationsEnabled,

    Boolean active,

    @Valid
    List<CreateDebtorContactRequestDto> contacts

) {

    public CreateDebtorRequestDto {
        if (active == null) {
            active = true;
        }
        if (contacts == null) {
            contacts = Collections.emptyList();
        }

        if (notificationsEnabled == null) {
            notificationsEnabled = false;
        }
    }

}
