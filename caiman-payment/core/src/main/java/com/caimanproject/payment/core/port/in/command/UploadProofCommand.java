package com.caimanproject.payment.core.port.in.command;

import com.caimanproject.payment.core.domain.types.PaymentType;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Builder;

@Builder
public record UploadProofCommand(
        UUID token,
        byte[] fileContent,
        String fileContentType,
        String originalFilename,
        PaymentType paymentType,
        BigDecimal declaredAmount) {}
