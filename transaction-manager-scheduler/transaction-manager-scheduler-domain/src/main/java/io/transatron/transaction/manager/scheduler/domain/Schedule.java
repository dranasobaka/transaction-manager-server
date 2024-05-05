package io.transatron.transaction.manager.scheduler.domain;

import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class Schedule {

    String cron;
    long fixedRateMillis;
    long startTsMillis;
    long endTsMillis;

    public static Schedule of(String cron, long startTsMillis, long endTsMillis) {
        return new Schedule(cron, 0, startTsMillis, endTsMillis);
    }

    public static Schedule of(long fixedRateMillis, long startTsMillis, long endTsMillis) {
        return new Schedule(null, fixedRateMillis, startTsMillis, endTsMillis);
    }
}
