package com.caimanproject.billing.infrastructure.database.adapter;

import com.caimanproject.billing.core.domain.model.Invoice;
import com.caimanproject.billing.core.port.out.InvoiceSearchGateway;
import com.caimanproject.billing.infrastructure.database.mapper.InvoiceEntityMapper;
import com.caimanproject.billing.infrastructure.database.repository.InvoiceRepository;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class InvoiceSearchAdapter implements InvoiceSearchGateway {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceEntityMapper invoiceEntityMapper;

    @Override
    @Transactional(readOnly = true)
    public Optional<Long> findMaxCycleIndex(final UUID chargePlanId) {
        return invoiceRepository.findMaxCycleIndex(chargePlanId.toString());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsGeneratedOn(final UUID chargePlanId, final LocalDate generationDate) {
        return invoiceRepository.existsByChargePlanIdAndGenerationDate(chargePlanId.toString(), generationDate);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsAny(final UUID chargePlanId) {
        return invoiceRepository.existsByChargePlanId(chargePlanId.toString());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Invoice> findById(final UUID id) {
        return invoiceRepository.findById(id.toString()).map(invoiceEntityMapper::toModel);
    }
}
