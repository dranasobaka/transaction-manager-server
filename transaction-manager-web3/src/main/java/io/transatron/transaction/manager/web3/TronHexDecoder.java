package io.transatron.transaction.manager.web3;

import com.google.protobuf.ByteString;
import io.transatron.transaction.manager.domain.Transaction;
import io.transatron.transaction.manager.domain.exception.BadRequestException;
import io.transatron.transaction.manager.web3.utils.TronAddressUtils;
import io.transatron.transaction.manager.web3.utils.TronTransactionUtils;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.DecoderException;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.stereotype.Component;
import org.tron.trident.proto.Chain;
import org.tron.trident.proto.Contract;

import java.lang.reflect.Method;

import static io.transatron.transaction.manager.domain.exception.ErrorsTable.CORRUPTED_PAYLOAD;

@Slf4j
@Component
public class TronHexDecoder {

    private static final long DEFAULT_ENERGY_FEE = 420;

    public Transaction decodeTransaction(String rawTransaction) {
        var tronTxBuilder = transformHex(rawTransaction);

        return tronTxBuilder.rawTransaction(rawTransaction)
                            .build();
    }

    private Transaction.TransactionBuilder transformHex(String transactionHex) {
        var transactionBytes = decodeHex(transactionHex);
        var tx = parseTransactionObject(transactionBytes);
        var txHash = TronTransactionUtils.getTransactionHash(tx);

        if (tx.getRawData().getContractCount() <= 0) {
            return Transaction.builder()
                              .id(txHash);
        }

        var contract = tx.getRawData().getContract(0);
        try {
            var lastDotIndex = contract.getParameter()
                                       .getTypeUrl()
                                       .lastIndexOf(".");
            var contractName = contract.getParameter()
                                       .getTypeUrl()
                                       .substring(lastDotIndex + 1);
            var fullClassName = "org.tron.trident.proto.Contract$" + contractName;
            Class contractClass = Class.forName(fullClassName, true, Contract.class.getClassLoader());

            Object contractObject = contract.getParameter().unpack(contractClass);

            Method getOwnerAddressMethod = contractClass.getMethod("getOwnerAddress");
            var ownerAddress = (ByteString) getOwnerAddressMethod.invoke(contractObject);
            var txSenderAddress = TronAddressUtils.tronHexToBase58(ownerAddress);

            var receiverMethodName = contractName.equals("TriggerSmartContract") ? "getContractAddress" : "getToAddress";
            Method getToAddressMethod = contractClass.getMethod(receiverMethodName);
            var toAddress = (ByteString) getToAddressMethod.invoke(contractObject);
            var txReceiverAddress = TronAddressUtils.tronHexToBase58(toAddress);

            var amountMethodName = contractName.equals("TriggerSmartContract") ? "getCallTokenValue" : "getAmount";
            Method getAmountAddressMethod = contractClass.getMethod(amountMethodName);
            var amount = (long) getAmountAddressMethod.invoke(contractObject);

            var estimatedBandwidth = estimateBandwidth(tx);
            var estimatedEnergy = tx.getRawData().getFeeLimit() / DEFAULT_ENERGY_FEE;

            return Transaction.builder()
                              .id(txHash)
                              .from(txSenderAddress)
                              .to(txReceiverAddress)
                              .amount(amount)
                              .estimatedBandwidth(estimatedBandwidth)
                              .estimatedEnergy(estimatedEnergy);
        } catch (Exception ex) {
            log.error("Error parsing transaction", ex);
            throw new BadRequestException("Unable to decode transaction payload", CORRUPTED_PAYLOAD);
        }
    }

    private byte[] decodeHex(String transactionHex) {
        try {
            return Hex.decode(transactionHex);
        } catch (DecoderException ex) {
            throw new BadRequestException("Unable to decode transaction payload", CORRUPTED_PAYLOAD);
        }
    }

    private Chain.Transaction parseTransactionObject(byte[] transactionBytes) {
        try {
            return Chain.Transaction.parseFrom(transactionBytes);
        } catch (Exception ex) {
            log.error("Error parsing transaction ", ex);
            throw new BadRequestException("Unable to decode transaction payload", CORRUPTED_PAYLOAD);
        }
    }

    /**
     * Taken from ApiWrapper
     */
    private long estimateBandwidth(Chain.Transaction txn) {
        return txn.toBuilder().clearRet().build().getSerializedSize() + 64;
    }

}
