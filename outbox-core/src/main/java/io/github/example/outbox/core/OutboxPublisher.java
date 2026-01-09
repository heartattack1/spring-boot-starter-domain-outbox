package io.github.example.outbox.core;

public interface OutboxPublisher {
  PublishResult publish(OutboxMessage message) throws Exception;
}
