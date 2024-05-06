package io.transatron.transaction.manager.web3.utils;

import lombok.experimental.UtilityClass;

import java.util.function.Supplier;

import static java.util.Objects.nonNull;

@UtilityClass
public class TronRequestUtils {

    public static <T> T delayIfRequested(Supplier<T> task, Long requestDelay) {
        if (nonNull(requestDelay) && requestDelay > 0) {
            ThreadUtils.sleepQuietly(requestDelay);
        }
        return task.get();
    }

}
