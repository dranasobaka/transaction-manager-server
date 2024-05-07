package io.transatron.transaction.manager.scheduler.processor;

import io.transatron.transaction.manager.domain.OrderStatus;
import io.transatron.transaction.manager.domain.TransactionStatus;
import io.transatron.transaction.manager.entity.OrderEntity;
import io.transatron.transaction.manager.entity.ResourceAddressEntity;
import io.transatron.transaction.manager.entity.TransactionEntity;
import io.transatron.transaction.manager.repository.OrderRepository;
import io.transatron.transaction.manager.repository.ResourceAddressRepository;
import io.transatron.transaction.manager.scheduler.configuration.properties.WalletsProperties;
import io.transatron.transaction.manager.scheduler.domain.payload.HandleOrderPayload;
import io.transatron.transaction.manager.scheduler.notification.Notification;
import io.transatron.transaction.manager.web3.ResourceProviderService;
import io.transatron.transaction.manager.web3.TronTransactionHandler;
import io.transatron.transaction.manager.web3.api.TronHttpApiFeignClient;
import io.transatron.transaction.manager.web3.api.dto.BroadcastHexRequest;
import io.transatron.transaction.manager.web3.configuration.properties.TronProperties;
import io.transatron.transaction.manager.web3.model.DelegatedResources;
import io.transatron.transaction.manager.web3.model.ResourceProvider;
import io.transatron.transaction.manager.web3.utils.TronAddressUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.transatron.transaction.manager.web3.TronConstants.TRX_DECIMALS;
import static io.transatron.transaction.manager.web3.utils.TronRequestUtils.delayIfRequested;

@Slf4j
@RequiredArgsConstructor
public class FulfillTransactionsProcessor implements Processor {

    private final OrderRepository orderRepository;

    private final ResourceAddressRepository resourceAddressRepository;

    private final ResourceProviderService resourceProviderService;

    private final TronHttpApiFeignClient tronHttpApiClient;

    private final TronTransactionHandler tronTransactionHandler;

    private final WalletsProperties walletsProperties;

    private final TronProperties tronProperties;

    @Override
    public void process(Exchange exchange) throws Exception {
        var notification = exchange.getIn().getBody(Notification.class);
        var payload = (LinkedHashMap) notification.getPayload();
        var orderId = (String) payload.get("orderId");

        var optionalOrderEntity = orderRepository.findById(UUID.fromString(orderId));

        if (optionalOrderEntity.isEmpty()) {
            log.error("Unable to find order with ID = {}", orderId);
            return;
        }
        var orderEntity = optionalOrderEntity.get();

        orderEntity.setStatus(OrderStatus.IN_PROGRESS);
        orderRepository.save(orderEntity);

        log.info("Delegating {} energy and {} bandwidth to '{}' wallet",
                 orderEntity.getOwnEnergy(), orderEntity.getOwnBandwidth(), TronAddressUtils.toBase58(orderEntity.getWalletAddress()));
        var delegatedResourcesProviders = delegateResources(orderEntity);

        log.info("Broadcasting transactions for order '{}'", orderEntity.getId());
        var unsuccessfulTransactionsCount = orderEntity.getTransactions().stream()
                .map(this::broadcastTransaction)
                .filter(isSuccessful -> !isSuccessful)
                .count();

        if (unsuccessfulTransactionsCount > 0) {
            log.info("There were {} unsuccessful transactions", unsuccessfulTransactionsCount);
            orderEntity.setStatus(OrderStatus.PARTIALLY_FULFILLED);
        } else {
            log.info("All transactions are broadcasted!");
            orderEntity.setStatus(OrderStatus.FULFILLED);
        }

        orderRepository.save(orderEntity);

        reclaimResources(orderEntity, delegatedResourcesProviders);
    }

