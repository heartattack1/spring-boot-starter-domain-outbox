package io.github.example.outbox.jdbc;

import io.github.example.outbox.core.OutboxMessage;
import io.github.example.outbox.core.OutboxStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class JpaOutboxRepositoryAdapter implements OutboxRepository {
  private final JpaOutboxMessageRepository repository;
  private final OutboxMessageHeaderMapper headerMapper;

  @Override
  @Transactional
  public void saveAll(List<OutboxMessage> messages) {
    List<OutboxMessageEntity> entities = messages.stream()
        .map(this::toEntity)
        .toList();
    repository.saveAll(entities);
  }

  @Override
  @Transactional
  public List<OutboxMessage> lockNextBatch(List<OutboxStatus> statuses, Instant now, int batchSize) {
    String[] statusValues = statuses.stream()
        .map(Enum::name)
        .toArray(String[]::new);
    return repository.lockNextBatch(statusValues, now, batchSize).stream()
        .map(this::toDomain)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public int countPending(List<OutboxStatus> statuses, Instant now) {
    String[] statusValues = statuses.stream()
        .map(Enum::name)
        .toArray(String[]::new);
    return repository.countPending(statusValues, now);
  }

  @Override
  @Transactional
  public void markPublished(UUID id, Instant publishedAt) {
    repository.markPublished(id, OutboxStatus.PUBLISHED.name(), publishedAt);
  }

  @Override
  @Transactional
  public void markFailed(UUID id, int attempts, Instant nextAttemptAt, String error) {
    repository.markFailed(id, OutboxStatus.FAILED.name(), attempts, nextAttemptAt, error);
  }

  @Override
  @Transactional
  public void markDead(UUID id, int attempts, String error) {
    repository.markDead(id, OutboxStatus.DEAD.name(), attempts, error);
  }

  private OutboxMessageEntity toEntity(OutboxMessage message) {
    OutboxMessageEntity entity = new OutboxMessageEntity();
    entity.setId(message.id());
    entity.setOccurredAt(message.occurredAt());
    entity.setAggregateType(message.aggregateType());
    entity.setAggregateId(message.aggregateId());
    entity.setEventType(message.eventType());
    entity.setEventVersion(message.eventVersion());
    entity.setPayload(message.payload());
    entity.setHeaders(headerMapper.toJson(message.headers()));
    entity.setStatus(message.status().name());
    entity.setAttempts(message.attempts());
    entity.setNextAttemptAt(message.nextAttemptAt());
    entity.setPublishedAt(message.publishedAt());
    entity.setError(message.error());
    return entity;
  }

  private OutboxMessage toDomain(OutboxMessageEntity entity) {
    return new OutboxMessage(
        entity.getId(),
        entity.getOccurredAt(),
        entity.getAggregateType(),
        entity.getAggregateId(),
        entity.getEventType(),
        entity.getEventVersion(),
        entity.getPayload(),
        headerMapper.fromJson(entity.getHeaders()),
        OutboxStatus.valueOf(entity.getStatus()),
        entity.getAttempts(),
        entity.getNextAttemptAt(),
        entity.getPublishedAt(),
        entity.getError()
    );
  }
}
