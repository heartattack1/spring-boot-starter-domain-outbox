package io.github.example.outbox.jdbc;

import io.github.example.outbox.core.DomainEvent;
import io.github.example.outbox.core.OutboxMessage;
import io.github.example.outbox.core.OutboxService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JdbcOutboxService implements OutboxService {
  private final OutboxRepository outboxRepository;
  private final OutboxMessageFactory messageFactory;

  @Override
  public void enqueue(List<DomainEvent> events) {
    if (events == null || events.isEmpty()) {
      return;
    }
    List<OutboxMessage> messages = events.stream()
        .map(messageFactory::fromDomainEvent)
        .toList();
    outboxRepository.saveAll(messages);
  }
}
