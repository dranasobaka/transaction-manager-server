package io.transatron.transaction.manager.scheduler.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SubscriptionType {

    ONE_TIME("one-time"),
    CRON("cron"),
    PERIODIC("periodic");

    private final String value;

    public static SubscriptionType findByValue(String value) {
        for (var subscriptionType : SubscriptionType.values()) {
            if (subscriptionType.getValue().equals(value)) {
                return subscriptionType;
            }
        }
        throw new RuntimeException("Not supported type " + value);
    }
}
