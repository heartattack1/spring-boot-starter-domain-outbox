package io.github.example.outbox.core;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public interface DomainEvent {
  UUID eventId();

  Instant occurredAt();

  String type();

  String aggregateType();

  String aggregateId();

  int version();

  Map<String, String> headers();
}
