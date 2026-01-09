package io.github.example.outbox.jdbc;

import io.github.example.outbox.core.OutboxMessage;
import io.github.example.outbox.core.OutboxStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OutboxRepository {
  void saveAll(List<OutboxMessage> messages);

  List<OutboxMessage> lockNextBatch(List<OutboxStatus> statuses, Instant now, int batchSize);

  int countPending(List<OutboxStatus> statuses, Instant now);

  void markPublished(UUID id, Instant publishedAt);

  void markFailed(UUID id, int attempts, Instant nextAttemptAt, String error);

  void markDead(UUID id, int attempts, String error);
}
