package com.caimanproject.billing.infrastructure.database.repository;

import com.caimanproject.billing.infrastructure.database.entity.ChargePlanEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChargePlanRepository extends CrudRepository<ChargePlanEntity, String> {}
