package com.caimanproject.billing.infrastructure.database.adapter;

import com.caimanproject.billing.core.domain.model.Invoice;
import com.caimanproject.billing.core.port.out.InvoicePersistenceGateway;
import com.caimanproject.billing.infrastructure.database.entity.InvoiceEntity;
import com.caimanproject.billing.infrastructure.database.mapper.InvoiceEntityMapper;
import com.caimanproject.billing.infrastructure.database.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class InvoicePersistenceAdapter implements InvoicePersistenceGateway {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceEntityMapper invoiceEntityMapper;

    @Override
    @Transactional
    public Invoice save(final Invoice invoice) {
        final InvoiceEntity invoiceEntity = invoiceEntityMapper.toEntity(invoice);
        final InvoiceEntity saved = invoiceRepository.save(invoiceEntity);
        return invoiceEntityMapper.toModel(saved);
    }

}
