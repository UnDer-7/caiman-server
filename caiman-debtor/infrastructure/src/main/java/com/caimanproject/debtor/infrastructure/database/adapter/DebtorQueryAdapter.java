package com.caimanproject.debtor.infrastructure.database.adapter;

import com.caimanproject.contracts.gateway.DebtorGateway;
import com.caimanproject.debtor.infrastructure.database.repository.DebtorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class DebtorQueryAdapter implements DebtorGateway {

    private final DebtorRepository debtorRepository;

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<UUID> findMissingIds(final Set<UUID> ids) {
        if (ids.isEmpty()) {
            return Collections.emptySet();
        }

        final var foundIds = debtorRepository.findAllByIdIn(ids.stream().map(UUID::toString).collect(Collectors.toSet()))
            .stream()
            .map(UUID::fromString)
            .collect(Collectors.toSet());

        if (foundIds.size() == ids.size()) {
            return Collections.emptySet();
        }

        return ids.stream()
            .filter(Predicate.not(foundIds::contains))
            .collect(Collectors.toSet());
    }

}
