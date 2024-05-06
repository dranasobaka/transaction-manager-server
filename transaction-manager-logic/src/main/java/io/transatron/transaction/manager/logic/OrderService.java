package io.transatron.transaction.manager.logic;

import feign.FeignException;
import io.transatron.transaction.manager.domain.Order;
import io.transatron.transaction.manager.domain.OrderStatus;
import io.transatron.transaction.manager.domain.Transaction;
import io.transatron.transaction.manager.domain.TransactionStatus;
import io.transatron.transaction.manager.domain.exception.BadRequestException;
import io.transatron.transaction.manager.domain.exception.ResourceNotFoundException;
import io.transatron.transaction.manager.entity.OrderEntity;
import io.transatron.transaction.manager.entity.TransactionEntity;
import io.transatron.transaction.manager.logic.api.TransaTronFeignClient;
import io.transatron.transaction.manager.logic.estimator.OrderResourcesEstimator;
import io.transatron.transaction.manager.logic.mapper.OrderMapper;
import io.transatron.transaction.manager.logic.model.OrderUsdtEstimation;
import io.transatron.transaction.manager.logic.validation.CreateOrderValidator;
import io.transatron.transaction.manager.repository.OrderRepository;
import io.transatron.transaction.manager.scheduler.SubscriptionService;
import io.transatron.transaction.manager.scheduler.domain.Subscription;
import io.transatron.transaction.manager.scheduler.domain.SubscriptionId;
import io.transatron.transaction.manager.scheduler.domain.SubscriptionType;
import io.transatron.transaction.manager.scheduler.domain.payload.CreateTronEnergyOrderPayload;
import io.transatron.transaction.manager.scheduler.domain.payload.HandleOrderPayload;
import io.transatron.transaction.manager.web3.TRXDexManager;
import io.transatron.transaction.manager.web3.TronHexDecoder;
import io.transatron.transaction.manager.web3.utils.TronAddressUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static io.transatron.transaction.manager.domain.exception.ErrorsTable.PAYMENT_FAILED;
import static io.transatron.transaction.manager.domain.exception.ErrorsTable.RESOURCE_NOT_FOUND;
import static io.transatron.transaction.manager.scheduler.domain.EventTypes.CREATE_TRON_ENERGY_ORDER;
import static io.transatron.transaction.manager.scheduler.domain.EventTypes.FULFILL_ORDER;
import static io.transatron.transaction.manager.web3.TronConstants.TRX_DECIMALS;

@Slf4j
@RequiredArgsConstructor
@Service
public class OrderService {

    private static final double OWN_ENERGY_PRICE_RATE_SUN = 50;
    private static final double OWN_BANDWIDTH_PRICE_RATE_SUN = 50;
    private static final double EXTERNAL_ENERGY_PRICE_RATE_SUN = 70;

    private final OrderRepository repository;

    private final OrderMapper mapper;

    private final TronHexDecoder txDecoder;

    private final CreateOrderValidator createOrderValidator;

    private final OrderResourcesEstimator resourcesEstimator;

    private final TransaTronFeignClient transaTronClient;

    private final SubscriptionService subscriptionService;

    private final TRXDexManager trxDexManager;

    public Optional<Order> findLastOrder(String walletAddress) {
        var address = TronAddressUtils.toHex(walletAddress);

        return repository.findLastByWalletAddress(address)
                         .map(mapper::toModel);
    }

    public Order findOrderById(UUID orderId) {
        return repository.findById(orderId)
                         .map(mapper::toModel)
                         .orElseThrow(() -> new ResourceNotFoundException("Unable to find requested resource", RESOURCE_NOT_FOUND));
    }

    public OrderUsdtEstimation estimateOrder(List<String> userTransactions, Timestamp fulfillFrom) {
        var decodedUserTxs = userTransactions.stream()
                                             .map(txDecoder::decodeTransaction)
                                             .toList();

        createOrderValidator.validate(decodedUserTxs, fulfillFrom);

        var accumulatedEnergy = decodedUserTxs.stream()
                                              .map(Transaction::estimatedEnergy)
                                              .reduce(Long::sum)
                                              .orElse(0L);
        var accumulatedBandwidth = decodedUserTxs.stream()
                                                 .map(Transaction::estimatedBandwidth)
                                                 .reduce(Long::sum)
                                                 .orElse(0L);

        var availableOwnEnergy = resourcesEstimator.estimateEnergy(fulfillFrom);
        var externalEnergy = (accumulatedEnergy - availableOwnEnergy) > 0 ? accumulatedEnergy - availableOwnEnergy : 0;

        var regularPrice = EXTERNAL_ENERGY_PRICE_RATE_SUN * externalEnergy;
        var transatronPrice = (OWN_ENERGY_PRICE_RATE_SUN * availableOwnEnergy) + (OWN_BANDWIDTH_PRICE_RATE_SUN * accumulatedBandwidth);

        var regularPriceTrx = regularPrice / TRX_DECIMALS;
        var transatronPriceTrx = transatronPrice / TRX_DECIMALS;

        var regularPriceUsdt = trxDexManager.getTokenToTrxOutputPrice(Double.valueOf(regularPriceTrx).longValue());
        var transatronPriceUsdt = trxDexManager.getTokenToTrxOutputPrice(Double.valueOf(transatronPriceTrx).longValue());

        return new OrderUsdtEstimation(availableOwnEnergy, externalEnergy, accumulatedBandwidth, regularPriceUsdt, transatronPriceUsdt);
    }

