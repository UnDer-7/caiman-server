package com.caimanproject.payment.infrastructure.database.repository;

import com.caimanproject.payment.infrastructure.database.entity.PaymentProofEntity;
import com.caimanproject.payment.infrastructure.database.entity.PaymentProofStatus;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentProofRepository extends CrudRepository<PaymentProofEntity, String> {

    boolean existsByInvoiceIdAndStatusNot(String invoiceId, PaymentProofStatus status);
}
