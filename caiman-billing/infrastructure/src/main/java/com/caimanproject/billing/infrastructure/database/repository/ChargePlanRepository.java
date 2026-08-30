package com.caimanproject.billing.infrastructure.database.repository;

import com.caimanproject.billing.core.domain.types.ChargePlanStatus;
import com.caimanproject.billing.infrastructure.database.entity.ChargePlanEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChargePlanRepository extends JpaRepository<ChargePlanEntity, String> {

    List<ChargePlanEntity> findByStatus(ChargePlanStatus status);
}
