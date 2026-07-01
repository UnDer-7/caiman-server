package com.caimanproject.debtor.infrastructure.database.repository;

import com.caimanproject.debtor.infrastructure.database.entity.DebtorEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Repository
public interface DebtorRepository extends CrudRepository<DebtorEntity, String> {

    @Query("""
        SELECT id FROM DebtorEntity WHERE id in :ids
        """)
    Set<String> findAllByIdIn(@Param("ids") Set<String> ids);

}
