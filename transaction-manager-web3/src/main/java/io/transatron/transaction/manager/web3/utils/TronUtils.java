package io.transatron.transaction.manager.web3.utils;

import com.google.protobuf.ByteString;
import io.transatron.transaction.manager.web3.model.ContractType;
import lombok.experimental.UtilityClass;
import org.tron.trident.proto.Response;

import java.util.ArrayList;
import java.util.List;

import static io.transatron.transaction.manager.web3.model.ContractType.DelegateResourceContract;
import static io.transatron.transaction.manager.web3.model.ContractType.UnDelegateResourceContract;

@UtilityClass
public class TronUtils {

    public static int isResourceAccountSetupValid(final Response.Account resourceAccount, final String resourceManagerAddress) {
        for (int i = 0; i < resourceAccount.getActivePermissionCount(); i++) {
            final var permission = resourceAccount.getActivePermission(i);
            if (permission.getKeysCount() > 1) {
                continue;
            }

            final var key = permission.getKeys(0);
            final var keyBase58 = TronAddressUtils.tronHexToBase58(key.getAddress());
            if (!resourceManagerAddress.equals(keyBase58)) {
                continue;
            }

            final var allowedOperations = operationsDecoder(permission.getOperations());
            if (allowedOperations.contains(DelegateResourceContract) && allowedOperations.contains(UnDelegateResourceContract)) {
                return permission.getId();
            }
        }

        return -1;
    }

    // Description: get all allowable contract types according to the operations code
    public static List<ContractType> operationsDecoder(final ByteString operations) {
        final var contractIDs = new ArrayList<ContractType>();
        for (int byteIdx = 0; byteIdx < 32; byteIdx++) {          // 32 bytes
            final var opByte = operations.byteAt(byteIdx);
            for (int j = 0; j < 8; j++) {
                if ((opByte >> j & 0x1) == 1) {
                    contractIDs.add(ContractType.getContractTypeByNum(byteIdx * 8 + j));
                }
            }
        }
        return contractIDs;
    }

}
