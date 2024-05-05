package io.transatron.transaction.manager.web3.model;

import java.util.List;

public record TronAccountResourcesProviders(List<TronAccountResources> energyProviders,
                                            List<TronAccountResources> bandwidthProviders) {
}
