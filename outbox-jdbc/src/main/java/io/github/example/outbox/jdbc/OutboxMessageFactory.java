package io.github.example.outbox.jdbc;

import io.github.example.outbox.core.DomainEvent;
import io.github.example.outbox.core.OutboxMessage;
import io.github.example.outbox.core.OutboxStatus;
import java.time.Instant;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxMessageFactory {
  private final DomainEventPayloadMapper payloadMapper;

  public OutboxMessage fromDomainEvent(DomainEvent event) {
    String payload = payloadMapper.toJson(event);
    Map<String, String> headers = event.headers() == null ? Map.of() : event.headers();
    Instant occurredAt = event.occurredAt();
    return new OutboxMessage(
        event.eventId(),
        occurredAt,
        event.aggregateType(),
        event.aggregateId(),
        event.type(),
        event.version(),
        payload,
        headers,
        OutboxStatus.NEW,
        0,
        occurredAt,
        null,
        null
    );
  }
}
