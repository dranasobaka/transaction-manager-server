package io.transatron.transaction.manager.logic.validation;

import io.transatron.transaction.manager.domain.OrderStatus;
import io.transatron.transaction.manager.domain.Transaction;
import io.transatron.transaction.manager.domain.exception.BadRequestException;
import io.transatron.transaction.manager.repository.OrderRepository;
import io.transatron.transaction.manager.web3.utils.TronAddressUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Clock;
import java.util.List;
import java.util.Set;

import static io.transatron.transaction.manager.domain.OrderStatus.IN_PROGRESS;
import static io.transatron.transaction.manager.domain.OrderStatus.SCHEDULED;
import static io.transatron.transaction.manager.domain.exception.ErrorsTable.VALIDATION_FAILED;
import static org.apache.commons.lang3.ObjectUtils.isNotEmpty;
import static org.apache.commons.lang3.ObjectUtils.max;

@Component
@RequiredArgsConstructor
public class CreateOrderValidator {

    private static final int MILLIS_IN_HOUR = 3600 * 1000;
    private static final Set<OrderStatus> ACTIVE_ORDER_STATUSES = Set.of(IN_PROGRESS, SCHEDULED);

    private final Clock clock;

    private final OrderRepository repository;

    public void validate(List<Transaction> userTxs, Timestamp fulfillFrom) {
        assertFromAddress(userTxs);
        assertFulfillingTime(fulfillFrom);
        assertNoActiveOrdersCreated(userTxs.get(0).from());
    }

    public void validate(List<Transaction> userTxs, Transaction paymentTx, Timestamp fulfillFrom) {
        assertFromAddress(userTxs, paymentTx);
        assertFulfillingTime(fulfillFrom);
        assertNoActiveOrdersCreated(paymentTx.from());
        // TODO: verify amount in payment transaction = expected payment amount ?
    }

    private void assertFromAddress(List<Transaction> userTxs, Transaction paymentTx) {
        var fromAddresses = userTxs.stream()
                                   .map(Transaction::from)
                                   .distinct()
                                   .toList();

        if (fromAddresses.size() > 1 || !fromAddresses.get(0).equals(paymentTx.from())) {
            throw new BadRequestException("All transactions must have single sender address", VALIDATION_FAILED);
        }
    }

    private void assertFromAddress(List<Transaction> userTxs) {
        var fromAddresses = userTxs.stream()
                                   .map(Transaction::from)
                                   .distinct()
                                   .toList();

        if (fromAddresses.size() > 1) {
            throw new BadRequestException("All transactions must have single sender address", VALIDATION_FAILED);
        }
    }

    private void assertFulfillingTime(Timestamp fulfillFrom) {
        var minFulfillFromTime = clock.instant().toEpochMilli() + MILLIS_IN_HOUR;
        var maxFulfillFromTime = clock.instant().toEpochMilli() + (MILLIS_IN_HOUR * 47);

        if (fulfillFrom.getTime() < minFulfillFromTime) {
            throw new BadRequestException("Earliest available fulfilling time is 1 hour from now", VALIDATION_FAILED);
        }
        if (fulfillFrom.getTime() > maxFulfillFromTime) {
            throw new BadRequestException("Maximum delay for transactions is 47 hours", VALIDATION_FAILED);
        }
    }

    private void assertNoActiveOrdersCreated(String walletAddress) {
        var address = TronAddressUtils.toHex(walletAddress);
        var foundOrders = repository.findAllByWalletAddressAndStatusIn(address, ACTIVE_ORDER_STATUSES);
        if (isNotEmpty(foundOrders)) {
            throw new BadRequestException("There is already active order for this wallet. Limit of active orders is 1", VALIDATION_FAILED);
        }
    }

}
