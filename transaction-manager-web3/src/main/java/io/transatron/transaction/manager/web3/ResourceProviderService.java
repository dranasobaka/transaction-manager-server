package io.transatron.transaction.manager.web3;

import io.transatron.transaction.manager.web3.model.ResourceProvider;
import io.transatron.transaction.manager.web3.model.TronAccountResources;
import io.transatron.transaction.manager.web3.model.TronAccountResourcesProviders;
import io.transatron.transaction.manager.web3.utils.TronAddressUtils;
import io.transatron.transaction.manager.web3.utils.TronUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.tron.trident.core.ApiWrapper;
import org.tron.trident.proto.Common;
import org.tron.trident.proto.Response;

import java.util.ArrayList;
import java.util.List;

import static io.transatron.transaction.manager.web3.TronConstants.TRX_DECIMALS;

@Slf4j
@RequiredArgsConstructor
@Component
public class ResourceProviderService {

    private final ApiWrapper apiWrapper;

    public TronAccountResources getAccountResources(String address, String managerAddress) {
        var providerTronAccount = apiWrapper.getAccount(address);
        var providerTronAccountResources = apiWrapper.getAccountResource(address);

        return getResourcesStaked(providerTronAccount, providerTronAccountResources)
                    .address(address)
                    .managerAddress(managerAddress)
                    .build();
    }

    public TronAccountResourcesProviders getAvailableResourceProviders(List<ResourceProvider> resourceProviders) {
        var energyProviders = new ArrayList<TronAccountResources>();
        var bandwidthProviders = new ArrayList<TronAccountResources>();

        for (var provider : resourceProviders) {
            var resourceAddress58 = provider.address();
            var resourceManagerAddress58 = provider.managerAddress();

            var providerTronAccount = apiWrapper.getAccount(resourceAddress58);
            var providerTronAccountResources = apiWrapper.getAccountResource(resourceAddress58);

            var verifiedPermissionID = TronUtils.isResourceAccountSetupValid(providerTronAccount, resourceManagerAddress58);
            if (verifiedPermissionID < 0) {
                continue;
            }

            var accountResources = getResourcesStaked(providerTronAccount, providerTronAccountResources)
                                        .address(resourceAddress58)
                                        .managerAddress(resourceManagerAddress58)
                                        .build();
            if (accountResources.totalStakedEnergy() > 0) {
                energyProviders.add(accountResources);
            }
            if (accountResources.totalStakedBandwidth() > 0) {
                bandwidthProviders.add(accountResources);
            }
        }

        return new TronAccountResourcesProviders(energyProviders, bandwidthProviders);
    }

    public TronAccountResources.TronAccountResourcesBuilder getResourcesStaked(Response.Account resourceAccountData,
                                                                               Response.AccountResourceMessage resourceAccResources) {
        var trxBalance = resourceAccountData.getBalance();
        var delegatedEnergy = resourceAccountData.getAccountResource().getDelegatedFrozenV2BalanceForEnergy();
        var delegatedBandwidth = resourceAccountData.getDelegatedFrozenV2BalanceForBandwidth();

        var keepMinimumBandwidth = 300;   // single delegate/undelegate

        var energyUsedTRX = (long) (((double) resourceAccResources.getEnergyUsed()) * resourceAccResources.getTotalEnergyWeight() / 90000000000L * TRX_DECIMALS);
        var netUsedTRX = (long) (((double) resourceAccResources.getNetUsed() + keepMinimumBandwidth) * resourceAccResources.getTotalNetWeight() / 43200000000L * TRX_DECIMALS);

        log.info("---------------------------");
        log.info("ResourceAddress: {}", TronAddressUtils.tronHexToBase58(resourceAccountData.getAddress()));
        log.info("TRX: {}, Delegated Energy TRX: {}, Delegated Bandwidth TRX: {}",
                 trxBalance * 1.0 / TRX_DECIMALS, delegatedEnergy * 1.0 / TRX_DECIMALS, delegatedBandwidth * 1.0 / TRX_DECIMALS);
        log.info("Energy: {} / {}", resourceAccResources.getEnergyLimit(), resourceAccResources.getEnergyUsed());
        log.info("Bandwidth: {} / {}", resourceAccResources.getNetLimit(), resourceAccResources.getNetUsed());
        log.info("Free Net: {} / {}", resourceAccResources.getFreeNetLimit(), resourceAccResources.getFreeNetUsed());
        log.info("Energy used TRX: {}", energyUsedTRX / TRX_DECIMALS);
        log.info("Net used TRX: {}", netUsedTRX / TRX_DECIMALS);

        long totalStakenEnergyTRX = 0;
        long totalStakenBandwidthTRX = 0;

        for (var freezeRow : resourceAccountData.getFrozenV2List()) {
            var amount = freezeRow.getAmount();
            var code = freezeRow.getType();
            if (code == Common.ResourceCode.ENERGY) {
                totalStakenEnergyTRX += amount;
            } else if (code == Common.ResourceCode.BANDWIDTH) {
                totalStakenBandwidthTRX += amount;
            }
        }

        var availableEnergyTRX = totalStakenEnergyTRX - delegatedEnergy - energyUsedTRX;
        var availableBandwidthTRX = totalStakenBandwidthTRX - delegatedBandwidth - netUsedTRX;

        log.info("totalStakenEnergyTRX = {}", totalStakenEnergyTRX * 1.0 / TRX_DECIMALS);
        log.info("totalStakenBandwidthTRX = {}", totalStakenBandwidthTRX * 1.0 / TRX_DECIMALS);
        log.info("availableEnergyTRX = {}", availableEnergyTRX * 1.0 / TRX_DECIMALS);
        log.info("availableBandwidthTRX = {}", availableBandwidthTRX * 1.0 / TRX_DECIMALS);

        return TronAccountResources.builder()
                                   .totalStakedEnergy(totalStakenEnergyTRX)
                                   .totalStakedBandwidth(totalStakenBandwidthTRX)
                                   .availableEnergy(availableEnergyTRX)
                                   .availableBandwidth(availableBandwidthTRX);
    }

}
