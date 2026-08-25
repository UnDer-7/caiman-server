package com.caimanproject.billing.infrastructure.database.repository;

import com.caimanproject.billing.core.domain.types.ChargePlanStatus;
import com.caimanproject.billing.infrastructure.database.entity.ChargePlanEntity;
import java.util.List;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChargePlanRepository extends CrudRepository<ChargePlanEntity, String> {

    List<ChargePlanEntity> findByStatus(ChargePlanStatus status);
}
