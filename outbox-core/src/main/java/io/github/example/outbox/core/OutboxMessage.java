package io.github.example.outbox.core;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record OutboxMessage(
    UUID id,
    Instant occurredAt,
    String aggregateType,
    String aggregateId,
    String eventType,
    int eventVersion,
    String payload,
    Map<String, String> headers,
    OutboxStatus status,
    int attempts,
    Instant nextAttemptAt,
    Instant publishedAt,
    String error
) {}
