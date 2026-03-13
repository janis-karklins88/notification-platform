package lv.janis.notification_platform.ingest.adapter.out.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import lv.janis.notification_platform.ingest.domain.Event;
import lv.janis.notification_platform.ingest.domain.EventStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class EventRepositoryAdapterTest {
  private final EventJpaRepository repository = mock(EventJpaRepository.class);
  private final EventRepositoryAdapter adapter = new EventRepositoryAdapter(repository);

  @Test
  void saveDelegatesToRepository() {
    Event event = mock(Event.class);
    when(repository.save(event)).thenReturn(event);

    Event result = adapter.save(event);

    assertSame(event, result);
    verify(repository).save(event);
  }

  @Test
  void findByIdDelegatesToRepository() {
    UUID id = UUID.randomUUID();
    Event event = mock(Event.class);
    when(repository.findById(id)).thenReturn(Optional.of(event));

    Optional<Event> result = adapter.findById(id);

    assertEquals(Optional.of(event), result);
    verify(repository).findById(id);
  }

  @Test
  void findByTenantIdAndIdempotencyKeyDelegatesToRepository() {
    UUID tenantId = UUID.randomUUID();
    String idempotencyKey = "idem";
    Event event = mock(Event.class);
    when(repository.findByTenant_IdAndIdempotencyKey(tenantId, idempotencyKey)).thenReturn(Optional.of(event));

    Optional<Event> result = adapter.findByTenantIdAndIdempotencyKey(tenantId, idempotencyKey);

    assertEquals(Optional.of(event), result);
    verify(repository).findByTenant_IdAndIdempotencyKey(tenantId, idempotencyKey);
  }

  @Test
  void findByIdAndTenantIdDelegatesToRepository() {
    UUID id = UUID.randomUUID();
    UUID tenantId = UUID.randomUUID();
    Event event = mock(Event.class);
    when(repository.findByIdAndTenant_Id(id, tenantId)).thenReturn(Optional.of(event));

    Optional<Event> result = adapter.findByIdAndTenantId(id, tenantId);

    assertEquals(Optional.of(event), result);
    verify(repository).findByIdAndTenant_Id(id, tenantId);
  }

  @Test
  void findTopNByStatusUsesLimitAndSort() {
    EventStatus status = EventStatus.RECEIVED;
    int limit = 5;
    List<Event> expected = List.of(mock(Event.class), mock(Event.class));
    when(repository.findByStatus(eq(status), org.mockito.ArgumentMatchers.any())).thenReturn(expected);

    List<Event> result = adapter.findTopNByStatus(status, limit);

    assertSame(expected, result);

    ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
    verify(repository).findByStatus(eq(status), pageableCaptor.capture());

    Pageable pageable = pageableCaptor.getValue();
    assertEquals(0, pageable.getPageNumber());
    assertEquals(limit, pageable.getPageSize());
    assertEquals(Sort.by("receivedAt"), pageable.getSort());
  }
}
