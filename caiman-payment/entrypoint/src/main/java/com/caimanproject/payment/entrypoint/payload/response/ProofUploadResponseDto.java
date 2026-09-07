package com.caimanproject.payment.entrypoint.payload.response;

import java.util.UUID;
import lombok.Builder;

@Builder
public record ProofUploadResponseDto(UUID proofId, String message) {}