    public void createOrder(List<String> userTransactions, String paymentTransaction, Timestamp fulfillFrom) {
        var decodedPaymentTx = txDecoder.decodeTransaction(paymentTransaction);
        var decodedUserTxs = userTransactions.stream()
                                             .map(txDecoder::decodeTransaction)
                                             .toList();

        createOrderValidator.validate(decodedUserTxs, decodedPaymentTx, fulfillFrom);

        var orderEstimation = estimateOrder(userTransactions, fulfillFrom);

        try {
            var responseBody = transaTronClient.broadcastHexTransaction(paymentTransaction);
            log.info("Processed payment transaction. Result: {}", responseBody);
        } catch (FeignException ex) {
            log.warn("Payment transaction is failed with error. Aborting further processing.", ex);
            throw new BadRequestException("Unable to process payment transaction", PAYMENT_FAILED);
        }

        if (orderEstimation.externalEnergy() > 0) {
            createTronEnergySubscription(decodedPaymentTx.from(), orderEstimation.externalEnergy(), fulfillFrom);
        }
        createOrder(decodedUserTxs, fulfillFrom, orderEstimation);
    }

    private void createOrder(List<Transaction> userTransactions, Timestamp fulfillFrom, OrderUsdtEstimation orderEstimation) {
        var fulfillTo = Timestamp.from(fulfillFrom.toInstant().plus(Duration.ofHours(1)));
        var fromAddress = userTransactions.get(0).from();

        var transactionEntities = userTransactions.stream()
                .map(this::newTransaction)
                .toList();

        var orderEntity = new OrderEntity();
        orderEntity.setId(UUID.randomUUID());
        orderEntity.setStatus(OrderStatus.SCHEDULED);
        orderEntity.setTransactions(transactionEntities);
        orderEntity.setFulfillFrom(fulfillFrom);
        orderEntity.setFulfillTo(fulfillTo);
        orderEntity.setWalletAddress(TronAddressUtils.toHex(fromAddress));
        orderEntity.setOwnEnergy(orderEstimation.ownEnergy());
        orderEntity.setExternalEnergy(orderEstimation.externalEnergy());
        orderEntity.setOwnBandwidth(orderEstimation.ownBandwidth());

        repository.save(orderEntity);

        var payload = new HandleOrderPayload(orderEntity.getId());

        var subscription = Subscription.builder()
                .subscriptionId(SubscriptionId.of(UUID.randomUUID().toString(), FULFILL_ORDER))
                .subscriptionType(SubscriptionType.ONE_TIME)
                .triggerTsMillis(fulfillFrom.getTime())
                .payload(payload)
                .build();
        subscriptionService.save(subscription);
    }

    private void createTronEnergySubscription(String targetWalletAddress, Long energy, Timestamp fulfillFrom) {
        // create Tron Energy order 15 minutes before actual order
        var triggerTs = fulfillFrom.toInstant().minus(Duration.ofMinutes(15));
        var payload = new CreateTronEnergyOrderPayload(targetWalletAddress, energy);

        var subscription = Subscription.builder()
                .subscriptionId(SubscriptionId.of(UUID.randomUUID().toString(), CREATE_TRON_ENERGY_ORDER))
                .subscriptionType(SubscriptionType.ONE_TIME)
                .triggerTsMillis(triggerTs.toEpochMilli())
                .payload(payload)
                .build();
        subscriptionService.save(subscription);
    }

    private TransactionEntity newTransaction(Transaction tx) {
        var txEntity = new TransactionEntity();
        txEntity.setStatus(TransactionStatus.SCHEDULED);
        txEntity.setTxId(tx.id());
        txEntity.setToAddress(TronAddressUtils.toHex(tx.to()));
        txEntity.setTxAmount(tx.amount());
        txEntity.setRawTransaction(tx.rawTransaction());

        return txEntity;
    }

}
