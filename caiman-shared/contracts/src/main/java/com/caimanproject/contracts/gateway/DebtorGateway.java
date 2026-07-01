package com.caimanproject.contracts.gateway;

import java.util.Set;
import java.util.UUID;

public interface DebtorGateway {

    /**
     * @param ids debtor ids to check
     * @return subset of {@code ids} that do not correspond to any existing debtor; empty if all exist
     */
    Set<UUID> findMissingIds(Set<UUID> ids);
}
