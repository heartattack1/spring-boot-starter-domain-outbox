# spring-boot-starter-domain-outbox

Transactional Domain Events with Outbox Pattern for Spring Boot (JDBC/JPA, retries, metrics, multi-instance safe).

## Ценность starter’а (что именно решает)

- **Транзакционная надёжность**: событие фиксируется в БД в рамках той же транзакции, что и изменения домена.
- **Гарантия доставки (at-least-once)**: публикация во внешнюю шину (Kafka/Rabbit/HTTP) идёт из outbox таблицы с ретраями.
- **Идемпотентность**: у события есть стабильный `event_id`, consumer’ы могут дедуплицировать.
- **Наблюдаемость**: метрики/трейсы по latency, retry, DLQ.

## Предлагаемая архитектура модулей

- **starter** — автоконфигурация Spring Boot.
- **core** — доменные интерфейсы и outbox pipeline (без Spring).
- **jdbc** — реализация хранения outbox на JDBC/JPA.
- **publisher-kafka** (опционально) — паблишер в Kafka.
- **publisher-amqp** (опционально) — Rabbit.
- **example-app** — демонстрационное приложение (Spring Boot).

Если нужно быстро стартовать, можно начать с 2 модулей: `starter` + `example-app`, а остальные развить позже.

## Public API (минимальный, но “enterprise-grade”)

### 1) Доменные события

```java
public interface DomainEvent {
  UUID eventId();          // стабильный id
  Instant occurredAt();
  String type();           // например "order.created"
  String aggregateType();  // "Order"
  String aggregateId();    // "123"
  int version();           // schema version события
  Map<String, String> headers();
}
```

### 2) Сбор событий из агрегатов (DDD-friendly)

Вариант A (наследование):

```java
public abstract class AggregateRoot {
  private final List<DomainEvent> domainEvents = new ArrayList<>();
  protected void addEvent(DomainEvent e) { domainEvents.add(e); }
  public List<DomainEvent> pullEvents() { ... } // возвращает и очищает
}
```

Вариант B (композиция + интерфейс):

```java
public interface HasDomainEvents {
  List<DomainEvent> pullEvents();
}
```

### 3) Publisher интерфейс

```java
public interface OutboxPublisher {
  PublishResult publish(OutboxMessage msg) throws Exception;
}
```

### 4) Outbox pipeline (ключевой сервис)

```java
public interface OutboxService {
  void enqueue(List<DomainEvent> events); // вызывается в транзакции
}
```

## Хранилище: схема outbox таблицы (Postgres-friendly)

Таблица `outbox_message`:

- `id` UUID PK (`event_id`)
- `occurred_at` timestamp
- `aggregate_type`, `aggregate_id`
- `event_type`
- `payload` jsonb (или text)
- `headers` jsonb
- `status` varchar: `NEW | PUBLISHED | FAILED | DEAD`
- `attempts` int
- `next_attempt_at` timestamp
- `published_at` timestamp nullable
- `error` text nullable

Индексы:

- `(status, next_attempt_at)`
- `(aggregate_type, aggregate_id)`

## Как работает end-to-end

1. Внутри use-case создаётся агрегат, он добавляет `DomainEvent`.
2. Репозиторий сохраняет агрегат, затем `outboxService.enqueue(aggregate.pullEvents())` — всё в одной транзакции.
3. Фоновый процесс (scheduler) периодически:
   - читает пачку `NEW/FAILED` с `next_attempt_at <= now`;
   - публикует через `OutboxPublisher`;
   - помечает `PUBLISHED` или увеличивает `attempts` и назначает backoff;
   - после `N` попыток → `DEAD` (и метрика/лог).

## Автоконфигурация starter’а (что включать “из коробки”)

### Properties

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
    type: kafka   # none|kafka|amqp|http
