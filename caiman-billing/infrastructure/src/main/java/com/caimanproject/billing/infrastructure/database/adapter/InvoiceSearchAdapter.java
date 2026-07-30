package com.caimanproject.billing.infrastructure.database.adapter;

import com.caimanproject.billing.core.port.out.InvoiceSearchGateway;
import com.caimanproject.billing.infrastructure.database.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class InvoiceSearchAdapter implements InvoiceSearchGateway {

    private final InvoiceRepository invoiceRepository;

    @Override
    public Optional<Long> findMaxCycleIndex(final UUID chargePlanId) {
        return invoiceRepository.findMaxCycleIndex(chargePlanId.toString());
    }

}
