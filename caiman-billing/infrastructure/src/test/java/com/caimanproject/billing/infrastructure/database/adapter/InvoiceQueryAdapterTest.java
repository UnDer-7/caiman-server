package com.caimanproject.billing.infrastructure.database.adapter;

import com.caimanproject.billing.core.domain.types.InvoiceStatus;
import com.caimanproject.billing.infrastructure.database.entity.ChargePlanEntity;
import com.caimanproject.billing.infrastructure.database.entity.ChargePlanMemberEntity;
import com.caimanproject.billing.infrastructure.database.entity.InvoiceEntity;
import com.caimanproject.billing.infrastructure.database.mapper.InvoiceSnapshotMapper;
import com.caimanproject.billing.infrastructure.database.mapper.InvoiceSnapshotMapperImpl;
import com.caimanproject.billing.infrastructure.database.repository.InvoiceRepository;
import com.caimanproject.contracts.gateway.debtor.DebtorGateway;
import com.caimanproject.contracts.gateway.debtor.DebtorSnapshotDto;
import com.caimanproject.mapper.IdMapperImpl;
import com.caimanproject.test.annotation.UnitTest;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@UnitTest
@ExtendWith(MockitoExtension.class)
class InvoiceQueryAdapterTest {

    @Mock
    InvoiceRepository invoiceRepository;

    @Mock
    DebtorGateway debtorGateway;

    @Spy
    InvoiceSnapshotMapper invoiceSnapshotMapper = new InvoiceSnapshotMapperImpl(new IdMapperImpl());

    @InjectMocks
    InvoiceQueryAdapter adapter;

    @Test
    void should_return_snapshot_with_debtor_name_when_token_matches() {
        final var debtorId = UUID.randomUUID();
        final var token = UUID.randomUUID();
        final var chargePlan = ChargePlanEntity.builder()
                .id(UUID.randomUUID().toString())
                .name("Shared Plan")
                .build();
        final var chargePlanMember = ChargePlanMemberEntity.builder()
                .id(UUID.randomUUID().toString())
                .debtorId(debtorId.toString())
                .build();
        final var invoiceId = UUID.randomUUID();
        final var dueDate = Instant.now();
        final var entity = InvoiceEntity.builder()
                .id(invoiceId.toString())
                .chargePlan(chargePlan)
                .chargePlanMember(chargePlanMember)
                .cycleIndex(3L)
                .amountDue(new BigDecimal("100.00"))
                .amountPaid(new BigDecimal("40.00"))
                .status(InvoiceStatus.PENDING)
                .dueDate(dueDate)
                .uploadToken(token.toString())
                .build();

        when(invoiceRepository.findByUploadToken(token.toString())).thenReturn(Optional.of(entity));
        when(debtorGateway.findById(debtorId))
                .thenReturn(Optional.of(DebtorSnapshotDto.builder()
                        .id(debtorId)
                        .name("John Doe")
                        .build()));

        final var result = adapter.findByUploadToken(token);

        assertThat(result).isPresent();
        final var snapshot = result.orElseThrow();
        assertThat(snapshot.id()).isEqualTo(invoiceId);
        assertThat(snapshot.chargePlanName()).isEqualTo("Shared Plan");
        assertThat(snapshot.debtorName()).isEqualTo("John Doe");
        assertThat(snapshot.amountDue()).isEqualByComparingTo("100.00");
        assertThat(snapshot.amountPaid()).isEqualByComparingTo("40.00");
        assertThat(snapshot.status()).isEqualTo("PENDING");
        assertThat(snapshot.dueDate()).isEqualTo(dueDate);
        assertThat(snapshot.cycleIndex()).isEqualTo(3);

        verify(debtorGateway).findById(debtorId);
    }

    @Test
    void should_return_empty_when_token_does_not_match_any_invoice() {
        final var token = UUID.randomUUID();
        when(invoiceRepository.findByUploadToken(token.toString())).thenReturn(Optional.empty());

        final var result = adapter.findByUploadToken(token);

        assertThat(result).isEmpty();
    }
}
