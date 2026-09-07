package com.caimanproject.payment.core.domain.service;

import com.caimanproject.contracts.exception.NotFoundException;
import com.caimanproject.contracts.gateway.invoice.InvoiceGateway;
import com.caimanproject.contracts.gateway.invoice.InvoiceSnapshotDto;
import com.caimanproject.contracts.validation.ValidationError;
import com.caimanproject.payment.core.domain.model.ProofPageView;
import com.caimanproject.payment.core.domain.types.BusinessExceptionCode;
import com.caimanproject.payment.core.port.in.GetProofPageUseCase;
import com.caimanproject.payment.core.port.in.command.GetProofPageCommand;
import com.caimanproject.payment.core.port.out.PaymentProofQueryGateway;
import java.util.Collections;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetProofPageService implements GetProofPageUseCase {

    private static final String STATUS_PAID = "PAID";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final Set<String> FORM_DISABLED_STATUSES = Set.of(STATUS_PAID, STATUS_CANCELLED);

    private final InvoiceGateway invoiceGateway;
    private final PaymentProofQueryGateway paymentProofQueryGateway;

    @Override
    public ProofPageView execute(final GetProofPageCommand command) {
        final var invoice = invoiceGateway
                .findByUploadToken(command.token())
                .orElseThrow(() -> new NotFoundException(Collections.singletonList(ValidationError.builder()
                        .code(BusinessExceptionCode.INVOICE_NOT_FOUND)
                        .build())));

        final boolean activeProofExists = paymentProofQueryGateway.existsActiveProof(invoice.id());
        final boolean formEnabled = !FORM_DISABLED_STATUSES.contains(invoice.status()) && !activeProofExists;

        return ProofPageView.builder()
                .chargePlanName(invoice.chargePlanName())
                .debtorName(invoice.debtorName())
                .amountDue(invoice.amountDue())
                .amountPaid(invoice.amountPaid())
                .dueDate(invoice.dueDate())
                .cycleIndex(invoice.cycleIndex())
                .formEnabled(formEnabled)
                .statusMessage(resolveStatusMessage(invoice, activeProofExists, formEnabled))
                .build();
    }

    private static String resolveStatusMessage(
            final InvoiceSnapshotDto invoice, final boolean activeProofExists, final boolean formEnabled) {
        if (formEnabled) {
            return null;
        }
        if (STATUS_PAID.equals(invoice.status())) {
            return "This invoice has already been paid.";
        }
        if (STATUS_CANCELLED.equals(invoice.status())) {
            return "This invoice has been cancelled.";
        }
        if (activeProofExists) {
            return "A proof is already under review for this invoice.";
        }
        return null;
    }
}
