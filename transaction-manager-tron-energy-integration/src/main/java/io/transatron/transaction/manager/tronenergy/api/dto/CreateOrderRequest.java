package io.transatron.transaction.manager.tronenergy.api.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {

    @Builder.Default
    private String market = "Open";

    private String address;

    private String target;

    private Long payment;

    private Long price;

    @Builder.Default
    private Integer resource = 0;

    private Long duration;

    @Builder.Default
    private Boolean partfill = Boolean.TRUE;

    @Builder.Default
    private Boolean bulk = Boolean.FALSE;

    private String apiKey;

}
