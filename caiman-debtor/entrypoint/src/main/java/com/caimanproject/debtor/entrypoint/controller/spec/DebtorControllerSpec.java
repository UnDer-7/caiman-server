package com.caimanproject.debtor.entrypoint.controller.spec;

import com.caimanproject.debtor.entrypoint.payload.request.CreateDebtorRequestDto;
import com.caimanproject.debtor.entrypoint.payload.response.DebtorResponseDto;
import com.caimanproject.web.constant.OpenApiConstants;
import com.caimanproject.web.dto.response.ErrorResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@Tag(name = "Debtors", description = "Operations for managing debtor records and their notification contacts.")
public interface DebtorControllerSpec {

    @Operation(
        summary = "Create a new debtor",
        description = """
            Creates a new debtor record with an optional list of contact endpoints.

            A debtor represents a person who owes money and can be enrolled in one or more charge plans.
            Once created, the debtor is active by default and eligible for plan membership assignment.
            Deactivating a debtor hides them from plan assignment but does not affect existing
            memberships, invoices, or payments.
            """
    )
    @ApiResponse(
        responseCode = OpenApiConstants.HttpStatusCodes.CREATED,
        description = """
            Debtor created successfully. Returns the full debtor representation including the
            system-assigned UUID, active status, notification flag, audit timestamps, and the
            resolved contact list.
            """,
        content = @Content(schema = @Schema(implementation = DebtorResponseDto.class))
    )
    @ApiResponse(
        responseCode = OpenApiConstants.HttpStatusCodes.UNPROCESSABLE_ENTITY,
        description = "Business rule violation. The request was well-formed but violated a business constraint. All possible business error scenarios are documented below.",
        content = @Content(
            schema = @Schema(implementation = ErrorResponseDto.class),
            examples = {
                @ExampleObject(
                    name = "Two or more contacts share the same contactType and contactValue",
                    summary = "duplicate contact value",
                    value = """
                        {
                          "code": "DEBTOR_BUSINESS_001",
                          "timestamp": "2026-06-09T17:58:44.646783829Z",
                          "message": "Informed contact list has duplicate contact value",
                          "detail": "Duplicate Contacts: contactType: EMAIL - contactValue: admin@gmail.com - priority: 1",
                          "httpStatusCode": 422
                        }
                        """
                ),
                @ExampleObject(
                    name = "Two or more contacts share the same contactType and priority",
                    summary = "duplicate contact priority",
                    value = """
                        {
                          "code": "DEBTOR_BUSINESS_002",
                          "timestamp": "2026-06-09T18:00:58.607002394Z",
                          "message": "Informed contact list has duplicate contact priority",
                          "detail": "Duplicate Contacts: contactType: EMAIL - contactValue: admin@gmail.com - priority: 1",
                          "httpStatusCode": 422
                        }
                        """
                ),
            }
        )
    )
    DebtorResponseDto createDebtor(@Valid @NotNull CreateDebtorRequestDto payload);

}

