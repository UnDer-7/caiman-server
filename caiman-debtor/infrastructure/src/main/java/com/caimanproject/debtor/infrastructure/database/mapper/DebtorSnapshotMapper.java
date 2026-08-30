package com.caimanproject.debtor.infrastructure.database.mapper;

import com.caimanproject.contracts.gateway.debtor.DebtorContactSnapshotDto;
import com.caimanproject.contracts.gateway.debtor.DebtorSnapshotDto;
import com.caimanproject.contracts.util.Constants;
import com.caimanproject.debtor.core.domain.model.Debtor;
import com.caimanproject.debtor.core.domain.model.DebtorContact;
import com.caimanproject.mapper.OptionalMapper;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = Constants.MAPSTRUCT_COMPONENT_MODEL,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR,
        uses = {OptionalMapper.class},
        unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface DebtorSnapshotMapper {

    DebtorSnapshotDto toSnapshotDto(Debtor model);

    DebtorContactSnapshotDto toSnapshotDto(DebtorContact model);
}
