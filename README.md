# spring-boot-starter-domain-outbox

Transactional Domain Events with Outbox Pattern for Spring Boot  
(JDBC/JPA, retries, multi-instance safe, observable)

---

## Problem

In distributed systems, publishing domain events directly to a message broker inside a business transaction leads to fundamental reliability issues:

- Events can be published while the database transaction is rolled back
- Database state and external messages become inconsistent
- Retry logic is ad-hoc and error-prone
- Multi-instance deployments introduce duplicate publishing risks

Spring application events (`@EventListener`) do **not** solve these problems and are not suitable for reliable inter-service communication.

---

## Solution

This starter implements the **Outbox Pattern** for Spring Boot applications:

- Domain events are persisted to an **outbox table** within the same transaction as the aggregate state
- A background processor publishes events to external systems (Kafka, AMQP, HTTP, etc.)
- Delivery guarantees are **at-least-once**
- The implementation is safe for **multi-instance deployments**
- Built with observability, retries, and failure handling in mind

The library is designed to be:
- DDD-friendly
- Infrastructure-agnostic at the core level
- Production-ready by default

---

## Architecture

### High-level flow

1. A use case modifies an aggregate
2. The aggregate records one or more domain events
3. Events are persisted to the `outbox_message` table in the same transaction
4. A poller selects pending messages using database-level locking
5. Messages are published via a configured publisher
6. Message state is updated (`PUBLISHED`, `FAILED`, `DEAD`)

### Modules

```
spring-boot-starter-domain-outbox
- outbox-core                 // domain abstractions (no Spring)
- outbox-jdbc                 // JDBC/JPA outbox storage + Liquibase
- outbox-spring-boot-starter  // auto-configuration
- example-app                 // reference implementation
```

### Guarantees by design

| Concern       | Approach                                 |
|---------------|-------------------------------------------|
| Consistency   | Single DB transaction                     |
| Delivery      | At-least-once                             |
| Concurrency   | SELECT … FOR UPDATE SKIP LOCKED           |
| Idempotency   | Stable event_id                           |
| Retry         | Configurable backoff and max attempts     |
| Failure       | DEAD-letter state                         |

---

## Quickstart

### 1. Add dependency

```kotlin
dependencies {
  implementation("io.github.<your-gh>:spring-boot-starter-domain-outbox:<version>")
}
```

### 2. Enable Liquibase migrations

```yaml
spring:
  liquibase:
    enabled: true
```

### 3. Publish domain events

Inside your aggregate:

```java
public class Order extends AggregateRoot {

  public static Order create(...) {
    Order order = new Order(...);
    order.addEvent(new OrderCreatedEvent(...));
    return order;
  }
}
```

Inside your use case or repository:

```java
orderRepository.save(order);
outboxService.enqueue(order.pullEvents());
```

---

## Configuration

```yaml
outbox:
  enabled: true

  polling:
    enabled: true
    fixed-delay: 1s
    batch-size: 100
    lock-timeout: 5s

  retry:
    max-attempts: 10
    backoff: exponential
    initial: 1s
    max: 1m

  publisher:
    type: none   # none | kafka | amqp | http
```

---

## Guarantees and Semantics

### Delivery
- At-least-once
- Duplicate delivery is possible
- Consumers must be idempotent

### Ordering
- Guaranteed per aggregate, not globally

### Transactions
- Domain state and outbox message are persisted atomically
- Publishing happens outside the transaction

### Failure handling
- Failed publishes are retried with backoff
- After max attempts, message is marked as DEAD

---

## Observability

The starter exposes:
- Backlog size
- Publish latency
- Retry counts
- DEAD message count

Integrations:
- Micrometer
- Spring Boot Actuator

---

## FAQ

### Why not Spring Events?
They are in-memory, non-transactional, and not suitable for inter-service communication.

### Is this exactly-once delivery?
No. Exactly-once delivery is not achievable in general distributed systems.
This library provides at-least-once delivery with explicit idempotency.

---

## Roadmap

- Kafka publisher
- AMQP publisher
- HTTP publisher
- Admin endpoints for DEAD messages
- Partitioned outbox support
- Kotlin-first API

---

## License

Apache License 2.0
