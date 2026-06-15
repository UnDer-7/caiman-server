package com.caimanproject.debtor.entrypoint.payload.response;

import com.caimanproject.debtor.core.domain.types.ContactType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import lombok.Builder;

@Schema(description = "A resolved contact endpoint belonging to a debtor.")
@Builder
public record DebtorContactResponseDto(
        @Schema(
                description = "System-assigned UUID for this contact entry.",
                example = "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                nullable = false)
        UUID id,

        @Schema(description = "Notification channel. One of: EMAIL.", example = "EMAIL", nullable = false)
        ContactType contactType,

        @Schema(description = """
            The actual contact address for the given contactType. \
            EMAIL — standard email address (e.g. user@example.com).
            """, example = "user@example.com", nullable = false) @NotBlank
        String contactValue,

        @Schema(
                description =
                        "Dispatch priority within the same (debtor, contactType) group. Lower value = higher priority. 1 is the primary contact.",
                example = "1",
                nullable = false)
        Integer priority,

        @Schema(description = "Audit timestamps for this contact record.", nullable = false)
        AuditResponseDto audit) {

    public DebtorContactResponseDto {
        if (priority == null) {
            priority = 1;
        }
    }
}
