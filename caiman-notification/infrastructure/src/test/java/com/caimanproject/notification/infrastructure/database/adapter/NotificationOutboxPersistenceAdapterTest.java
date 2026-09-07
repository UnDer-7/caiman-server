package com.caimanproject.notification.infrastructure.database.adapter;

import com.caimanproject.mapper.IdMapper;
import com.caimanproject.mapper.IdMapperImpl;
import com.caimanproject.mapper.OptionalMapper;
import com.caimanproject.mapper.OptionalMapperImpl;
import com.caimanproject.notification.core.test.builder.NotificationOutboxDomainBuilder;
import com.caimanproject.notification.infrastructure.database.entity.NotificationOutboxEntity;
import com.caimanproject.notification.infrastructure.database.mapper.NotificationAuditEntityMapper;
import com.caimanproject.notification.infrastructure.database.mapper.NotificationAuditEntityMapperImpl;
import com.caimanproject.notification.infrastructure.database.mapper.NotificationOutboxEntityMapper;
import com.caimanproject.notification.infrastructure.database.mapper.NotificationOutboxEntityMapperImpl;
import com.caimanproject.notification.infrastructure.database.repository.NotificationOutboxRepository;
import com.caimanproject.test.annotation.UnitTest;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@UnitTest
@ExtendWith(MockitoExtension.class)
class NotificationOutboxPersistenceAdapterTest {

    @Mock
    NotificationOutboxRepository notificationOutboxRepository;

    @Spy
    IdMapper idMapper = new IdMapperImpl();

    @Spy
    OptionalMapper optionalMapper = new OptionalMapperImpl();

    @Spy
    NotificationAuditEntityMapper notificationAuditEntityMapper = new NotificationAuditEntityMapperImpl(optionalMapper);

    @Spy
    NotificationOutboxEntityMapper notificationOutboxEntityMapper =
            new NotificationOutboxEntityMapperImpl(optionalMapper, idMapper, notificationAuditEntityMapper);

    @InjectMocks
    NotificationOutboxPersistenceAdapter adapter;

    @Test
    void findEligibleForDispatch_queries_repository_with_a_page_request_bounded_by_limit() {
        // Given
        final var entity = buildOutboxEntity();
        Mockito.when(notificationOutboxRepository.findEligibleForDispatch(
                        Mockito.any(Instant.class), Mockito.any(Pageable.class)))
                .thenReturn(List.of(entity));
        final var now = Instant.now();

        // When
        final var result = adapter.findEligibleForDispatch(now, 50);

        // Then
        final var pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        Mockito.verify(notificationOutboxRepository).findEligibleForDispatch(Mockito.eq(now), pageableCaptor.capture());
        Assertions.assertThat(pageableCaptor.getValue()).isEqualTo(PageRequest.of(0, 50));
        Assertions.assertThat(result).hasSize(1);
        Assertions.assertThat(result.getFirst().getId()).contains(UUID.fromString(entity.getId()));
    }

    @Test
    void findStuckProcessing_queries_repository_by_threshold_and_maps_to_domain() {
        // Given
        final var entity = buildOutboxEntity();
        final var threshold = Instant.now();
        Mockito.when(notificationOutboxRepository.findStuckProcessing(threshold))
                .thenReturn(List.of(entity));

        // When
        final var result = adapter.findStuckProcessing(threshold);

        // Then
        Assertions.assertThat(result).hasSize(1);
        Assertions.assertThat(result.getFirst().getId()).contains(UUID.fromString(entity.getId()));
    }

    @Test
    void delete_removes_by_id() {
        // Given
        final var id = UUID.randomUUID();

        // When
        adapter.delete(id);

        // Then
        Mockito.verify(notificationOutboxRepository).deleteById(id.toString());
    }

    private NotificationOutboxEntity buildOutboxEntity() {
        final var outbox =
                NotificationOutboxDomainBuilder.buildNotificationOutboxFull().build();
        return notificationOutboxEntityMapper.toEntity(outbox);
    }
}
