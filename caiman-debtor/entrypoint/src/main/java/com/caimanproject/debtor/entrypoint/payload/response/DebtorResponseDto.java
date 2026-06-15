package com.caimanproject.debtor.entrypoint.payload.response;

import com.caimanproject.web.constant.OpenApiConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Schema(
        description =
                "Full representation of a debtor, including identity, notification settings, audit timestamps, and resolved contact list.")
@Builder
public record DebtorResponseDto(
        @Schema(
                description = "System-assigned UUID for this debtor.",
                example = "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                nullable = false)
        UUID id,

        @Schema(
                description = "Full name of the debtor. Used in notifications and UI listings.",
                example = "John Doe",
                nullable = false)
        String name,

        @Schema(
                description = "Free-text notes about this debtor. Internal use only. Not sent in notifications.",
                example = "Prefers to be contacted via WhatsApp.",
                nullable = true)
        String notes,

        @Schema(description = """
            Master switch for notifications for this debtor. Both this flag and \
            charge_plan.notifications_enabled must be true for any notification to be sent.
            """, example = OpenApiConstants.Examples.TRUE, nullable = false)
        Boolean notificationsEnabled,

        @Schema(
                description =
                        "Whether this debtor is active. When false, the debtor is hidden from plan membership assignment but existing memberships, invoices, and payments are not affected.",
                example = OpenApiConstants.Examples.TRUE,
                nullable = false)
        Boolean active,

        @Schema(description = "Audit timestamps for this debtor record.", nullable = false)
        AuditResponseDto audit,

        @Schema(
                description =
                        "Resolved list of contact endpoints for this debtor. Empty list when no contacts are registered.",
                nullable = false)
        List<DebtorContactResponseDto> contacts) {

    public DebtorResponseDto {
        if (contacts == null) {
            contacts = Collections.emptyList();
        }
    }
}
