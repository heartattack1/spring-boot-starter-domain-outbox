package io.github.example.outbox.jdbc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaOutboxMessageRepository extends JpaRepository<OutboxMessageEntity, UUID> {
  @Query(
      value = """
          select *
            from outbox_message
           where status = any (:statuses)
             and next_attempt_at <= :now
           order by occurred_at
           for update skip locked
           limit :batchSize
          """,
      nativeQuery = true
  )
  List<OutboxMessageEntity> lockNextBatch(
      @Param("statuses") String[] statuses,
      @Param("now") Instant now,
      @Param("batchSize") int batchSize
  );

  @Query(
      value = """
          select count(*)
            from outbox_message
           where status = any (:statuses)
             and next_attempt_at <= :now
          """,
      nativeQuery = true
  )
  int countPending(@Param("statuses") String[] statuses, @Param("now") Instant now);

  @Modifying
  @Query(
      value = """
          update outbox_message
             set status = :status,
                 published_at = :publishedAt,
                 error = null
           where id = :id
          """,
      nativeQuery = true
  )
  int markPublished(
      @Param("id") UUID id,
      @Param("status") String status,
      @Param("publishedAt") Instant publishedAt
  );

  @Modifying
  @Query(
      value = """
          update outbox_message
             set status = :status,
                 attempts = :attempts,
                 next_attempt_at = :nextAttemptAt,
                 error = :error
           where id = :id
          """,
      nativeQuery = true
  )
  int markFailed(
      @Param("id") UUID id,
      @Param("status") String status,
      @Param("attempts") int attempts,
      @Param("nextAttemptAt") Instant nextAttemptAt,
      @Param("error") String error
  );

  @Modifying
  @Query(
      value = """
          update outbox_message
             set status = :status,
                 attempts = :attempts,
                 error = :error
           where id = :id
          """,
      nativeQuery = true
  )
  int markDead(
      @Param("id") UUID id,
      @Param("status") String status,
      @Param("attempts") int attempts,
      @Param("error") String error
  );
}
