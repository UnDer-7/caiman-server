package com.caimanproject.payment.core.domain.model;

import com.caimanproject.contracts.exception.DomainException;
import com.caimanproject.contracts.util.DomainValidation;
import com.caimanproject.payment.core.domain.types.DomainExceptionCode;
import com.caimanproject.payment.core.domain.types.PaymentProofStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class PaymentProof {

    @Getter(AccessLevel.NONE)
    private final UUID id;

    private final UUID invoiceId;

    private final String filePath;

    private final String originalFilename;

    private final String fileContentType;

    private final long fileSizeBytes;

    private final String uploadToken;

    @Getter(AccessLevel.NONE)
    private final BigDecimal aiExtractedValue;

    @Getter(AccessLevel.NONE)
    private final BigDecimal finalValue;

    private final boolean requiresManualReview;

    @Getter(AccessLevel.NONE)
    private final String aiRawResponse;

    private final PaymentProofStatus status;

    private final Audit audit;

    @Builder(builderMethodName = "restoreBuilder", builderClassName = "RestoreBuilder")
    public PaymentProof(
            final UUID id,
            final UUID invoiceId,
            final String filePath,
            final String originalFilename,
            final String fileContentType,
            final long fileSizeBytes,
            final String uploadToken,
            final BigDecimal aiExtractedValue,
            final BigDecimal finalValue,
            final boolean requiresManualReview,
            final String aiRawResponse,
            final PaymentProofStatus status,
            final Audit audit) {

        // Optional
        this.id = id;
        this.aiExtractedValue = aiExtractedValue;
        this.finalValue = finalValue;
        this.aiRawResponse = aiRawResponse;

        // Required
        this.invoiceId = invoiceId;
        this.filePath = filePath;
        this.originalFilename = originalFilename;
        this.fileContentType = fileContentType;
        this.fileSizeBytes = fileSizeBytes;
        this.uploadToken = uploadToken;
        this.requiresManualReview = requiresManualReview;
        this.status = status;
        this.audit = Objects.requireNonNullElseGet(audit, Audit::new);

        final var fieldValidations = DomainValidation.validateAll(List.of(
                DomainValidation.validate(invoiceId, "$.invoiceId", DomainExceptionCode.INVALID_VALUE),
                DomainValidation.validate(filePath, "$.filePath", DomainExceptionCode.INVALID_VALUE),
                DomainValidation.validate(originalFilename, "$.originalFilename", DomainExceptionCode.INVALID_VALUE),
                DomainValidation.validate(fileContentType, "$.fileContentType", DomainExceptionCode.INVALID_VALUE),
                DomainValidation.validate(uploadToken, "$.uploadToken", DomainExceptionCode.INVALID_VALUE),
                DomainValidation.validate(status, "$.status", DomainExceptionCode.INVALID_VALUE)));

        fieldValidations.throwIfInvalid(DomainException::new);
    }

    @Builder(builderMethodName = "createBuilder", builderClassName = "CreateBuilder")
    public PaymentProof(
            final UUID invoiceId,
            final String filePath,
            final String originalFilename,
            final String fileContentType,
            final long fileSizeBytes,
            final String uploadToken,
            final PaymentProofStatus status,
            final boolean requiresManualReview) {
        this(
                null,
                invoiceId,
                filePath,
                originalFilename,
                fileContentType,
                fileSizeBytes,
                uploadToken,
                null,
                null,
                requiresManualReview,
                null,
                status,
                null);
    }

    public Optional<UUID> getId() {
        return Optional.ofNullable(id);
    }

    public Optional<BigDecimal> getAiExtractedValue() {
        return Optional.ofNullable(aiExtractedValue);
    }

    public Optional<BigDecimal> getFinalValue() {
        return Optional.ofNullable(finalValue);
    }

    public Optional<String> getAiRawResponse() {
        return Optional.ofNullable(aiRawResponse);
    }
}
