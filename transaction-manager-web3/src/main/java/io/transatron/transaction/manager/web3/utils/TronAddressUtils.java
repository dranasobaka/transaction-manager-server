package io.transatron.transaction.manager.web3.utils;

import com.google.protobuf.ByteString;
import lombok.experimental.UtilityClass;
import org.bouncycastle.util.encoders.Hex;
import org.tron.trident.utils.Base58Check;

import java.util.Arrays;

@UtilityClass
public class TronAddressUtils {

    public static byte[] toHex(String base58Address) {
        var decodedAddress = Base58Check.base58ToBytes(base58Address);
        return Arrays.copyOfRange(decodedAddress, 1, 21);
    }

    public static String toBase58(byte[] address) {
        var addressWithPrefix = new byte[21];
        addressWithPrefix[0] = 0x41;
        System.arraycopy(address, 0, addressWithPrefix, 1, 20);

        return Base58Check.bytesToBase58(addressWithPrefix);
    }

    public static String tronHexToBase58(String address41) {
        var data = Hex.decode(address41);
        if (data.length != 21) {
            throw new IllegalArgumentException("Address length must be 21 bytes");
        }
        if (data[0] != 65) {
            throw new IllegalArgumentException("Hex address header does not match Tron header");
        }

        var keyBytes = new byte[20];
        //truncate 1 byte header "0x41"
        System.arraycopy(data, 1, keyBytes, 0, 20);

        return toBase58(keyBytes);
    }

    public static String tronHexToBase58(ByteString address) {
        if (address.toByteArray().length != 21) {
            throw new IllegalArgumentException("Address length must be 21 bytes");
        }
        if (address.toByteArray()[0] != 65) {
            throw new IllegalArgumentException("Hex address header does not match Tron header");
        }

        var keyBytes = new byte[20];
        //truncate 1 byte header "0x41"
        System.arraycopy(address.toByteArray(), 1, keyBytes, 0, 20);

        return toBase58(keyBytes);
    }

}
