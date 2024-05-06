package io.transatron.transaction.manager.web3;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.tron.trident.abi.TypeReference;
import org.tron.trident.abi.datatypes.Function;
import org.tron.trident.abi.datatypes.generated.Uint256;
import org.tron.trident.core.ApiWrapper;

import java.math.BigInteger;
import java.util.List;

@Slf4j
@Component
public class TRXDexManager {

    private final ApiWrapper tronApiWrapper;

    private final String sunSwapUSDTTRXPairAddress = "TQn9Y2khEsLJW1ChVWFMSMeRDow5KcbLSE"; //SunSwap USDT-TRX pair address

    private final String dummyAddress = "TYyziUUcWChpsy3JYRc7pVCt4ZCsGNTh8x"; //dummy address for testing

    private double cachedPriceTRX;

    private long cachedPriceTimestamp;

    final long cacheTimeout = 60 * 1000; // 1 minute

    public TRXDexManager(ApiWrapper tronApiWrapper) {
        this.tronApiWrapper = tronApiWrapper;
    }

    public double getTrxPrice() {
        long now = System.currentTimeMillis();
        if (cachedPriceTRX > 0 && (now - cachedPriceTimestamp) < cacheTimeout) {
            return cachedPriceTRX;
        } else {
            var trxAmount = 100000000L;   //  100 TRX
            getTokenToTrxOutputPrice(trxAmount);    //  cache updated within this call
            return cachedPriceTRX;
        }
    }

    /**
     * returns USDT amount required to buy TRXAmount
     *
     * @param TRXAmount
     * @return
     */
    public long getTokenToTrxOutputPrice(long TRXAmount) {
        var usdtAmount = BigInteger.ZERO;
        var trxAmountU = new Uint256(BigInteger.valueOf(TRXAmount));
        // function getTokenToTrxOutputPrice(uint256 trx_bought) public view returns (uint256)
        var getTokenToTrxOutputPrice = new Function("getTokenToTrxOutputPrice",
                    List.of(trxAmountU),
                    List.of(new TypeReference<Uint256>() {})
                );
        try {
            var txnExtension = tronApiWrapper.constantCall(dummyAddress, sunSwapUSDTTRXPairAddress, getTokenToTrxOutputPrice);
            var result = txnExtension.getConstantResult(0);
            usdtAmount = new BigInteger(result.toByteArray());

            // update cache
            cachedPriceTRX = usdtAmount.doubleValue() / TRXAmount;
            cachedPriceTimestamp = System.currentTimeMillis();

            log.info("Amount required for {} TRX is {} USDT", TRXAmount, usdtAmount.longValue());
        } catch (Exception ex) {
            log.error("Error getting TRX price", ex);
        }
        return usdtAmount.longValue();
    }

}
