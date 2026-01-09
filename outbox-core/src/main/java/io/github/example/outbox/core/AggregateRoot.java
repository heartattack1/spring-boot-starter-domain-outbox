package io.github.example.outbox.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class AggregateRoot {
  private final List<DomainEvent> domainEvents = new ArrayList<>();

  protected void addEvent(DomainEvent event) {
    domainEvents.add(event);
  }

  public List<DomainEvent> pullEvents() {
    if (domainEvents.isEmpty()) {
      return List.of();
    }
    List<DomainEvent> events = new ArrayList<>(domainEvents);
    domainEvents.clear();
    return Collections.unmodifiableList(events);
  }
}
