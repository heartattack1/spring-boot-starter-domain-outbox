package io.github.example.outbox.core;

import java.util.List;

public interface OutboxService {
  void enqueue(List<DomainEvent> events);
}
