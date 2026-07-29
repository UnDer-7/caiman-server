package com.caimanproject.billing.infrastructure.database.adapter;

import com.caimanproject.billing.core.domain.model.ChargePlan;
import com.caimanproject.billing.core.domain.types.ChargePlanStatus;
import com.caimanproject.billing.core.port.out.ChargePlanSearchGateway;
import com.caimanproject.billing.infrastructure.database.mapper.ChargePlanEntityMapper;
import com.caimanproject.billing.infrastructure.database.repository.ChargePlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChargePlanSearchAdapter implements ChargePlanSearchGateway {

    private final ChargePlanRepository chargePlanRepository;
    private final ChargePlanEntityMapper chargePlanEntityMapper;

    @Override
    @Transactional(readOnly = true)
    public List<ChargePlan> getAllActives() {
        final var found = chargePlanRepository.findByStatus(ChargePlanStatus.ACTIVE);

        return chargePlanEntityMapper.toModel(found);
    }

}
