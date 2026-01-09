package io.github.example.outbox.starter;

import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxRetryCalculator {
  private final OutboxProperties properties;

  public Instant nextAttemptAt(Instant now, int attempt) {
    Duration delay = calculateDelay(attempt);
    return now.plus(delay);
  }

  private Duration calculateDelay(int attempt) {
    Duration initial = properties.getRetry().getInitial();
    Duration max = properties.getRetry().getMax();
    if (properties.getRetry().getBackoff() == OutboxProperties.Backoff.FIXED) {
      return initial;
    }
    long exponent = Math.max(0, attempt - 1);
    long multiplier = 1L << Math.min(30, exponent);
    Duration computed = initial.multipliedBy(multiplier);
    if (computed.compareTo(max) > 0) {
      return max;
    }
    return computed;
  }
}
