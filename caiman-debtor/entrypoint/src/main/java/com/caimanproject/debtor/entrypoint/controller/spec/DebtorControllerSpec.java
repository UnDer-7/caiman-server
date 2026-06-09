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

@Tag(name = "Debtor", description = "")
public interface DebtorControllerSpec {

    @Operation(
        summary = "ToDo",
        description = """
            ToDo
            """
    )
    @ApiResponse(
        responseCode = OpenApiConstants.HttpStatusCodes.CREATED,
        description = "ToDo",
        content = @Content(schema = @Schema(implementation = DebtorResponseDto.class))
    )
    @ApiResponse(
        responseCode = OpenApiConstants.HttpStatusCodes.UNPROCESSABLE_ENTITY,
        description = "Business rule violation — all possible error scenarios are documented below", // melhorar texto
        content = @Content(
            schema = @Schema(implementation = ErrorResponseDto.class),
            examples = {
                @ExampleObject(
                    name = "duplicate contact value - ToDo", // descricao um pouco mais detalhada, mas nao muito grande do pq pode ocorrer
                    summary = "ToDo", // breve descricao, como titulo
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
                    name = "duplicate contact priority - ToDo", // descricao um pouco mais detalhada, mas nao muito grande do pq pode ocorrer
                    summary = "ToDo", // breve descricao, como titulo
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
    @ApiResponse(
        responseCode = OpenApiConstants.HttpStatusCodes.INTERNAL_SERVER_ERROR,
        description = "Internal erros", // melhorar texto
        content = @Content(
            schema = @Schema(implementation = ErrorResponseDto.class),
            examples = {
                @ExampleObject(
                    name = "internal validation, All message are the same, the code changes depending on the error - ToDo", // descricao um pouco mais detalhada, mas nao muito grande do pq pode ocorrer
                    summary = "internal validation - ToDo", // breve descricao, como titulo
                    value = """
                        {
                          "code": "DEBTOR_DOMAIN_001",
                          "timestamp": "2026-06-09T17:58:44.646783829Z",
                          "message": "An internal validation failure occurred.",
                          "detail": "null",
                          "httpStatusCode": 500
                        }
                        """
                ),
                @ExampleObject(
                    name = "unexpected erro ocured, All message are the same, the code changes depending on the error - ToDo", // descricao um pouco mais detalhada, mas nao muito grande do pq pode ocorrer
                    summary = "unexpected erro ocured - ToDo", // breve descricao, como titulo
                    value = """
                        {
                          "code": "WEB_SUPPORT_001",
                          "timestamp": "2026-06-09T17:58:44.646783829Z",
                          "message": "An internal server error occurred.",
                          "detail": "null",
                          "httpStatusCode": 500
                        }
                        """
                )
            }
        )
    )
    DebtorResponseDto createDebtor(@Valid @NotNull CreateDebtorRequestDto payload);

}

