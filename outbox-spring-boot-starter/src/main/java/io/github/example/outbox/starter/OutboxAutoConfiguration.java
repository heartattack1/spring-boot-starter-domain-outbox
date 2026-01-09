package io.github.example.outbox.starter;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@AutoConfiguration
@EnableConfigurationProperties(OutboxProperties.class)
public class OutboxAutoConfiguration {
}
