package com.caimanproject.payment.core.domain.strategy;

import com.caimanproject.contracts.gateway.invoice.InvoiceSnapshotDto;
import com.caimanproject.payment.core.domain.types.PaymentType;
import java.math.BigDecimal;
import lombok.Builder;

@Builder
public record ProofValidationContext(
        byte[] fileContent,
        String fileContentType,
        String originalFilename,
        InvoiceSnapshotDto invoice,
        PaymentType paymentType,
        BigDecimal declaredAmount) {}
