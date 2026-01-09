package io.github.example.outbox.core;

import java.util.List;

public interface HasDomainEvents {
  List<DomainEvent> pullEvents();
}
