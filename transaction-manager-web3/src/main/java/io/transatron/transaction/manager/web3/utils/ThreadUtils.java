package io.transatron.transaction.manager.web3.utils;

import lombok.experimental.UtilityClass;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

@UtilityClass
public class ThreadUtils {

    public static void sleepQuietly(final long millis) {
        try {
            sleep(millis);
        } catch (final InterruptedException ex) {
            // ignore
        }
    }

    public static void sleep(final long millis) throws InterruptedException {
        MILLISECONDS.sleep(millis);
    }

}
