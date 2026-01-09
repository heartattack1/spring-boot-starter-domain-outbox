package io.github.example.outbox.starter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.example.outbox.core.OutboxPublisher;
import io.github.example.outbox.core.OutboxService;
import io.github.example.outbox.jdbc.DomainEventPayloadMapper;
import io.github.example.outbox.jdbc.JdbcOutboxRepository;
import io.github.example.outbox.jdbc.JdbcOutboxService;
import io.github.example.outbox.jdbc.OutboxMessageFactory;
import io.github.example.outbox.jdbc.OutboxMessageHeaderMapper;
import io.github.example.outbox.jdbc.OutboxRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

@AutoConfiguration
@EnableConfigurationProperties(OutboxProperties.class)
@ConditionalOnProperty(prefix = "outbox", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OutboxAutoConfiguration {
  @Bean
  @ConditionalOnClass(ObjectMapper.class)
  @ConditionalOnMissingBean
  public DomainEventPayloadMapper domainEventPayloadMapper(ObjectMapper objectMapper) {
    return new DomainEventPayloadMapper(objectMapper);
  }

  @Bean
  @ConditionalOnClass(ObjectMapper.class)
  @ConditionalOnMissingBean
  public OutboxMessageHeaderMapper outboxMessageHeaderMapper(ObjectMapper objectMapper) {
    return new OutboxMessageHeaderMapper(objectMapper);
  }

  @Bean
  @ConditionalOnMissingBean
  public OutboxMessageFactory outboxMessageFactory(DomainEventPayloadMapper payloadMapper) {
    return new OutboxMessageFactory(payloadMapper);
  }

  @Bean
  @ConditionalOnClass(JdbcTemplate.class)
  @ConditionalOnMissingBean
  public OutboxRepository outboxRepository(
      JdbcTemplate jdbcTemplate,
      OutboxMessageHeaderMapper headerMapper
  ) {
    return new JdbcOutboxRepository(jdbcTemplate, headerMapper);
  }

  @Bean
  @ConditionalOnMissingBean
  public OutboxService outboxService(OutboxRepository outboxRepository, OutboxMessageFactory factory) {
    return new JdbcOutboxService(outboxRepository, factory);
  }

  @Bean
  @ConditionalOnMissingBean
  public OutboxPublisher outboxPublisher() {
    return new NoOpOutboxPublisher();
  }

  @Bean
  @ConditionalOnMissingBean
  public OutboxRetryCalculator outboxRetryCalculator(OutboxProperties properties) {
    return new OutboxRetryCalculator(properties);
  }

  @Bean
  @ConditionalOnClass(MeterRegistry.class)
  @ConditionalOnMissingBean
  public OutboxMetrics outboxMetrics(MeterRegistry meterRegistry) {
    return new OutboxMetrics(meterRegistry);
  }

  @EnableScheduling
  @org.springframework.context.annotation.Configuration(proxyBeanMethods = false)
  @ConditionalOnProperty(prefix = "outbox.polling", name = "enabled", havingValue = "true", matchIfMissing = true)
  static class PollingConfiguration {
    @Bean
    public OutboxPollingProcessor outboxPollingProcessor(
        OutboxRepository outboxRepository,
        OutboxPublisher outboxPublisher,
        OutboxProperties properties,
        OutboxRetryCalculator retryCalculator,
        org.springframework.beans.factory.ObjectProvider<OutboxMetrics> metricsProvider
    ) {
      return new OutboxPollingProcessor(
          outboxRepository,
          outboxPublisher,
          properties,
          retryCalculator,
          metricsProvider.getIfAvailable()
      );
    }
  }
}
