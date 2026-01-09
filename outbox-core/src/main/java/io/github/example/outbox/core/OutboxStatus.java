package io.github.example.outbox.core;

public enum OutboxStatus {
  NEW,
  PUBLISHED,
  FAILED,
  DEAD
}
