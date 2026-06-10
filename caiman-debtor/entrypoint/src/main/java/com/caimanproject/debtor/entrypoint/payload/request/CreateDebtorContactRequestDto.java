package com.caimanproject.debtor.entrypoint.payload.request;

import com.caimanproject.debtor.core.domain.types.ContactType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Schema(description = "A single contact endpoint for a debtor, identifying both the channel and the address used by the notification dispatcher.")
@Builder
public record CreateDebtorContactRequestDto(
    @Schema(
        description = """
            Notification channel. One of: EMAIL, MOBILE_PHONE, WHATSAPP, TELEGRAM. \
            EMAIL — standard SMTP address. \
            MOBILE_PHONE — E.164 phone number (e.g. +5561986823666). \
            WHATSAPP — E.164 number; integration adapter constructs the JID. \
            TELEGRAM — username without @ or E.164 phone number.
            """,
        example = "EMAIL",
        nullable = false,
        requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    ContactType contactType,

    @Schema(
        description = """
            The actual contact address for the given contactType. \
            EMAIL — standard email address (e.g. user@example.com). \
            MOBILE_PHONE — E.164 (e.g. +5561986823666). \
            WHATSAPP — E.164 number. \
            TELEGRAM — username (without @) or E.164 phone number.
            """,
        example = "user@example.com",
        nullable = false,
        requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Size(max = 500)
    String contactValue,

    @Schema(
        description = """
            Dispatch priority within the same (debtor, contactType) group. \
            Lower value = higher priority. 1 is the primary contact. \
            Huginn picks the entry with the lowest priority when routing. \
            Defaults to 1 when not provided.
            """,
        example = "1",
        nullable = true,
        requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @Positive
    Integer priority
) {

    public CreateDebtorContactRequestDto {
        if (priority == null) {
            priority = 1;
        }
    }
}
