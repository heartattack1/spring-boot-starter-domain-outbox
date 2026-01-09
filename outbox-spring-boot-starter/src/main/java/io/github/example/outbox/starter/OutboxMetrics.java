package io.github.example.outbox.starter;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxMetrics {
  private final MeterRegistry meterRegistry;
  private Counter publishedCounter;
  private Counter retryCounter;
  private Counter deadCounter;
  private Timer publishTimer;
  private AtomicInteger backlogGauge;

  @PostConstruct
  void init() {
    this.publishedCounter = meterRegistry.counter("outbox.published");
    this.retryCounter = meterRegistry.counter("outbox.retried");
    this.deadCounter = meterRegistry.counter("outbox.dead");
    this.publishTimer = meterRegistry.timer("outbox.publish.latency");
    this.backlogGauge = meterRegistry.gauge("outbox.backlog", new AtomicInteger(0));
  }

  public void recordBacklog(int backlog) {
    backlogGauge.set(backlog);
  }

  public void recordPublish(Duration duration) {
    publishedCounter.increment();
    publishTimer.record(duration);
  }

  public void recordRetry() {
    retryCounter.increment();
  }

  public void recordDead() {
    deadCounter.increment();
  }
}
