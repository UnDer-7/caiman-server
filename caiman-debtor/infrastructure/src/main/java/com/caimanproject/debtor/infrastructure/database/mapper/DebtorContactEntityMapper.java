package com.caimanproject.debtor.infrastructure.database.mapper;

import com.caimanproject.contracts.util.Constants;
import com.caimanproject.debtor.core.domain.model.DebtorContact;
import com.caimanproject.debtor.infrastructure.database.entity.DebtorContactEntity;
import com.caimanproject.mapper.IdMapper;
import com.caimanproject.mapper.OptionalMapper;
import java.util.Collection;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = Constants.MAPSTRUCT_COMPONENT_MODEL,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR,
        uses = {OptionalMapper.class, IdMapper.class, DebtorAuditEntityMapper.class},
        unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface DebtorContactEntityMapper {

    DebtorContact toModel(DebtorContactEntity entity);

    @Mapping(target = "debtor", ignore = true)
    DebtorContactEntity toEntity(DebtorContact model);

    Collection<DebtorContactEntity> toEntity(Collection<DebtorContact> models);
}
