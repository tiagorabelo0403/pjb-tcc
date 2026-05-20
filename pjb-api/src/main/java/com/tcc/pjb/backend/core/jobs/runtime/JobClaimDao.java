package com.tcc.pjb.backend.core.jobs.runtime;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class JobClaimDao {

    private final JdbcTemplate jdbc;

    public JobClaimDao(JdbcTemplate jdbc) {
        this.jdbc = Objects.requireNonNull(jdbc);
    }

    public List<JobClaim> claimNext(String instanceId, Instant now, Instant expiredLockAt, int limit) {
        String sql = "WITH cte AS (" +
                "  SELECT id, type, owner_user_id, inbox_key FROM tb_job" +
                "  WHERE status IN ('PENDING','FAILED')" +
                "    AND (next_retry_at IS NULL OR next_retry_at <= ?)" +
                "    AND (locked_at IS NULL OR locked_at <= ?)" +
                "  ORDER BY priority DESC, created_at ASC" +
                "  FOR UPDATE SKIP LOCKED" +
                "  LIMIT ?" +
                ")" +
                " UPDATE tb_job j" +
                " SET status='RUNNING', locked_by=?, locked_at=?, updated_at=?" +
                " FROM cte" +
                " WHERE j.id = cte.id AND j.status <> 'PAUSED'" +
                " RETURNING j.id, j.type, j.owner_user_id, j.inbox_key";

        return jdbc.query(sql, ps -> {
            ps.setTimestamp(1, Timestamp.from(now));
            ps.setTimestamp(2, Timestamp.from(expiredLockAt));
            ps.setInt(3, limit);
            ps.setString(4, instanceId);
            ps.setTimestamp(5, Timestamp.from(now));
            ps.setTimestamp(6, Timestamp.from(now));
        }, (ResultSet rs) -> {
            java.util.ArrayList<JobClaim> claims = new java.util.ArrayList<>();
            while (rs.next()) {
                Object o = rs.getObject(1);
                String type = rs.getString(2);
                String ownerUserId = rs.getString(3);
                String inboxKey = rs.getString(4);
                UUID id;
                if (o instanceof UUID u) {
                    id = u;
                } else if (o != null) {
                    id = UUID.fromString(o.toString());
                } else {
                    continue;
                }
                claims.add(JobClaim.of(id, type, ownerUserId, inboxKey));
            }
            return claims;
        });
    }

    public int touchLock(UUID jobId, String instanceId, Instant now) {
        Timestamp ts = Timestamp.from(now);
        return jdbc.update("UPDATE tb_job SET locked_at=?, updated_at=? WHERE id=? AND locked_by=?", ts, ts, jobId, instanceId);
    }

    public int pauseAllByType(String jobType, String reason, Instant now) {
        Timestamp ts = Timestamp.from(now);
        return jdbc.update("UPDATE tb_job SET status='PAUSED', paused_at=?, pause_reason=?, locked_by=NULL, locked_at=NULL, updated_at=? WHERE type=? AND status IN ('PENDING','FAILED')", ts, reason, ts, jobType);
    }

    public int resumeAllByType(String jobType, Instant now) {
        return jdbc.update("UPDATE tb_job SET status='PENDING', paused_at=NULL, pause_reason=NULL, next_retry_at=NULL, updated_at=? WHERE type=? AND status='PAUSED'", Timestamp.from(now), jobType);
    }
}
