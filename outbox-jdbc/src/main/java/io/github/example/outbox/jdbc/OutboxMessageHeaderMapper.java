package io.github.example.outbox.jdbc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxMessageHeaderMapper {
  private static final TypeReference<Map<String, String>> HEADER_TYPE = new TypeReference<>() {};
  private final ObjectMapper objectMapper;

  public String toJson(Map<String, String> headers) {
    try {
      return objectMapper.writeValueAsString(headers);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Failed to serialize outbox headers", e);
    }
  }

  public Map<String, String> fromJson(String json) {
    try {
      return objectMapper.readValue(json, HEADER_TYPE);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Failed to deserialize outbox headers", e);
    }
  }
}