```

### Auto-config beans

- `OutboxService`
- `OutboxRepository` (JDBC / JPA)
- `OutboxProcessor` (poller)
- `OutboxPublisher` (условно по classpath / property)
- Micrometer timers/counters
- `HealthIndicator` (задержка outbox, количество `FAILED/DEAD`)

## Важные детали, которые отличают “игрушку” от enterprise

- **Конкуренция**: несколько инстансов приложения.
  - Решение: `SELECT ... FOR UPDATE SKIP LOCKED` (Postgres) или advisory locks.
- **Сериализация**: версия схемы события + явный `type`.
  - Можно дать SPI: `EventSerializer` (Jackson).
- **Идемпотентность**: `event_id` уникальный → можно безопасно ретраить.
- **At-least-once**: честно описать это в README и предложить consumer dedupe.
- **DLQ**: статус `DEAD` + отдельный endpoint / admin view.
- **Observability**: latency (`occurredAt → publishedAt`), retry count, backlog size.

## Что показать в example-app (чтобы HR/Tech Lead “купил”)

- Use-case **CreateOrder**:
  - сохраняет `Order`;
  - записывает `OrderCreatedEvent` в outbox;
  - publisher “в никуда” (лог) или Kafka (если подключишь Testcontainers).
- Тесты:
  - интеграционный: транзакция → запись в outbox;
  - интеграционный: poller публикует и помечает `PUBLISHED`;
  - тест на ретраи: publisher падает 2 раза → `attempts` увеличились → потом success;
  - конкурентный тест (по желанию): два poller’а не публикуют одно и то же.

## План реализации на GitHub

1. `core` интерфейсы + модели (`DomainEvent`, `OutboxMessage`, `OutboxService`).
2. `jdbc` репозиторий + миграция (Flyway/Liquibase).
3. `starter` автоконфигурация + properties.
4. poller + retry/backoff.
5. `example-app` + Testcontainers + интеграционные тесты.
6. Observability (Micrometer) + `HealthIndicator`.
7. (опционально) `publisher-kafka`.

## Рекомендация по неймингу и позиционированию

**Название**: `spring-boot-starter-domain-outbox`

**Подзаголовок**: “Transactional Domain Events with Outbox Pattern for Spring Boot (JDBC/JPA, retries, metrics, multi-instance safe).”

## Структура репозитория (multi-module Gradle)

```text
spring-boot-starter-domain-outbox
├── README.md
├── LICENSE
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradle
│   └── wrapper
│       └── gradle-wrapper.properties
├── outbox-core
│   ├── build.gradle.kts
│   └── src
│       └── main
│           └── java
├── outbox-jdbc
│   ├── build.gradle.kts
│   └── src
│       └── main
│           ├── java
│           └── resources
│               └── db
│                   └── changelog
│                       ├── db.changelog-master.yaml
│                       └── 001-create-outbox.yaml
├── outbox-spring-boot-starter
│   ├── build.gradle.kts
│   └── src
│       └── main
│           ├── java
│           └── resources
│               └── META-INF
│                   └── spring
│                       └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
└── example-app
    ├── build.gradle.kts
    └── src
        └── main
            ├── java
            └── resources
                └── application.yaml
```

## Актуальные версии (Java 25)

- Spring Boot **4.0.1** (официально совместим до Java 25).
- Gradle **9.2.1**.
- Liquibase Gradle plugin **org.liquibase.gradle 3.1.0**.
- Требование Spring Boot Gradle plugin: Gradle **8.14+** или **9.x**.

## Gradle: root настройки (Java 25, Spring Boot 4.0.1, публикация)

**gradle/wrapper/gradle-wrapper.properties**

```properties
distributionUrl=https\://services.gradle.org/distributions/gradle-9.2.1-bin.zip
```

**settings.gradle.kts**

```kotlin
rootProject.name = "spring-boot-starter-domain-outbox"

include(
  "outbox-core",
  "outbox-jdbc",
  "outbox-spring-boot-starter",
  "example-app"
)
```

**build.gradle.kts (root)**

```kotlin
plugins {
  // Применяем точечно по модулям, здесь можно не включать boot-плагин глобально
  id("maven-publish")
}

allprojects {
  group = "io.github.<your-gh>"
  version = "0.1.0-SNAPSHOT"

  repositories {
    mavenCentral()
  }
}

subprojects {
  plugins.withId("java") {
    the<JavaPluginExtension>().toolchain {
      languageVersion.set(JavaLanguageVersion.of(25))
    }
  }
}
```

**gradle.properties**

```properties
org.gradle.jvmargs=-Xmx2g -Dfile.encoding=UTF-8
kotlin.code.style=official

# Версии (удобно держать тут)
springBootVersion=4.0.1
liquibaseGradlePluginVersion=3.1.0
```

## Liquibase: миграции (Outbox)

**outbox-jdbc/src/main/resources/db/changelog/db.changelog-master.yaml**

```yaml
databaseChangeLog:
  - include:
      file: db/changelog/001-create-outbox.yaml
