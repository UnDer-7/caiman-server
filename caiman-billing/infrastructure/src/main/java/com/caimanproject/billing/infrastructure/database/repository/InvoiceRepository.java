package com.caimanproject.billing.infrastructure.database.repository;

import com.caimanproject.billing.infrastructure.database.entity.InvoiceEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InvoiceRepository extends CrudRepository<InvoiceEntity, String> {

    @Query("""
        SELECT cycleIndex FROM InvoiceEntity WHERE chargePlan.id = :chargePlanId
        """)
    Optional<Long> findMaxCycleIndex(@Param("chargePlanId") String chargePlanId);
}
