package com.caimanproject.debtor.infrastructure.database.mapper;

import com.caimanproject.contracts.util.Constants;
import com.caimanproject.debtor.core.domain.model.Audit;
import com.caimanproject.debtor.infrastructure.database.entity.AuditEmbeddable;
import com.caimanproject.mapper.OptionalMapper;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = Constants.MAPSTRUCT_COMPONENT_MODEL,
    uses = {OptionalMapper.class},
    unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface AuditEntityMapper {

    AuditEmbeddable toEntity(Audit model);

    Audit toDto(AuditEmbeddable model);
}
