package com.caimanproject.debtor.entrypoint.payload.request;

import com.caimanproject.web.constant.OpenApiConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.util.Collections;
import java.util.List;

@Schema(description = "Request payload for creating a new debtor.")
@Builder
public record CreateDebtorRequestDto(
    @Schema(
        description = "Full name of the debtor. Used in notifications and UI listings.",
        example = "John Doe",
        nullable = false,
        requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Size(max = 255)
    String name,

    @Schema(
        description = "Free-text notes about this debtor. Internal use only. Not sent in notifications.",
        example = "Prefers to be contacted via WhatsApp.",
        nullable = true,
        requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    String notes,

    @Schema(
        description = """
            Master switch for notifications for this debtor. Both this flag and \
            charge_plan.notifications_enabled must be true for any notification to be sent. \
            Defaults to false when not provided.
            """,
        example = OpenApiConstants.Examples.TRUE,
        nullable = true,
        requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    Boolean notificationsEnabled,

    @Schema(
        description = "List of contact endpoints for this debtor. Each entry defines a channel and address used by the notification dispatcher. Defaults to empty list when not provided.",
        nullable = true,
        requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @Valid
    List<CreateDebtorContactRequestDto> contacts

) {

    public CreateDebtorRequestDto {
        if (contacts == null) {
            contacts = Collections.emptyList();
        }

        if (notificationsEnabled == null) {
            notificationsEnabled = false;
        }
    }

}
