package com.caimanproject.debtor.entrypoint.payload.response;

import com.caimanproject.web.constant.OpenApiConstants;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Audit timestamps for the record. All values are in UTC.")
public record AuditResponseDto(
    @Schema(
        description = "Timestamp when this record was created. Always in UTC.",
        example = OpenApiConstants.Examples.DATE_TIME,
        nullable = false)
    Instant createdAt,

    @Schema(
        description = "Timestamp of the last update to this record. Always in UTC.",
        example = OpenApiConstants.Examples.DATE_TIME,
        nullable = false)
    Instant updatedAt
) {

}
