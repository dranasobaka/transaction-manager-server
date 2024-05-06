package io.transatron.transaction.manager.scheduler.partitioning.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@RequiredArgsConstructor
public class JdbcPartitionLockRepository {

    private static final String INSERT_LOCK_QUERY_TEMPLATE = """
        INSERT INTO locks(s_partition, locked_by_hostname, locked_until_ts)
        VALUES (:partition, :hostname, :lockuntil)
        ON CONFLICT (s_partition) DO UPDATE 
        SET locked_by_hostname = CASE WHEN locks.locked_until_ts < :now THEN EXCLUDED.locked_by_hostname ELSE locks.locked_by_hostname END,
            locked_until_ts = CASE WHEN locks.locked_by_hostname = :hostname OR locks.locked_until_ts < :now THEN EXCLUDED.locked_until_ts ELSE locks.locked_until_ts END;
        """;
    private static final String DELETE_LOCK_QUERY_TEMPLATE = """
        DELETE
        FROM locks 
        WHERE s_partition = :partition 
          AND locked_by_hostname = :hostname
        """;
    private static final String GET_LOCK_BY_PARTITION_QUERY = "SELECT * FROM locks WHERE s_partition = :partition";

    private final NamedParameterJdbcOperations jdbc;

    public boolean upsert(int partition, Instant lockUntil, Instant now) {
        try {
            var before = getLockForPartition(partition);
            var params = Map.of("partition", partition,
                                "hostname", "localhost",
                                "lockuntil", Timestamp.from(lockUntil),
                                "now", Timestamp.from(now));
            var rows = jdbc.update(INSERT_LOCK_QUERY_TEMPLATE, params);
            var after = getLockForPartition(partition);

            // Unfortunately we cannot rely on return from "update(...)" method in a case of PostgreSQL,
            // because even if no actual update happened in UPSERT - we still receive an affected row.
            // Therefore, we comparing partition lock before and after an UPSERT has been executed.
            return rows != 0
                && ((isNull(before) && nonNull(after)) || !before.equals(after));
        } catch (DuplicateKeyException ex) {
            return false;
        }
    }

    public boolean deleteByPartitionAndHostname(Integer partition) {
        var params = Map.of("partition", partition,
                            "hostname", "localhost");
        var rows = jdbc.update(DELETE_LOCK_QUERY_TEMPLATE, params);
        return rows != 0;
    }

    private PartitionLockEntity getLockForPartition(int partition) {
        var params = Map.of("partition", partition);
        try {
            return jdbc.queryForObject(GET_LOCK_BY_PARTITION_QUERY, params, this::getPartitionLockEntity);
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    private PartitionLockEntity getPartitionLockEntity(ResultSet rs, int i) throws SQLException {
        return new PartitionLockEntity(rs.getInt("s_partition"),
                                       rs.getString("locked_by_hostname"),
                                       rs.getTimestamp("locked_until_ts").toInstant());
    }

    public record PartitionLockEntity(Integer partition, String hostname, Instant lockedUntilTsMillis) {
    }

}