    private List<DelegatedResources> delegateResources(OrderEntity orderEntity) {
        var resourceAddressesEntities = resourceAddressRepository.findAll();

        var managerAddressToPermissionID = resourceAddressesEntities.stream()
                .collect(Collectors.toMap(entity -> TronAddressUtils.toBase58(entity.getManagerAddress()), ResourceAddressEntity::getPermissionId));

        var resourceProviders = resourceAddressesEntities.stream()
                .map(entity -> {
                    var address = TronAddressUtils.toBase58(entity.getAddress());
                    var managerAddress = TronAddressUtils.toBase58(entity.getManagerAddress());
                    return new ResourceProvider(address, managerAddress);
                })
                .toList();
        var energyProviders = resourceProviderService.getAvailableResourceProviders(resourceProviders).energyProviders();

        var usedResources = new HashMap<String, DelegatedResources>();

        var energyLeftToDelegate = orderEntity.getOwnEnergy();
        for (var energyProvider : energyProviders) {
            if (energyLeftToDelegate == 0) {
                break;
            }

            var availableEnergy = energyProvider.availableEnergy();
            if (availableEnergy < 72_000) {     // i.e. if energy less than 2 transactions (delegate and reclaim)
                continue;
            }
            var energyToDelegate = availableEnergy >= energyLeftToDelegate ? energyLeftToDelegate : availableEnergy;
            var energyToDelegateTRX = resourceProviderService.castToEnergyTrx(energyProvider.address(), energyToDelegate);

            var receiverAddress = TronAddressUtils.toBase58(orderEntity.getWalletAddress());
            var permissionId = managerAddressToPermissionID.get(energyProvider.managerAddress());
            var privateKey = walletsProperties.getPrivateKeys().get(energyProvider.managerAddress());
            tronTransactionHandler.delegateEnergy(energyProvider.address(), receiverAddress, energyToDelegateTRX, permissionId, privateKey);

            usedResources.put(energyProvider.address(),
                              new DelegatedResources(energyProvider.address(), energyProvider.managerAddress(), energyToDelegate, 0L));

            energyLeftToDelegate -= energyToDelegate;
        }

        var bandwidthLeftToDelegate = orderEntity.getOwnBandwidth();
        var bandwidthProviders = resourceProviderService.getAvailableResourceProviders(resourceProviders).bandwidthProviders();
        for (var bandwidthProvider : bandwidthProviders) {
            if (bandwidthLeftToDelegate == 0) {
                break;
            }

            var availableBandwidth = bandwidthProvider.availableBandwidth();
            var bandwidthToDelegate = availableBandwidth >= bandwidthLeftToDelegate ? bandwidthLeftToDelegate : availableBandwidth;
            var bandwidthToDelegateTRX = resourceProviderService.castToBandwidthTrx(bandwidthProvider.address(), bandwidthToDelegate);

            var receiverAddress = TronAddressUtils.toBase58(orderEntity.getWalletAddress());
            var permissionId = managerAddressToPermissionID.get(bandwidthProvider.managerAddress());
            var privateKey = walletsProperties.getPrivateKeys().get(bandwidthProvider.managerAddress());
            tronTransactionHandler.delegateBandwidth(bandwidthProvider.address(), receiverAddress, bandwidthToDelegateTRX, permissionId, privateKey);

            if (usedResources.containsKey(bandwidthProvider.address())) {
                var delegatedResources = usedResources.get(bandwidthProvider.address())
                    .toBuilder()
                    .delegatedBandwidth(bandwidthToDelegate)
                    .build();
                usedResources.put(bandwidthProvider.address(), delegatedResources);
            } else {
                usedResources.put(bandwidthProvider.address(),
                                  new DelegatedResources(bandwidthProvider.address(), bandwidthProvider.managerAddress(), 0L, bandwidthToDelegate));
            }

            bandwidthLeftToDelegate -= bandwidthToDelegate;
        }

        return usedResources.values().stream().toList();
    }

    private void reclaimResources(OrderEntity orderEntity, List<DelegatedResources> usedProviders) {
        var userAddress = TronAddressUtils.toBase58(orderEntity.getWalletAddress());

        log.info("Reclaiming resources from '{}'. Resources to reclaim: {}", userAddress, usedProviders);

        var resourceAddressesEntities = resourceAddressRepository.findAll();

        var managerAddressToPermissionID = resourceAddressesEntities.stream()
                .collect(Collectors.toMap(entity -> TronAddressUtils.toBase58(entity.getManagerAddress()), ResourceAddressEntity::getPermissionId));

        usedProviders.forEach(provider -> {
            var permissionId = managerAddressToPermissionID.get(provider.managerAddress());
            var privateKey = walletsProperties.getPrivateKeys().get(provider.managerAddress());

            if (provider.delegatedEnergy() > 0) {
                var energyToReclaimTrx = resourceProviderService.castToEnergyTrx(provider.address(), provider.delegatedEnergy());
                tronTransactionHandler.undelegateEnergy(provider.address(), userAddress, energyToReclaimTrx, permissionId, privateKey);
            }
            if (provider.delegatedBandwidth() > 0) {
                var bandwidthToReclaimTrx = resourceProviderService.castToBandwidthTrx(provider.address(), provider.delegatedBandwidth());
                tronTransactionHandler.undelegateBandwidth(provider.address(), userAddress, bandwidthToReclaimTrx, permissionId, privateKey);
            }
        });

        log.info("Resources are reclaimed from '{}'", userAddress);
    }

    private boolean broadcastTransaction(TransactionEntity txEntity) {
        var request = new BroadcastHexRequest(txEntity.getRawTransaction());
        try {
            var result = delayIfRequested(() -> tronHttpApiClient.broadcastHex(request), tronProperties.requestDelayMillis());
            log.info("Broadcasted transaction result: {}", result);
            txEntity.setStatus(TransactionStatus.SUCCESSFUL);
            return true;
        } catch (Exception ex) {
            txEntity.setStatus(TransactionStatus.FAILED);
            return false;
        }
    }

}
