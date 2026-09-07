package com.caimanproject.billing.infrastructure.database.repository;

import com.caimanproject.billing.infrastructure.database.entity.InvoiceEntity;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface InvoiceRepository extends CrudRepository<InvoiceEntity, String> {

    @Query("""
        SELECT MAX(i.cycleIndex) FROM InvoiceEntity i WHERE i.chargePlan.id = :chargePlanId
        """)
    Optional<Long> findMaxCycleIndex(@Param("chargePlanId") String chargePlanId);

    @Query("""
        SELECT COUNT(i) > 0 FROM InvoiceEntity i
        WHERE i.chargePlan.id = :chargePlanId
          AND i.generationDate = :generationDate
        """)
    boolean existsByChargePlanIdAndGenerationDate(
            @Param("chargePlanId") String chargePlanId, @Param("generationDate") LocalDate generationDate);

    @Query("""
        SELECT COUNT(i) > 0 FROM InvoiceEntity i WHERE i.chargePlan.id = :chargePlanId
        """)
    boolean existsByChargePlanId(@Param("chargePlanId") String chargePlanId);

    Optional<InvoiceEntity> findByUploadToken(String uploadToken);
}
