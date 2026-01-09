package io.github.example.outbox.starter;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "outbox")
public class OutboxProperties {
  private boolean enabled = true;
  private final Polling polling = new Polling();
  private final Retry retry = new Retry();
  private final Publisher publisher = new Publisher();

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public Polling getPolling() {
    return polling;
  }

  public Retry getRetry() {
    return retry;
  }

  public Publisher getPublisher() {
    return publisher;
  }

  public static class Polling {
    private boolean enabled = true;
    private Duration fixedDelay = Duration.ofSeconds(1);
    private int batchSize = 100;
    private Duration lockTimeout = Duration.ofSeconds(5);

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public Duration getFixedDelay() {
      return fixedDelay;
    }

    public void setFixedDelay(Duration fixedDelay) {
      this.fixedDelay = fixedDelay;
    }

    public int getBatchSize() {
      return batchSize;
    }

    public void setBatchSize(int batchSize) {
      this.batchSize = batchSize;
    }

    public Duration getLockTimeout() {
      return lockTimeout;
    }

    public void setLockTimeout(Duration lockTimeout) {
      this.lockTimeout = lockTimeout;
    }
  }

  public static class Retry {
    private int maxAttempts = 10;
    private Backoff backoff = Backoff.EXPONENTIAL;
    private Duration initial = Duration.ofSeconds(1);
    private Duration max = Duration.ofMinutes(1);

    public int getMaxAttempts() {
      return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
      this.maxAttempts = maxAttempts;
    }

    public Backoff getBackoff() {
      return backoff;
    }

    public void setBackoff(Backoff backoff) {
      this.backoff = backoff;
    }

    public Duration getInitial() {
      return initial;
    }

    public void setInitial(Duration initial) {
      this.initial = initial;
    }

    public Duration getMax() {
      return max;
    }

    public void setMax(Duration max) {
      this.max = max;
    }
  }

  public static class Publisher {
    private PublisherType type = PublisherType.NONE;

    public PublisherType getType() {
      return type;
    }

    public void setType(PublisherType type) {
      this.type = type;
    }
  }

  public enum Backoff {
    FIXED,
    EXPONENTIAL
  }

  public enum PublisherType {
    NONE,
    KAFKA,
    AMQP,
    HTTP
  }
}
