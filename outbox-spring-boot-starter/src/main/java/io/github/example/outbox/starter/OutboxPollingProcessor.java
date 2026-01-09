package io.github.example.outbox.starter;

import io.github.example.outbox.core.OutboxMessage;
import io.github.example.outbox.core.OutboxPublisher;
import io.github.example.outbox.core.OutboxStatus;
import io.github.example.outbox.core.PublishResult;
import io.github.example.outbox.jdbc.OutboxRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class OutboxPollingProcessor {
  private static final List<OutboxStatus> PENDING_STATUSES = List.of(OutboxStatus.NEW, OutboxStatus.FAILED);

  private final OutboxRepository outboxRepository;
  private final OutboxPublisher outboxPublisher;
  private final OutboxProperties properties;
  private final OutboxRetryCalculator retryCalculator;
  @Nullable
  private final OutboxMetrics metrics;

  @Scheduled(fixedDelayString = "${outbox.polling.fixed-delay:1s}")
  public void poll() {
    if (!properties.getPolling().isEnabled()) {
      return;
    }
    Instant now = Instant.now();
    recordBacklog(now);
    List<OutboxMessage> batch = outboxRepository.lockNextBatch(
        PENDING_STATUSES,
        now,
        properties.getPolling().getBatchSize()
    );
    for (OutboxMessage message : batch) {
      publishMessage(message, now);
    }
  }

  private void publishMessage(OutboxMessage message, Instant now) {
    Instant start = Instant.now();
    try {
      PublishResult result = outboxPublisher.publish(message);
      if (result.success()) {
        outboxRepository.markPublished(message.id(), Instant.now());
        recordPublishLatency(start);
      } else {
        handleFailure(message, result.message(), now);
      }
    } catch (Exception ex) {
      log.warn("Failed to publish outbox message {}", message.id(), ex);
      handleFailure(message, ex.getMessage(), now);
    }
  }

  private void handleFailure(OutboxMessage message, String error, Instant now) {
    int nextAttempts = message.attempts() + 1;
    if (nextAttempts >= properties.getRetry().getMaxAttempts()) {
      outboxRepository.markDead(message.id(), nextAttempts, error);
      recordDead();
      return;
    }
    recordRetry();
    Instant nextAttemptAt = retryCalculator.nextAttemptAt(now, nextAttempts);
    outboxRepository.markFailed(message.id(), nextAttempts, nextAttemptAt, error);
  }

  private void recordBacklog(Instant now) {
    if (metrics == null) {
      return;
    }
    int backlog = outboxRepository.countPending(PENDING_STATUSES, now);
    metrics.recordBacklog(backlog);
  }

  private void recordPublishLatency(Instant start) {
    if (metrics == null) {
      return;
    }
    metrics.recordPublish(Duration.between(start, Instant.now()));
  }

  private void recordRetry() {
    if (metrics == null) {
      return;
    }
    metrics.recordRetry();
  }

  private void recordDead() {
    if (metrics == null) {
      return;
    }
    metrics.recordDead();
  }
}
