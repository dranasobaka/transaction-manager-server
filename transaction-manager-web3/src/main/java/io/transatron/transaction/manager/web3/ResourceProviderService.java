package io.transatron.transaction.manager.web3;

import io.transatron.transaction.manager.web3.configuration.properties.TronProperties;
import io.transatron.transaction.manager.web3.model.ResourceProvider;
import io.transatron.transaction.manager.web3.model.TronAccountResources;
import io.transatron.transaction.manager.web3.model.TronAccountResourcesProviders;
import io.transatron.transaction.manager.web3.utils.TronAddressUtils;
import io.transatron.transaction.manager.web3.utils.TronUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;
import org.tron.trident.core.ApiWrapper;
import org.tron.trident.proto.Common;
import org.tron.trident.proto.Response;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static io.transatron.transaction.manager.web3.TronConstants.TRX_DECIMALS;
import static io.transatron.transaction.manager.web3.utils.TronRequestUtils.delayIfRequested;

@Slf4j
@RequiredArgsConstructor
@Component
public class ResourceProviderService {

    private final ApiWrapper apiWrapper;

    private final TronProperties properties;

    public TronAccountResources getAccountResources(String address, String managerAddress) {
        var providerTronAccount = delayIfRequested(() -> apiWrapper.getAccount(address), properties.requestDelayMillis());
        var providerTronAccountResources = delayIfRequested(() -> apiWrapper.getAccountResource(address), properties.requestDelayMillis());

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

            var providerTronAccount = delayIfRequested(() -> apiWrapper.getAccount(resourceAddress58), properties.requestDelayMillis());
            var providerTronAccountResources = delayIfRequested(() -> apiWrapper.getAccountResource(resourceAddress58), properties.requestDelayMillis());

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
        var delegatedEnergyTRX = resourceAccountData.getAccountResource().getDelegatedFrozenV2BalanceForEnergy();
        var delegatedBandwidthTRX = resourceAccountData.getDelegatedFrozenV2BalanceForBandwidth();

        var keepMinimumBandwidth = 300;   // single delegate/undelegate

        var energyUsedTRX = (long) (((double) resourceAccResources.getEnergyUsed()) * resourceAccResources.getTotalEnergyWeight() / 90000000000L * TRX_DECIMALS);
        var netUsedTRX = (long) (((double) resourceAccResources.getNetUsed() + keepMinimumBandwidth) * resourceAccResources.getTotalNetWeight() / 43200000000L * TRX_DECIMALS);

        log.info("---------------------------");
        log.info("ResourceAddress: {}", TronAddressUtils.tronHexToBase58(resourceAccountData.getAddress()));
        log.info("TRX: {}, Delegated Energy TRX: {}, Delegated Bandwidth TRX: {}",
                 trxBalance * 1.0 / TRX_DECIMALS, delegatedEnergyTRX * 1.0 / TRX_DECIMALS, delegatedBandwidthTRX * 1.0 / TRX_DECIMALS);
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

        var availableEnergyTRX = totalStakenEnergyTRX - delegatedEnergyTRX - energyUsedTRX;
        var availableBandwidthTRX = totalStakenBandwidthTRX - delegatedBandwidthTRX - netUsedTRX;

        var availableBandwidth = BigInteger.valueOf(availableBandwidthTRX).multiply(BigInteger.valueOf(43200000000L))
                                                                          .divide(BigInteger.valueOf(TRX_DECIMALS))
                                                                          .divide(BigInteger.valueOf(resourceAccResources.getTotalNetWeight()))
                                                                          .longValue();
        var availableEnergy = BigInteger.valueOf(availableEnergyTRX).multiply(BigInteger.valueOf(90000000000L))
                                                                    .divide(BigInteger.valueOf(TRX_DECIMALS))
                                                                    .divide(BigInteger.valueOf(resourceAccResources.getTotalEnergyWeight()))
                                                                    .longValue();

        var totalBandwidth = BigInteger.valueOf(totalStakenBandwidthTRX).multiply(BigInteger.valueOf(43200000000L))
                                                                        .divide(BigInteger.valueOf(TRX_DECIMALS))
                                                                        .divide(BigInteger.valueOf(resourceAccResources.getTotalNetWeight()))
                                                                        .longValue();
        var totalEnergy = BigInteger.valueOf(totalStakenEnergyTRX).multiply(BigInteger.valueOf(90000000000L))
                                                                  .divide(BigInteger.valueOf(TRX_DECIMALS))
                                                                  .divide(BigInteger.valueOf(resourceAccResources.getTotalEnergyWeight()))
                                                                  .longValue();

        log.info("totalStakenEnergyTRX = {}", totalStakenEnergyTRX * 1.0 / TRX_DECIMALS);
        log.info("totalStakenBandwidthTRX = {}", totalStakenBandwidthTRX * 1.0 / TRX_DECIMALS);
        log.info("availableEnergyTRX = {}", availableEnergyTRX * 1.0 / TRX_DECIMALS);
        log.info("availableBandwidthTRX = {}", availableBandwidthTRX * 1.0 / TRX_DECIMALS);

        return TronAccountResources.builder()
                                   .totalStakedEnergy(totalEnergy)
                                   .totalStakedBandwidth(totalBandwidth)
                                   .availableEnergy(availableEnergy)
                                   .availableBandwidth(availableBandwidth);
    }

    public Long castToEnergyTrx(String address, Long energy) {
        var resourceAccResources = delayIfRequested(() -> apiWrapper.getAccountResource(address), properties.requestDelayMillis());

        return energy * resourceAccResources.getTotalEnergyWeight() / 90000000000L * TRX_DECIMALS;
    }

    public Long castToBandwidthTrx(String address, Long bandwidth) {
        var resourceAccResources = delayIfRequested(() -> apiWrapper.getAccountResource(address), properties.requestDelayMillis());

        return bandwidth * resourceAccResources.getTotalNetWeight() / 43200000000L * TRX_DECIMALS;
    }

}
