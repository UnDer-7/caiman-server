package com.caimanproject.billing.core.domain.service.odin;

import com.caimanproject.billing.core.domain.types.ChargePlanType;
import com.caimanproject.billing.core.port.out.ChargePlanSearchGateway;
import com.caimanproject.billing.core.test.builder.ChargePlanDomainBuilder;
import com.caimanproject.test.annotation.UnitTest;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class RunOdinServiceTest {

    @Mock
    ChargePlanSearchGateway chargePlanSearchGateway;

    @Mock
    RotatingInvoiceGenerator rotatingInvoiceGenerator;

    @Mock
    SplitInvoiceGenerator splitInvoiceGenerator;

    @InjectMocks
    RunOdinService service;

    @Test
    void should_dispatch_to_rotating_generator_when_type_is_rotating_and_due_today() {
        // Given
        final var chargePlan = ChargePlanDomainBuilder.buildRotatingChargePlanDueTodayFull().build();
        Mockito.when(chargePlanSearchGateway.getAllActives()).thenReturn(List.of(chargePlan));

        // When
        service.execute();

        // Then
        Mockito.verify(rotatingInvoiceGenerator).generate(chargePlan, LocalDate.now(ZoneOffset.UTC));
        Mockito.verifyNoInteractions(splitInvoiceGenerator);
    }

    @Test
    void should_dispatch_to_split_generator_when_type_is_split_and_due_today() {
        // Given
        final var chargePlan = ChargePlanDomainBuilder.buildRotatingChargePlanDueTodayFull()
                .type(ChargePlanType.SPLIT)
                .build();
        Mockito.when(chargePlanSearchGateway.getAllActives()).thenReturn(List.of(chargePlan));

        // When
        service.execute();

        // Then
        Mockito.verify(splitInvoiceGenerator).generate(chargePlan, LocalDate.now(ZoneOffset.UTC));
        Mockito.verifyNoInteractions(rotatingInvoiceGenerator);
    }

    @Test
    void should_not_dispatch_when_plan_is_not_due_today() {
        // Given
        final var chargePlan = ChargePlanDomainBuilder.buildRotatingChargePlanDueTodayFull()
                .cycleAnchorDate(LocalDate.now(ZoneOffset.UTC).plusDays(1))
                .build();
        Mockito.when(chargePlanSearchGateway.getAllActives()).thenReturn(List.of(chargePlan));

        // When
        service.execute();

        // Then
        Mockito.verifyNoInteractions(rotatingInvoiceGenerator, splitInvoiceGenerator);
    }
}
