package io.transatron.transaction.manager.web3;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.transatron.transaction.manager.domain.Transaction;
import io.transatron.transaction.manager.domain.exception.BadRequestException;
import io.transatron.transaction.manager.web3.utils.TronAddressUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static io.transatron.transaction.manager.domain.exception.ErrorsTable.CORRUPTED_PAYLOAD;
import static org.apache.commons.lang3.ObjectUtils.isEmpty;

@Slf4j
@Component
public class TronTransactionDecoder {

    private final ObjectMapper objectMapper;

    public TronTransactionDecoder() {
        this.objectMapper = new ObjectMapper();
    }

    public Transaction decodeTransaction(String rawTransaction) {
        var rootNode = readTreeOrThrowException(rawTransaction);

        var tronTxBuilder = transform(rootNode);

        return tronTxBuilder.rawTransaction(rawTransaction)
                            .build();
    }

    private JsonNode readTreeOrThrowException(String rawTransaction) {
        try {
            return objectMapper.readTree(rawTransaction);
        } catch (JsonProcessingException ex) {
            throw new BadRequestException("Unable to decode transaction payload", CORRUPTED_PAYLOAD);
        }
    }

    private Transaction.TransactionBuilder transform(JsonNode rootNode) {
        if (!rootNode.has("txID") || !rootNode.has("raw_data")) {
            throw new BadRequestException("Unable to decode transaction payload", CORRUPTED_PAYLOAD);
        }
        var txID = rootNode.get("txID").asText();
        var tronTxBuilder = Transaction.builder()
                                       .id(txID);

        var rawDataNode = rootNode.get("raw_data");

        if (!rawDataNode.isObject() || !rawDataNode.has("contract")) {
            throw new BadRequestException("Unable to decode transaction payload", CORRUPTED_PAYLOAD);
        }
        var contractNodes = rawDataNode.get("contract");

        if (!contractNodes.isArray()) {
            throw new BadRequestException("Unable to decode transaction payload", CORRUPTED_PAYLOAD);
        }

        for (var contractNode : contractNodes) {
            if (!contractNode.isObject()) {
                throw new BadRequestException("Unable to decode transaction payload", CORRUPTED_PAYLOAD);
            }
            var parameterNode = contractNode.get("parameter");
            if (parameterNode.isNull()) {
                continue;
            }

            var valueNode = parameterNode.get("value");
            if (valueNode.isNull()) {
                continue;
            }

            var ownerAddressNode = valueNode.get("owner_address");
            var toAddressNode = valueNode.get("to_address");
            var amountNode = valueNode.get("amount");
            if (ownerAddressNode.isNull() || isEmpty(ownerAddressNode.asText())
                    || toAddressNode.isNull() || isEmpty(toAddressNode.asText())
                    || toAddressNode.isNull() || isEmpty(toAddressNode.asLong())) {
                continue;
            }

            var ownerAddress = decodeAddress(ownerAddressNode.asText());
            var toAddress = decodeAddress(toAddressNode.asText());
            var amount = amountNode.asLong();

            tronTxBuilder.from(ownerAddress)
                         .to(toAddress)
                         .amount(amount);
            break;
        }

        var estimatedBandwidth = estimateBandwidth(rootNode);
        var estimatedEnergy = estimateEnergy(rawDataNode);

        log.debug("estimatedBandwidth: {}", estimatedBandwidth);
        log.debug("estimatedEnergy: {}", estimatedEnergy);

        return tronTxBuilder.estimatedEnergy(estimatedEnergy)
                            .estimatedBandwidth(estimatedBandwidth);
    }

    private long estimateBandwidth(JsonNode rootNode) {
        if (!rootNode.has("signature") || !rootNode.get("signature").isArray()) {
            return 0;
        }

        var signatureSizeInBytes = 0;
        for (var signatureNode : rootNode.get("signature")) {
            signatureSizeInBytes += (signatureNode.asText().length() / 2);
        }

        if (!rootNode.has("raw_data_hex")) {
            return signatureSizeInBytes;
        }

        final var rawDataHex = rootNode.get("raw_data_hex").asText();

        return rawDataHex.length() / 2   //raw data size in bytes
            + signatureSizeInBytes       //signature size in bytes
            + 64                         // don't know what exactly is this. taken from ApiWrapper.estimateBandwidth
            + 5;                         // my assumption...
    }

    private long estimateEnergy(JsonNode rawDataNode) {
        if (!rawDataNode.has("fee_limit") || rawDataNode.get("fee_limit").isLong()) {
            return 0;
        }
        return rawDataNode.get("fee_limit").asLong() / 420;
    }

    private String decodeAddress(String address) {
        if (address.startsWith("T")) {
            return address;
        }
        if (address.startsWith("41")) {
            return TronAddressUtils.tronHexToBase58(address);
        }
        throw new BadRequestException("Unable to decode address in transaction: " + address, CORRUPTED_PAYLOAD);
    }

}
