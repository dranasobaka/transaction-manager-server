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
import java.math.BigInteger;
import java.util.Arrays;

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

        var txBuilder = Transaction.builder()
                                   .id(txHash);

        if (tx.getRawData().getContractCount() <= 0) {
            return txBuilder;
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

            if (contractName.equals("TriggerSmartContract")) {
                parseSmartContractData(contract, txBuilder);
            } else {
                Method getToAddressMethod = contractClass.getMethod("getToAddress");
                var toAddress = (ByteString) getToAddressMethod.invoke(contractObject);
                var txReceiverAddress = TronAddressUtils.tronHexToBase58(toAddress);

                Method getAmountAddressMethod = contractClass.getMethod("getAmount");
                var amount = (long) getAmountAddressMethod.invoke(contractObject);

                txBuilder.to(txReceiverAddress)
                         .amount(amount);
            }

            var estimatedBandwidth = estimateBandwidth(tx);
            var estimatedEnergy = tx.getRawData().getFeeLimit() / DEFAULT_ENERGY_FEE;

            return txBuilder.from(txSenderAddress)
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

    private void parseSmartContractData(Chain.Transaction.ContractOrBuilder contract, Transaction.TransactionBuilder txBuilder) {
        try {
            var contractData = contract.getParameter().unpack(Contract.TriggerSmartContract.class);
            var contractDataHex = contractData.getData().toByteArray();

            var address = Arrays.copyOfRange(contractDataHex, 16, 36);
            var amount = Arrays.copyOfRange(contractDataHex, 36, 68);
            var transactionToAddress = TronAddressUtils.toBase58(address);
            var txAmount = new BigInteger(amount).longValue();

            txBuilder.to(transactionToAddress)
                     .amount(txAmount);
        } catch (Exception ex) {
            log.error("Unable to parse data in TriggerSmartContract", ex);
        }
    }

}
