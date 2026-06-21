package com.caimanproject.debtor.infrastructure.database.mapper;

import com.caimanproject.contracts.util.Constants;
import com.caimanproject.mapper.OptionalMapper;
import java.util.Optional;
import java.util.UUID;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = Constants.MAPSTRUCT_COMPONENT_MODEL,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR,
        uses = {OptionalMapper.class, DebtorContactEntityMapper.class, AuditEntityMapper.class},
        unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface IdMapper {

    default String toString(final UUID id) {
        return Optional.ofNullable(id).map(UUID::toString).orElse(null);
    }

    default UUID toUUID(final String id) {
        return Optional.ofNullable(id).map(UUID::fromString).orElse(null);
    }

    default String toString(final Optional<UUID> id) {
        if (id == null) {
            return null;
        }

        return id.map(UUID::toString).orElse(null);
    }

    default UUID toUUID(final Optional<String> id) {
        if (id == null) {
            return null;
        }

        return id.map(UUID::fromString).orElse(null);
    }
}
