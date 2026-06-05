package com.caimanproject.debtor.infrastructure.database.mapper;

import com.caimanproject.contracts.util.Constants;
import com.caimanproject.debtor.core.domain.model.DebtorContact;
import com.caimanproject.debtor.infrastructure.database.entity.DebtorContactEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = Constants.MAPSTRUCT_COMPONENT_MODEL,
    uses = {OptionalEntityMapper.class, AuditEntityMapper.class},
    unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface DebtorContactEntityMapper {

    DebtorContact toModel(DebtorContactEntity entity);

    @Mapping(target = "debtor", ignore = true)
    DebtorContactEntity toEntity(DebtorContact model);
}
