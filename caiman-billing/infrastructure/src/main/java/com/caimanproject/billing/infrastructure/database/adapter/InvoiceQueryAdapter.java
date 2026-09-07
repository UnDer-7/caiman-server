package com.caimanproject.billing.infrastructure.database.adapter;

import com.caimanproject.billing.infrastructure.database.entity.InvoiceEntity;
import com.caimanproject.billing.infrastructure.database.mapper.InvoiceSnapshotMapper;
import com.caimanproject.billing.infrastructure.database.repository.InvoiceRepository;
import com.caimanproject.contracts.gateway.debtor.DebtorGateway;
import com.caimanproject.contracts.gateway.debtor.DebtorSnapshotDto;
import com.caimanproject.contracts.gateway.invoice.InvoiceGateway;
import com.caimanproject.contracts.gateway.invoice.InvoiceSnapshotDto;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class InvoiceQueryAdapter implements InvoiceGateway {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceSnapshotMapper invoiceSnapshotMapper;
    private final DebtorGateway debtorGateway;

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public Optional<InvoiceSnapshotDto> findByUploadToken(final UUID token) {
        return invoiceRepository.findByUploadToken(token.toString()).map(this::toSnapshotWithDebtorName);
    }

    private InvoiceSnapshotDto toSnapshotWithDebtorName(final InvoiceEntity entity) {
        final var debtorId = UUID.fromString(entity.getChargePlanMember().getDebtorId());
        final var debtorName =
                debtorGateway.findById(debtorId).map(DebtorSnapshotDto::name).orElse(null);

        return invoiceSnapshotMapper.toSnapshotDto(entity).toBuilder()
                .debtorName(debtorName)
                .build();
    }
}
