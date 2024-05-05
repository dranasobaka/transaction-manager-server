package io.transatron.transaction.manager.web3.model;

import lombok.Builder;

@Builder(toBuilder = true)
public record DelegatedResources(String address,
                                 String managerAddress,
                                 Long delegatedEnergy,
                                 Long delegatedBandwidth) {
}
