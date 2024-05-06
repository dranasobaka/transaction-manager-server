package io.transatron.transaction.manager.scheduler.partitioning.locking;

import io.transatron.transaction.manager.scheduler.partitioning.PartitionLock;
import io.transatron.transaction.manager.scheduler.partitioning.repository.JdbcPartitionLockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

import java.time.Instant;

@Slf4j
@RequiredArgsConstructor
public class PostgresPartitionLock implements PartitionLock {

    private final JdbcPartitionLockRepository jdbcPartitionLockRepository;
    private final long lockTtlMillis;

    @Override
    @Retryable(
        maxAttemptsExpression = "${scheduler.service.partition.lock.retryCount:3}",
        backoff = @Backoff(
            delayExpression = "${scheduler.service.partition.lock.retryDelayMillis:1}",
            multiplierExpression = "${scheduler.service.partition.lock.retryMultiplier:2}",
            random = true
        )
    )
    public boolean tryLock(int partition, Instant now) {
        log.debug("Locking partition '{}'", partition);
        var locked = jdbcPartitionLockRepository.upsert(partition, now.plusMillis(lockTtlMillis), now);
        log.debug("Lock for partition '{}' acquired: {} by 'localhost'", partition, locked);
        return locked;
    }

    @Override
    public boolean unlock(int partition) {
        try {
            log.debug("Releasing partition '{}' lock by 'localhost'", partition);
            var deleted = jdbcPartitionLockRepository.deleteByPartitionAndHostname(partition);
            log.debug("Released: {} partition '{}' lock by 'localhost'", deleted, partition);
            return deleted;
        } catch (Exception ex) {
            log.debug("Un-locking partition '{}' failed by 'localhost'", partition);
            return false;
        }
    }

}
