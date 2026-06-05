package com.caimanproject.debtor.entrypoint.payload.response;

import com.caimanproject.debtor.entrypoint.payload.request.CreateDebtorContactRequestDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Builder
public record DebtorResponseDto(
    UUID id,

    String name,

    String notes,

    Boolean notificationsEnabled,

    Boolean active,

    AuditResponseDto audit,

    List<DebtorContactResponseDto> contacts

) {

    public DebtorResponseDto {
        if (contacts == null) {
            contacts = Collections.emptyList();
        }
    }
}
