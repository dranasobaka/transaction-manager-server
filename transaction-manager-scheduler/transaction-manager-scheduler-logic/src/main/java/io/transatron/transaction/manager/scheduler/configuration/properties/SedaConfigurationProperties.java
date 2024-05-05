package io.transatron.transaction.manager.scheduler.configuration.properties;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class SedaConfigurationProperties {

    private int queueSize;

    private int poolSize;

    @Builder.Default
    private boolean blockWhenFull = true;

    @Builder.Default
    private int pollTimeout = 1000;

    @NotNull
    private String name;

    public String getUri() {
        return "seda:" + name + "?" +
            "size=" + queueSize + "&" +
            "blockWhenFull=" + blockWhenFull + "&" +
            "concurrentConsumers=" + poolSize + "&" +
            "pollTimeout=" + pollTimeout;
    }

}
