package io.transatron.transaction.manager.tronenergy.model;

import lombok.Data;

@Data
public class EstimationResult {

    public long energyToDelegate;

    public long energyToDelegateTRXPrice;

    public long trxToSend;

    public long totalEstimatedResourcesCostTRX;

}
