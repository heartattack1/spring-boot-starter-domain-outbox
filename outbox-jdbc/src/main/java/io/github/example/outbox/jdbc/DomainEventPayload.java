package io.github.example.outbox.jdbc;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record DomainEventPayload(
    UUID id,
    Instant occurredAt,
    String aggregateType,
    String aggregateId,
    String eventType,
    int eventVersion,
    String payload,
    Map<String, String> headers
) {}