```

**outbox-jdbc/src/main/resources/db/changelog/001-create-outbox.yaml**

```yaml
databaseChangeLog:
  - changeSet:
      id: 001-create-outbox-message
      author: you
      changes:
        - createTable:
            tableName: outbox_message
            columns:
              - column:
                  name: id
                  type: uuid
                  constraints:
                    primaryKey: true
                    nullable: false

              - column:
                  name: occurred_at
                  type: timestamptz
                  constraints:
                    nullable: false

              - column:
                  name: aggregate_type
                  type: varchar(200)
                  constraints:
                    nullable: false

              - column:
                  name: aggregate_id
                  type: varchar(200)
                  constraints:
                    nullable: false

              - column:
                  name: event_type
                  type: varchar(300)
                  constraints:
                    nullable: false

              - column:
                  name: event_version
                  type: int
                  defaultValueNumeric: 1
                  constraints:
                    nullable: false

              - column:
                  name: payload
                  type: jsonb
                  constraints:
                    nullable: false

              - column:
                  name: headers
                  type: jsonb
                  constraints:
                    nullable: false

              - column:
                  name: status
                  type: varchar(20)
                  constraints:
                    nullable: false

              - column:
                  name: attempts
                  type: int
                  defaultValueNumeric: 0
                  constraints:
                    nullable: false

              - column:
                  name: next_attempt_at
                  type: timestamptz
                  constraints:
                    nullable: false

              - column:
                  name: published_at
                  type: timestamptz

              - column:
                  name: error
                  type: text

        # Ограничение статуса
        - addCheckConstraint:
            tableName: outbox_message
            constraintName: chk_outbox_status
            checkConstraint: "status in ('NEW','PUBLISHED','FAILED','DEAD')"

        # Индекс для poller-а: быстро выбирать то, что пора публиковать
        - createIndex:
            tableName: outbox_message
            indexName: idx_outbox_status_next_attempt
            columns:
              - column:
                  name: status
              - column:
                  name: next_attempt_at

        # Индекс для расследований/корреляций по агрегату
        - createIndex:
            tableName: outbox_message
            indexName: idx_outbox_aggregate
            columns:
              - column:
                  name: aggregate_type
              - column:
                  name: aggregate_id

        # Индекс по occurred_at полезен для админки/метрик (backlog age)
        - createIndex:
            tableName: outbox_message
            indexName: idx_outbox_occurred_at
            columns:
              - column:
                  name: occurred_at
```

### Практические замечания

- `next_attempt_at` обязателен — даже для `NEW` ставь `now()` при вставке; poller тогда прост.
- `headers` можно хранить пустым `{}`; это упрощает сериализацию.
- Если хочешь “совсем enterprise”, добавь partitioning (в README как опциональную рекомендацию), но в миграции для starter’а лучше не усложнять.

## Liquibase в Gradle (как подключить в outbox-jdbc)

**outbox-jdbc/build.gradle.kts (минимальная часть)**

```kotlin
plugins {
  `java-library`
  id("org.liquibase.gradle") version (property("liquibaseGradlePluginVersion") as String)
}

dependencies {
  // Для запуска liquibase tasks (runtime classpath)
  liquibaseRuntime("org.liquibase:liquibase-core")

  // Драйвер БД для выполнения миграций локально/в CI
  liquibaseRuntime("org.postgresql:postgresql")
}

liquibase {
  activities.register("main") {
    // Значения лучше брать из env/gradle properties в CI
    this.arguments = mapOf(
      "changelogFile" to "db/changelog/db.changelog-master.yaml",
      "url" to (System.getenv("DB_URL") ?: "jdbc:postgresql://localhost:5432/outbox"),
      "username" to (System.getenv("DB_USER") ?: "outbox"),
      "password" to (System.getenv("DB_PASS") ?: "outbox")
    )
  }
  runList = "main"
}
```

## Что ещё стоит добавить в README (коротко, но “по-взрослому”)

- Гарантия доставки: **at-least-once**.
- Конкурентность: рекомендация `SELECT … FOR UPDATE SKIP LOCKED` для нескольких инстансов.
- Идемпотентность: `id = event_id` как PK.
- Схема статусов и политика ретраев.
- Observability: backlog size, publish latency.
