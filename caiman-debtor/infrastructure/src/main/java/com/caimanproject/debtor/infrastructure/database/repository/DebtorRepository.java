package com.caimanproject.debtor.infrastructure.database.repository;

import com.caimanproject.debtor.infrastructure.database.entity.DebtorEntity;
import java.util.Set;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DebtorRepository extends CrudRepository<DebtorEntity, String> {

    @Query("""
        SELECT id FROM DebtorEntity WHERE id in :ids
        """)
    Set<String> findIdsByIdIn(@Param("ids") Set<String> ids);

    Set<String> id(String id);

}
