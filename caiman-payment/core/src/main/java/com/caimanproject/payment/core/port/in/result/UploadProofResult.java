package com.caimanproject.payment.core.port.in.result;

import com.caimanproject.payment.core.domain.types.PaymentProofStatus;
import java.util.UUID;
import lombok.Builder;

@Builder
public record UploadProofResult(UUID proofId, PaymentProofStatus status) {}
