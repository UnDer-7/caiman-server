package com.caimanproject.debtor.infrastructure.database.adapter;

import com.caimanproject.contracts.gateway.debtor.DebtorGateway;
import com.caimanproject.debtor.infrastructure.database.mapper.DebtorEntityMapper;
import com.caimanproject.debtor.infrastructure.database.repository.DebtorRepository;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.caimanproject.mapper.IdMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DebtorQueryAdapter implements DebtorGateway {

    private final DebtorRepository debtorRepository;
    private final DebtorEntityMapper debtorEntityMapper;
    private final IdMapper idMapper;

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public Set<UUID> findMissingIds(final Set<UUID> ids) {
        if (ids.isEmpty()) {
            return Collections.emptySet();
        }

        final var idsStr = idMapper.toString(ids);
        final var foundIds =
                debtorRepository.findIdsByIdIn(idsStr).stream()
                        .map(UUID::fromString)
                        .collect(Collectors.toSet());

        if (foundIds.size() == ids.size()) {
            return Collections.emptySet();
        }

        return ids.stream().filter(Predicate.not(foundIds::contains)).collect(Collectors.toSet());
    }

}
