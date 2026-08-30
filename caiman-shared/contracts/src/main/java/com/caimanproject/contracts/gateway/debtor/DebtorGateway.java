package com.caimanproject.contracts.gateway.debtor;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface DebtorGateway {

    /**
     * @param ids debtor ids to check
     * @return subset of {@code ids} that do not correspond to any existing debtor; empty if all exist
     */
    Set<UUID> findMissingIds(Set<UUID> ids);

    /**
     * @param id debtor id to look up
     * @return a snapshot of the debtor's data, empty if no debtor exists with that id
     */
    Optional<DebtorSnapshotDto> findById(UUID id);
}
