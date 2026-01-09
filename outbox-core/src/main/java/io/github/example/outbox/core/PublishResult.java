package io.github.example.outbox.core;

public record PublishResult(boolean success, String message) {
  public static PublishResult success() {
    return new PublishResult(true, null);
  }

  public static PublishResult failure(String message) {
    return new PublishResult(false, message);
  }
}
