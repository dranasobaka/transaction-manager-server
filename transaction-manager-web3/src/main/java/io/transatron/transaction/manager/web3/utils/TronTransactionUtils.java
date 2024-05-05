package io.transatron.transaction.manager.web3.utils;

import com.google.protobuf.ByteString;
import lombok.experimental.UtilityClass;
import org.bouncycastle.util.encoders.Hex;
import org.tron.trident.proto.Chain;

import static org.tron.trident.core.ApiWrapper.calculateTransactionHash;

@UtilityClass
public class TronTransactionUtils {

    public static String getTransactionHash(Chain.Transaction tx) {
        var txHash = calculateTransactionHash(tx);
        var encodedTxHash = Hex.encode(txHash);

        return ByteString.copyFrom(encodedTxHash)
                         .toStringUtf8();
    }

}
