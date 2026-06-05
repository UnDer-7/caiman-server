package com.caimanproject.debtor.entrypoint.payload.response;

import com.caimanproject.debtor.core.domain.types.ContactType;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record DebtorContactResponseDto(
    UUID id,

    ContactType contactType,

    @NotBlank
    String contactValue,

    Integer priority,

    AuditResponseDto audit
) {

    public DebtorContactResponseDto {
        if (priority == null) {
            priority = 1;
        }
    }
}
