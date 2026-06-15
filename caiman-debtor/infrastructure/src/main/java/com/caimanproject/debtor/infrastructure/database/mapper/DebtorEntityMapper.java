package com.caimanproject.debtor.infrastructure.database.mapper;

import com.caimanproject.contracts.util.Constants;
import com.caimanproject.debtor.core.domain.model.Debtor;
import com.caimanproject.debtor.infrastructure.database.entity.DebtorEntity;
import com.caimanproject.mapper.OptionalMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = Constants.MAPSTRUCT_COMPONENT_MODEL,
        uses = {OptionalMapper.class, DebtorContactEntityMapper.class, AuditEntityMapper.class, IdMapper.class},
        unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface DebtorEntityMapper {

    Debtor toModel(DebtorEntity entity);

    @Mapping(target = "contacts", ignore = true)
    DebtorEntity toEntity(Debtor model);
}
