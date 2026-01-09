package io.github.example.outbox.jdbc;

import io.github.example.outbox.core.OutboxMessage;
import io.github.example.outbox.core.OutboxStatus;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JdbcOutboxRepository implements OutboxRepository {
  private final JdbcTemplate jdbcTemplate;
  private final OutboxMessageHeaderMapper headerMapper;

  @Override
  public void saveAll(List<OutboxMessage> messages) {
    String sql = """
        insert into outbox_message (
          id,
          occurred_at,
          aggregate_type,
          aggregate_id,
          event_type,
          event_version,
          payload,
          headers,
          status,
          attempts,
          next_attempt_at,
          published_at,
          error
        )
        values (?, ?, ?, ?, ?, ?, ?::jsonb, ?::jsonb, ?, ?, ?, ?, ?)
        """;
    jdbcTemplate.batchUpdate(
        sql,
        messages,
        messages.size(),
        (ps, message) -> {
          ps.setObject(1, message.id());
          ps.setTimestamp(2, Timestamp.from(message.occurredAt()));
          ps.setString(3, message.aggregateType());
          ps.setString(4, message.aggregateId());
          ps.setString(5, message.eventType());
          ps.setInt(6, message.eventVersion());
          ps.setString(7, message.payload());
          ps.setString(8, headerMapper.toJson(message.headers()));
          ps.setString(9, message.status().name());
          ps.setInt(10, message.attempts());
          ps.setTimestamp(11, Timestamp.from(message.nextAttemptAt()));
          if (message.publishedAt() != null) {
            ps.setTimestamp(12, Timestamp.from(message.publishedAt()));
          } else {
            ps.setTimestamp(12, null);
          }
          ps.setString(13, message.error());
        });
  }

  @Override
  public List<OutboxMessage> lockNextBatch(List<OutboxStatus> statuses, Instant now, int batchSize) {
    String sql = """
        select id,
               occurred_at,
               aggregate_type,
               aggregate_id,
               event_type,
               event_version,
               payload,
               headers,
               status,
               attempts,
               next_attempt_at,
               published_at,
               error
          from outbox_message
         where status = any (?)
           and next_attempt_at <= ?
         order by occurred_at
         for update skip locked
         limit ?
        """;
    String[] statusValues = statuses.stream()
        .map(Enum::name)
        .toArray(String[]::new);
    return jdbcTemplate.query(
        sql,
        new Object[] {statusValues, Timestamp.from(now), batchSize},
        new OutboxMessageRowMapper()
    );
  }

  @Override
  public int countPending(List<OutboxStatus> statuses, Instant now) {
    String sql = """
        select count(*)
          from outbox_message
         where status = any (?)
           and next_attempt_at <= ?
        """;
    String[] statusValues = statuses.stream()
        .map(Enum::name)
        .toArray(String[]::new);
    Integer count = jdbcTemplate.queryForObject(
        sql,
        new Object[] {statusValues, Timestamp.from(now)},
        Integer.class
    );
    return count == null ? 0 : count;
  }

  @Override
  public void markPublished(UUID id, Instant publishedAt) {
    String sql = """
        update outbox_message
           set status = ?,
               published_at = ?,
               error = null
         where id = ?
        """;
    jdbcTemplate.update(sql, OutboxStatus.PUBLISHED.name(), Timestamp.from(publishedAt), id);
  }

  @Override
  public void markFailed(UUID id, int attempts, Instant nextAttemptAt, String error) {
    String sql = """
        update outbox_message
           set status = ?,
               attempts = ?,
               next_attempt_at = ?,
               error = ?
         where id = ?
        """;
    jdbcTemplate.update(
        sql,
        OutboxStatus.FAILED.name(),
        attempts,
        Timestamp.from(nextAttemptAt),
        error,
        id
    );
  }

  @Override
  public void markDead(UUID id, int attempts, String error) {
    String sql = """
        update outbox_message
           set status = ?,
               attempts = ?,
               error = ?
         where id = ?
        """;
    jdbcTemplate.update(sql, OutboxStatus.DEAD.name(), attempts, error, id);
  }

  private final class OutboxMessageRowMapper implements RowMapper<OutboxMessage> {
    @Override
    public OutboxMessage mapRow(ResultSet rs, int rowNum) throws SQLException {
      return new OutboxMessage(
          rs.getObject("id", UUID.class),
          rs.getTimestamp("occurred_at").toInstant(),
          rs.getString("aggregate_type"),
          rs.getString("aggregate_id"),
          rs.getString("event_type"),
          rs.getInt("event_version"),
          rs.getString("payload"),
          headerMapper.fromJson(rs.getString("headers")),
          OutboxStatus.valueOf(rs.getString("status")),
          rs.getInt("attempts"),
          rs.getTimestamp("next_attempt_at").toInstant(),
          rs.getTimestamp("published_at") != null ? rs.getTimestamp("published_at").toInstant() : null,
          rs.getString("error")
      );
    }
  }

  
}
