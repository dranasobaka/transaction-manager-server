package io.transatron.transaction.manager.logic.estimator;

import io.transatron.transaction.manager.web3.ResourceProviderService;
import io.transatron.transaction.manager.web3.model.ResourceProvider;
import io.transatron.transaction.manager.web3.model.TronAccountResources;
import io.transatron.transaction.manager.web3.model.TronAccountResourcesProviders;
import io.transatron.transaction.manager.repository.OrderRepository;
import io.transatron.transaction.manager.repository.ResourceAddressRepository;
import io.transatron.transaction.manager.web3.utils.ThreadUtils;
import io.transatron.transaction.manager.web3.utils.TronAddressUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Component
public class OrderResourcesEstimator {

    private static final long BLOCK_CREATION_MILLIS = 3_000;

    private final OrderRepository orderRepository;

    private final ResourceAddressRepository resourceAddressRepository;

    private final ResourceProviderService resourceProviderService;

    private final Clock clock;

    public Long estimateEnergy(Timestamp fulfillmentTime) {
        var resourceProviders = resourceAddressRepository.findAll().stream()
                .map(entity -> {
                    var address = TronAddressUtils.toBase58(entity.getAddress());
                    var managerAddress = TronAddressUtils.toBase58(entity.getManagerAddress());
                    return new ResourceProvider(address, managerAddress);
                })
                .toList();
        var availableAccountsWithResources = resourceProviderService.getAvailableResourceProviders(resourceProviders);

        ThreadUtils.sleepQuietly(BLOCK_CREATION_MILLIS);

        var energyRecoveryMap = getEnergyRecoveryAmountForAccounts(availableAccountsWithResources);

        // 1) calculate energy we should allocate for orders within range [fulfillment date-time - 1 day, fulfillment date-time]
        var maxAvailableEnergy = availableAccountsWithResources.energyProviders().stream()
                .collect(Collectors.toMap(TronAccountResources::address, TronAccountResources::totalStakedEnergy));
        var availableAccountEnergy = availableAccountsWithResources.energyProviders().stream()
                .collect(Collectors.toMap(TronAccountResources::address, TronAccountResources::availableEnergy));

        var fulfilmentFrom = Timestamp.from(clock.instant());
        var preOrders = orderRepository.findOrdersFulfillingWithinTimeRange(fulfilmentFrom, fulfillmentTime);

        for (var orderEntity : preOrders) {
            var accountAvailableEnergyAtFulfillingTime = energyRecoveryMap.entrySet().stream()
                    .map(resourcesEntry -> {
                        var walletAddress = resourcesEntry.getKey();
                        var recoveredEnergyUnits = resourcesEntry.getValue();

                        var estimatedRecoveredEnergy = estimateRecoveredEnergy(orderEntity.getFulfillFrom(), recoveredEnergyUnits);

                        var availableEnergyAtFulfillingTime = Math.min(availableAccountEnergy.get(walletAddress) + estimatedRecoveredEnergy, maxAvailableEnergy.get(walletAddress));

                        return new AbstractMap.SimpleEntry<>(walletAddress, availableEnergyAtFulfillingTime);
                    })
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            long energyToDelegate = orderEntity.getOwnEnergy();
            for (var entry : accountAvailableEnergyAtFulfillingTime.entrySet()) {
                var walletAddress = entry.getKey();
                var availableEnergy = entry.getValue();

                if (energyToDelegate > availableEnergy) {
                    availableAccountEnergy.put(walletAddress, 0L);
                    energyToDelegate -= availableEnergy;
                } else {
                    availableEnergy -= energyToDelegate;
                    availableAccountEnergy.put(walletAddress, availableEnergy);
                    break;
                }
            }
        }

        // amount of energy available at the moment of fulfilling of the newly requested order
        var minimalAvailableEnergy = availableAccountEnergy.values().stream()
                .reduce(Long::sum)
                .orElse(0L);

        // 2) calculate energy we should allocate for orders within range [fulfillment date-time, fulfillment date-time + 1 day]
        var postOrdersAvailableAccountEnergy = new HashMap<>(availableAccountEnergy);

        var fulfilmentTo = Timestamp.from(fulfillmentTime.toInstant().plus(Duration.ofHours(24)));
        var postOrders = orderRepository.findOrdersFulfillingWithinTimeRange(fulfillmentTime, fulfilmentTo);

        for (var orderEntity : postOrders) {
            var accountAvailableEnergyAtFulfillingTime = energyRecoveryMap.entrySet().stream()
                    .map(resourcesEntry -> {
                        var walletAddress = resourcesEntry.getKey();
                        var recoveredEnergyUnits = resourcesEntry.getValue();

                        var estimatedRecoveredEnergy = estimateRecoveredEnergy(orderEntity.getFulfillFrom(), recoveredEnergyUnits);

                        var availableEnergyAtFulfillingTime = Math.min(postOrdersAvailableAccountEnergy.get(walletAddress) + estimatedRecoveredEnergy, maxAvailableEnergy.get(walletAddress));

                        return new AbstractMap.SimpleEntry<>(walletAddress, availableEnergyAtFulfillingTime);
                    })
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            long energyToDelegate = orderEntity.getOwnEnergy();
            for (var entry : accountAvailableEnergyAtFulfillingTime.entrySet()) {
                var walletAddress = entry.getKey();
                var availableEnergy = entry.getValue();

                if (energyToDelegate > availableEnergy) {
                    postOrdersAvailableAccountEnergy.put(walletAddress, 0L);
                    energyToDelegate -= availableEnergy;
                } else {
                    availableEnergy -= energyToDelegate;
                    postOrdersAvailableAccountEnergy.put(walletAddress, availableEnergy);
                    break;
                }
            }

            var freeEnergy = postOrdersAvailableAccountEnergy.values().stream().reduce(Long::sum).orElse(0L);
            minimalAvailableEnergy = Math.min(minimalAvailableEnergy, freeEnergy);
        }

        return minimalAvailableEnergy;
    }

    private ConcurrentHashMap<String, Long> getEnergyRecoveryAmountForAccounts(TronAccountResourcesProviders accountResourcesProviders) {
        var executorService = Executors.newFixedThreadPool(accountResourcesProviders.energyProviders().size());

        var energyRecoveryMap = new ConcurrentHashMap<String, Long>();

        for (var energyResourceProvider : accountResourcesProviders.energyProviders()) {
            executorService.submit(() -> {
                var newResources = resourceProviderService.getAccountResources(energyResourceProvider.address(), energyResourceProvider.managerAddress());
                var energyDelta = newResources.availableEnergy() - energyResourceProvider.availableEnergy();
                energyRecoveryMap.put(energyResourceProvider.address(), energyDelta);
            });
        }
        try {
            executorService.awaitTermination(3, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            log.error("Unable to request resources from TRON node", ex);
        }

        return energyRecoveryMap;
    }

    private long estimateRecoveredEnergy(Timestamp fulfillmentTime, long energyRecoveredPerBlock) {
        var nowSeconds = clock.millis() / 1000;
        var fulfillmentTimeSeconds = fulfillmentTime.getTime() / 1000;

        return (fulfillmentTimeSeconds - nowSeconds) / BLOCK_CREATION_MILLIS * energyRecoveredPerBlock;
    }

}
