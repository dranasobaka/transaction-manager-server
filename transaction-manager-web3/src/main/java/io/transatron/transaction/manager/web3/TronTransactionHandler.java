package io.transatron.transaction.manager.web3;

import com.google.protobuf.ByteString;
import io.transatron.transaction.manager.web3.model.SendTransactionResult;
import io.transatron.transaction.manager.web3.model.TransactionType;
import io.transatron.transaction.manager.web3.utils.ThreadUtils;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.stereotype.Component;
import org.tron.trident.core.ApiWrapper;
import org.tron.trident.core.exceptions.IllegalException;
import org.tron.trident.core.key.KeyPair;
import org.tron.trident.core.utils.Sha256Hash;
import org.tron.trident.proto.Chain;
import org.tron.trident.proto.Response;

import static io.transatron.transaction.manager.web3.model.TransactionType.BANDWIDTH;
import static io.transatron.transaction.manager.web3.model.TransactionType.ENERGY;
import static io.transatron.transaction.manager.web3.model.TransactionType.UNBANDWIDTH;
import static io.transatron.transaction.manager.web3.model.TransactionType.UNENERGY;
import static org.apache.commons.lang3.StringUtils.EMPTY;

@Slf4j
@RequiredArgsConstructor
@Component
public class TronTransactionHandler {

    private static final int ENERGY_RESOURCE_CODE = 1;
    private static final int BANDWIDTH_RESOURCE_CODE = 0;

    private static final int SUCCESS_CODE = 0;

    private final ApiWrapper apiWrapper;

    public SendTransactionResult delegateEnergy(final String owner, final String receiver, final long amount, final int keyPermissionID, final String keyPK) {
        return sendTransaction(ENERGY, owner, receiver, amount, keyPermissionID, keyPK);
    }

    public SendTransactionResult undelegateEnergy(final String owner, final String receiver, final long amount, final int keyPermissionID, final String keyPK) {
        return sendTransaction(UNENERGY, owner, receiver, amount, keyPermissionID, keyPK);
    }

    public SendTransactionResult delegateBandwidth(final String owner, final String receiver, final long amount, final int keyPermissionID, final String keyPK) {
        return sendTransaction(BANDWIDTH, owner, receiver, amount, keyPermissionID, keyPK);
    }

    public SendTransactionResult undelegateBandwidth(final String owner, final String receiver, final long amount, final int keyPermissionID, final String keyPK) {
        return sendTransaction(UNBANDWIDTH, owner, receiver, amount, keyPermissionID, keyPK);
    }

    public SendTransactionResult sendTransaction(final TransactionType type,
                                                 final String owner,
                                                 final String receiver,
                                                 final long amount,
                                                 final int keyPermissionID,
                                                 final String keyPK) {
        try {
            final var txExtension = getTransactionExtension(type, owner, receiver, amount);
            final var txWithPermission = setPermissionIDIfNecessary(txExtension, keyPermissionID);

            final var resourceAPIKey = new KeyPair(keyPK);
            final var signedTxn = apiWrapper.signTransaction(txWithPermission, resourceAPIKey);

            final var sendTransactionResult = broadcastTransaction(signedTxn);

            //CONTRACT_VALIDATE_ERROR, account not activated (yet...) => wait and retry
            if (!shouldRetryBroadcastTransaction(sendTransactionResult)) {
                return sendTransactionResult;
            }
            // TODO: do we really need this sleep?
            final var sleepMillis = 500;
            log.info("Account isn't activated. Sleeping for {}ms and retrying.", sleepMillis);
            ThreadUtils.sleepQuietly(sleepMillis);
            return broadcastTransaction(signedTxn);
        } catch (final Exception ex) {
            final var errorMessage = "Error sending resource transaction, hash: null message:" + ex.getMessage();
            log.error(errorMessage);
            return new SendTransactionResult(null, -1, errorMessage);
        }
    }

    private Response.TransactionExtention getTransactionExtension(final TransactionType type,
                                                                  final String owner,
                                                                  final String receiver,
                                                                  final long amount) throws IllegalException {
        return switch (type) {
            case ENERGY      -> apiWrapper.delegateResourceV2(owner, amount, ENERGY_RESOURCE_CODE, receiver, false, 0);
            case BANDWIDTH   -> apiWrapper.delegateResourceV2(owner, amount, BANDWIDTH_RESOURCE_CODE, receiver, false, 0);
            case UNENERGY    -> apiWrapper.undelegateResource(owner, amount, ENERGY_RESOURCE_CODE, receiver);
            case UNBANDWIDTH -> apiWrapper.undelegateResource(owner, amount, BANDWIDTH_RESOURCE_CODE, receiver);
        };
    }

    private Response.TransactionExtention setPermissionIDIfNecessary(final Response.TransactionExtention txExtension,
                                                                     final int permissionID) {
        if (permissionID <= 0) {
            return txExtension;
        }
        return setPermissionID(txExtension, permissionID);
    }

    private Response.TransactionExtention setPermissionID(final Response.TransactionExtention txExtension,
                                                          final int permissionID) {
        final var contract = txExtension.getTransaction()
                                               .getRawData()
                                               .getContract(0)
                                               .toBuilder()
                                               .setPermissionId(permissionID)
                                               .build();

        final var rawData = txExtension.getTransaction()
                                       .getRawData()
                                       .toBuilder()
                                       .setContract(0, contract)
                                       .build();

        final var transaction = txExtension.getTransaction()
                                           .toBuilder()
                                           .setRawData(rawData)
                                           .build();
        final var transactionId = Sha256Hash.hash(true, transaction.getRawData().toByteArray());

        return txExtension.toBuilder()
                          .setTransaction(transaction)
                          .setTxid(ByteString.copyFrom(transactionId))
                          .build();
    }

    @SneakyThrows
    private SendTransactionResult broadcastTransaction(final Chain.Transaction txn) {
        final var txId = apiWrapper.calculateTransactionHash(txn);
        final var hash = ByteString.copyFrom(Hex.encode(txId)).toStringUtf8();

        final var txReturn = apiWrapper.blockingStub.broadcastTransaction(txn);

        if (txReturn.getResult()) {
            return new SendTransactionResult(hash, SUCCESS_CODE, EMPTY);
        }

        final var codeValue = txReturn.getCodeValue();
        final var message = resolveResultCode(codeValue) + ", " + txReturn.getMessage().toStringUtf8();
        log.error("Error sending resource transaction [hash={}, code value={}, message={}]", hash, codeValue, message);

        return new SendTransactionResult(hash, codeValue, message);
    }

    private String resolveResultCode(final int code) {
        return switch (code) {
            case 0 -> "SUCCESS";
            case 1 -> "SIGERROR";
            case 2 -> "CONTRACT_VALIDATE_ERROR";
            case 3 -> "CONTRACT_EXE_ERROR";
            case 4 -> "BANDWITH_ERROR";
            case 5 -> "DUP_TRANSACTION_ERROR";
            case 6 -> "TAPOS_ERROR";
            case 7 -> "TOO_BIG_TRANSACTION_ERROR";
            case 8 -> "TRANSACTION_EXPIRATION_ERROR";
            case 9 -> "SERVER_BUSY";
            case 10 -> "NO_CONNECTION";
            case 11 -> "NOT_ENOUGH_EFFECTIVE_CONNECTION";
            case 20 -> "OTHER_ERROR";
            default -> EMPTY;
        };
    }

    private boolean shouldRetryBroadcastTransaction(final SendTransactionResult result) {
        return result.exitCode() == 2
            && result.errorMessage().contains("Contract validate error")
            && result.errorMessage().contains("not exist");
    }

}
