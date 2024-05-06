package io.transatron.transaction.manager.logic;

import io.transatron.transaction.manager.domain.PaymentInfo;
import io.transatron.transaction.manager.logic.configuration.properties.PaymentInfoProperties;
import io.transatron.transaction.manager.repository.ResourceAddressRepository;
import io.transatron.transaction.manager.web3.ResourceProviderService;
import io.transatron.transaction.manager.web3.model.TronAccountResources;
import io.transatron.transaction.manager.web3.utils.TronAddressUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class PaymentInfoService {

    private final ResourceAddressRepository resourceAddressRepository;

    private final ResourceProviderService resourceProviderService;

    private final PaymentInfoProperties properties;

    public PaymentInfo getPaymentInfo() {
        var resourceAddresses = resourceAddressRepository.findAll();

        var accountsResources = resourceAddresses.stream()
                .map(entity -> {
                    var address = TronAddressUtils.toBase58(entity.getAddress());
                    var managerAddress = TronAddressUtils.toBase58(entity.getManagerAddress());
                    return resourceProviderService.getAccountResources(address, managerAddress);
                })
                .toList();

        var availableEnergy = accountsResources.stream()
                .map(TronAccountResources::availableEnergy)
                .reduce(Long::sum)
                .orElse(0L);
        var availableBandwidth = accountsResources.stream()
                .map(TronAccountResources::availableBandwidth)
                .reduce(Long::sum)
                .orElse(0L);

        return new PaymentInfo(properties.getDepositAddress(), availableEnergy, availableBandwidth);
    }

}
