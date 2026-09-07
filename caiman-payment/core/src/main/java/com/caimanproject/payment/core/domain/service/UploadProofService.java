package com.caimanproject.payment.core.domain.service;

import com.caimanproject.contracts.exception.BusinessException;
import com.caimanproject.contracts.exception.LogField;
import com.caimanproject.contracts.exception.NotFoundException;
import com.caimanproject.contracts.gateway.invoice.InvoiceGateway;
import com.caimanproject.contracts.validation.ValidationError;
import com.caimanproject.payment.core.domain.model.PaymentProof;
import com.caimanproject.payment.core.domain.strategy.ProofValidationContext;
import com.caimanproject.payment.core.domain.strategy.ProofValidationStrategyResolver;
import com.caimanproject.payment.core.domain.types.BusinessExceptionCode;
import com.caimanproject.payment.core.domain.types.PaymentProofStatus;
import com.caimanproject.payment.core.domain.types.ProofValidationMode;
import com.caimanproject.payment.core.port.in.UploadProofUseCase;
import com.caimanproject.payment.core.port.in.command.UploadProofCommand;
import com.caimanproject.payment.core.port.in.result.UploadProofResult;
import com.caimanproject.payment.core.port.out.PaymentProofPersistenceGateway;
import com.caimanproject.payment.core.port.out.PaymentProofQueryGateway;
import com.caimanproject.payment.core.port.out.ProofFileStorageGateway;
import com.caimanproject.payment.core.port.out.command.StoreProofFileCommand;
import java.util.Collections;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.argument.StructuredArguments;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UploadProofService implements UploadProofUseCase {

    private static final String STATUS_PAID = "PAID";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final Set<String> NOT_PAYABLE_STATUSES = Set.of(STATUS_PAID, STATUS_CANCELLED);

    private final InvoiceGateway invoiceGateway;
    private final PaymentProofQueryGateway paymentProofQueryGateway;
    private final ProofValidationStrategyResolver strategyResolver;
    private final ProofFileStorageGateway proofFileStorageGateway;
    private final PaymentProofPersistenceGateway paymentProofPersistenceGateway;

    @Override
    public UploadProofResult execute(final UploadProofCommand command) {
        final var invoice = invoiceGateway
                .findByUploadToken(command.token())
                .orElseThrow(() -> new NotFoundException(Collections.singletonList(ValidationError.builder()
                        .code(BusinessExceptionCode.INVOICE_NOT_FOUND)
                        .build())));

        if (paymentProofQueryGateway.existsActiveProof(invoice.id())) {
            throw new BusinessException(Collections.singletonList(ValidationError.builder()
                    .code(BusinessExceptionCode.ACTIVE_PROOF_EXISTS)
                    .build()));
        }

        if (NOT_PAYABLE_STATUSES.contains(invoice.status())) {
            throw new BusinessException(Collections.singletonList(ValidationError.builder()
                    .code(BusinessExceptionCode.INVOICE_NOT_PAYABLE)
                    .build()));
        }

        final var mode = ProofValidationMode.valueOf(invoice.proofValidationMode());
        final var strategy = strategyResolver.resolve(mode);

        final var context = ProofValidationContext.builder()
                .fileContent(command.fileContent())
                .fileContentType(command.fileContentType())
                .originalFilename(command.originalFilename())
                .invoice(invoice)
                .paymentType(command.paymentType())
                .declaredAmount(command.declaredAmount())
                .build();

        final var status = strategy.resolveStatus(context);

        log.info(
                LogField.Placeholders.SEVEN.getPlaceholder(),
                StructuredArguments.kv(LogField.MSG.label(), "payment proof upload received"),
                StructuredArguments.kv(LogField.INVOICE_ID.label(), invoice.id()),
                StructuredArguments.kv(LogField.PAYMENT_TYPE.label(), command.paymentType()),
                StructuredArguments.kv(LogField.DECLARED_AMOUNT.label(), command.declaredAmount()),
                StructuredArguments.kv(LogField.PROOF_FILE_NAME.label(), command.originalFilename()),
                StructuredArguments.kv(LogField.PROOF_CONTENT_TYPE.label(), command.fileContentType()),
                StructuredArguments.kv(
                        LogField.PROOF_FILE_SIZE_BYTES.label(),
                        command.fileContent() == null ? 0 : command.fileContent().length));

        final var filePath = proofFileStorageGateway.store(StoreProofFileCommand.builder()
                .fileContent(command.fileContent())
                .fileContentType(command.fileContentType())
                .debtorName(invoice.debtorName())
                .chargePlanName(invoice.chargePlanName())
                .cycleIndex(invoice.cycleIndex())
                .build());

        final var proof = PaymentProof.createBuilder()
                .invoiceId(invoice.id())
                .filePath(filePath)
                .originalFilename(command.originalFilename())
                .fileContentType(command.fileContentType())
                .fileSizeBytes(command.fileContent() == null ? 0 : command.fileContent().length)
                .uploadToken(command.token().toString())
                .status(status)
                .requiresManualReview(status == PaymentProofStatus.PENDING_MANUAL_REVIEW)
                .build();

        final var savedProof = paymentProofPersistenceGateway.save(proof);

        return UploadProofResult.builder()
                .proofId(savedProof.getId().orElseThrow())
                .status(savedProof.getStatus())
                .build();
    }
}
