package com.caimanproject.debtor.infrastructure.database.mapper;

import com.caimanproject.contracts.util.Constants;
import com.caimanproject.debtor.core.domain.model.Debtor;
import com.caimanproject.debtor.infrastructure.database.entity.DebtorEntity;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = Constants.MAPSTRUCT_COMPONENT_MODEL,
    uses = {OptionalEntityMapper.class, DebtorContactEntityMapper.class, AuditEntityMapper.class},
    unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface DebtorEntityMapper {

    Debtor toModel(DebtorEntity entity);

    DebtorEntity toEntity(Debtor model);
}
