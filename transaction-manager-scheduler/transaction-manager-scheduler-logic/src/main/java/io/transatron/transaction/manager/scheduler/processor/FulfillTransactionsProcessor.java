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
import io.transatron.transaction.manager.web3.model.DelegatedResources;
import io.transatron.transaction.manager.web3.model.ResourceProvider;
import io.transatron.transaction.manager.web3.utils.TronAddressUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class FulfillTransactionsProcessor implements Processor {

    private final OrderRepository orderRepository;

    private final ResourceAddressRepository resourceAddressRepository;

    private final ResourceProviderService resourceProviderService;

    private final TronHttpApiFeignClient tronHttpApiClient;

    private final TronTransactionHandler tronTransactionHandler;

    private final WalletsProperties walletsProperties;

    @Override
    public void process(Exchange exchange) throws Exception {
        var notification = exchange.getIn(Notification.class);
        var payload = (HandleOrderPayload) notification.getPayload();

        var optionalOrderEntity = orderRepository.findById(payload.orderId());

        if (optionalOrderEntity.isEmpty()) {
            log.error("Unable to find order with ID = {}", payload.orderId());
            return;
        }
        var orderEntity = optionalOrderEntity.get();

        orderEntity.setStatus(OrderStatus.IN_PROGRESS);
        orderRepository.save(orderEntity);

        var delegatedResourcesProviders = delegateResources(orderEntity);

        var unsuccessfulTransactionsCount = orderEntity.getTransactions().stream()
                .map(this::broadcastTransaction)
                .filter(isSuccessful -> !isSuccessful)
                .count();

        if (unsuccessfulTransactionsCount > 0) {
            orderEntity.setStatus(OrderStatus.PARTIALLY_FULFILLED);
        } else {
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

            var receiverAddress = TronAddressUtils.toBase58(orderEntity.getWalletAddress());
            var permissionId = managerAddressToPermissionID.get(energyProvider.managerAddress());
            var privateKey = walletsProperties.getPrivateKeys().get(energyProvider.managerAddress());
            tronTransactionHandler.delegateEnergy(energyProvider.address(), receiverAddress, energyToDelegate, permissionId, privateKey);

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

            var receiverAddress = TronAddressUtils.toBase58(orderEntity.getWalletAddress());
            var permissionId = managerAddressToPermissionID.get(bandwidthProvider.managerAddress());
            var privateKey = walletsProperties.getPrivateKeys().get(bandwidthProvider.managerAddress());
            tronTransactionHandler.delegateBandwidth(bandwidthProvider.address(), receiverAddress, bandwidthToDelegate, permissionId, privateKey);

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
        var resourceAddressesEntities = resourceAddressRepository.findAll();

        var managerAddressToPermissionID = resourceAddressesEntities.stream()
                .collect(Collectors.toMap(entity -> TronAddressUtils.toBase58(entity.getManagerAddress()), ResourceAddressEntity::getPermissionId));

        usedProviders.forEach(provider -> {
            var receiverAddress = TronAddressUtils.toBase58(orderEntity.getWalletAddress());
            var permissionId = managerAddressToPermissionID.get(provider.managerAddress());
            var privateKey = walletsProperties.getPrivateKeys().get(provider.managerAddress());

            if (provider.delegatedEnergy() > 0) {
                tronTransactionHandler.undelegateEnergy(provider.address(), receiverAddress, provider.delegatedEnergy(), permissionId, privateKey);
            }
            if (provider.delegatedBandwidth() > 0) {
                tronTransactionHandler.undelegateBandwidth(provider.address(), receiverAddress, provider.delegatedBandwidth(), permissionId, privateKey);
            }
        });
    }

    private boolean broadcastTransaction(TransactionEntity txEntity) {
        var request = new BroadcastHexRequest(txEntity.getRawTransaction());
        try {
            tronHttpApiClient.broadcastHex(request);
            txEntity.setStatus(TransactionStatus.SUCCESSFUL);
            return true;
        } catch (Exception ex) {
            txEntity.setStatus(TransactionStatus.FAILED);
            return false;
        }
    }

}
