package com.caimanproject.mapper;

import com.caimanproject.contracts.util.Constants;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = Constants.MAPSTRUCT_COMPONENT_MODEL,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR,
        unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface IdMapper {

    default Set<String> toString(final Set<UUID> ids) {
        if (ids == null) {
            return null;
        }

        return ids.stream().map(this::toString).collect(Collectors.toSet());
    }

    default List<String> toString(final List<UUID> ids) {
        if (ids == null) {
            return null;
        }

        return ids.stream().map(this::toString).toList();
    }

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
