package com.caimanproject.debtor.infrastructure.database.repository;

import com.caimanproject.debtor.infrastructure.database.entity.DebtorEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DebtorRepository extends CrudRepository<DebtorEntity, String> {}
