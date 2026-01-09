package io.github.example.outbox.starter;

import io.github.example.outbox.core.OutboxMessage;
import io.github.example.outbox.core.OutboxPublisher;
import io.github.example.outbox.core.PublishResult;
import org.springframework.stereotype.Component;

@Component
public class NoOpOutboxPublisher implements OutboxPublisher {
  @Override
  public PublishResult publish(OutboxMessage message) {
    return PublishResult.success();
  }
}
