package io.transatron.transaction.manager.tronenergy;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.transatron.transaction.manager.tronenergy.api.TronEnergyFeignClient;
import io.transatron.transaction.manager.tronenergy.api.dto.CreateOrderRequest;
import io.transatron.transaction.manager.tronenergy.configuration.properties.TronEnergyProperties;
import io.transatron.transaction.manager.tronenergy.model.EstimationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

import static io.transatron.transaction.manager.web3.TronConstants.TRX_DECIMALS;

@Slf4j
@RequiredArgsConstructor
@Component
public class TronEnergyManager {

    public static final long ENERGY_RENT_PERIOD = 3600;

    private static final double multiplier = 1.1;

    private final TronEnergyProperties properties;

    private final TronEnergyFeignClient tronEnergyClient;

    private long currentEnergyPrice;

    private long currentBandwidthPrice;

    private long currentEnergyMaxPrice;

    private long currentBandwidthMaxPrice;

    private Double currentlyAvailableEnergyAmount;

    private Double currentlyAvailableBandwidthAmount;

    private long currentCreditBalanceTRX;

    private String serverAddress;

    public void tronEnergyUpdateInfo() {
        try {
            var getInfoResponse = tronEnergyClient.getInfo();

            serverAddress = getInfoResponse.getAddress();
            var market = getInfoResponse.getMarket();
            currentlyAvailableEnergyAmount = market.getAvailableEnergy();
            currentlyAvailableBandwidthAmount = market.getAvailableBandwidth();

            var price = getInfoResponse.getPrice();

            for (var openBandwidthPrice : price.getOpenBandwidth()) {
                long minDuration = openBandwidthPrice.getMinDuration();
                if (minDuration == 86400) {
                    currentBandwidthPrice = openBandwidthPrice.getSuggestedPrice();
                }
            }

            for (var fastEnergyPrice : price.getFastEnergy()) {
                long minDuration = fastEnergyPrice.getMinDuration();
                if (minDuration == 86400) {
                    var suggestedPrice = fastEnergyPrice.getSuggestedPrice();
                    currentEnergyMaxPrice = (long) (suggestedPrice * multiplier);
                }
                if (minDuration == 3600) {
                    currentEnergyPrice = fastEnergyPrice.getSuggestedPrice(); // <== TODO: USING FAST ENERGY PRICE FOR 1 HOUR
                }
            }
            if (currentEnergyMaxPrice <= currentEnergyPrice) {
                currentEnergyMaxPrice = (long) (currentEnergyPrice * 1.2);
            }

            for (var fastBandwidthPrice : price.getFastBandwidth()) {
                long minDuration = fastBandwidthPrice.getMinDuration();
                if (minDuration == 86400) {
                    currentBandwidthMaxPrice = fastBandwidthPrice.getSuggestedPrice();
                }
                if (minDuration == 3600) {
                    currentBandwidthPrice = fastBandwidthPrice.getSuggestedPrice(); // <== TODO: USING FAST BANDWIDTH PRICE FOR 1 HOUR
                }
            }
            if (currentBandwidthMaxPrice <= currentBandwidthPrice) {
                currentBandwidthMaxPrice = (long) (currentBandwidthPrice * 1.2);
            }
        } catch (Exception ex) {
            log.error("Error loading info", ex);
        }
        // update balance
        getCurrentCreditBalanceTRX();
    }

    public long getCurrentCreditBalanceTRX() {
        try {
            var response = tronEnergyClient.getCredit(properties.getMarketWalletAddress());
            currentCreditBalanceTRX = response.getValue();
        } catch (Exception ex) {
            log.error("Error getting energy balance", ex);
        }
        return currentCreditBalanceTRX;
    }

    private EstimationResult estimateResources(long energyRequired, long bandwidthRequired, boolean useMaxPrices) {
        long bandwidthPriceMarket;
        long energyPriceMarket;

        long energyPriceBundle = getPriceForEnergyBundle(energyRequired);
        log.info("Estimate: Energy prices: current: " + currentEnergyPrice + " max: " + currentEnergyMaxPrice + " bundle :" + energyPriceBundle);

        if (useMaxPrices) {
            bandwidthPriceMarket = currentBandwidthMaxPrice;    //SUN
            energyPriceMarket = Math.max(currentEnergyMaxPrice, energyPriceBundle); //SUN
        } else {
            bandwidthPriceMarket = currentBandwidthPrice;   //SUN
            energyPriceMarket = Math.max(currentEnergyPrice, energyPriceBundle);    //SUN
        }

        var result = new EstimationResult();
        // acquire bandwidth

        long bandwidthTRXBurned = (long) (bandwidthRequired * 0.001 * TRX_DECIMALS);
        long bandwidthCostTRXDelegated = (long) (bandwidthRequired * bandwidthPriceMarket);
        if (bandwidthRequired > 0) {
            if (bandwidthTRXBurned < bandwidthCostTRXDelegated) {
                // burn TRX for Bandwidth
                result.trxToSend += bandwidthTRXBurned;
                result.totalEstimatedResourcesCostTRX += bandwidthTRXBurned;
            } else {
                //TODO: Delegate bandwidth is not implemented yet
                result.trxToSend += bandwidthTRXBurned;
                result.totalEstimatedResourcesCostTRX += bandwidthTRXBurned;
                log.error("Estimate: Delegating bandwidth is not implemented yet. cost Delegated: {} cost Burned: {}", bandwidthCostTRXDelegated, bandwidthTRXBurned);
            }
        }

        // acquire energy
        long energyCostTRXDelegated = (long) (energyRequired * energyPriceMarket);  // with decimals
        long energyCostTRXBurned = (long) (energyRequired * 420);   // with decimals
        // send energy order
        if (energyRequired > 0) {
            if (energyCostTRXBurned < energyCostTRXDelegated) {
                // burn TRX for Energy
                result.trxToSend += energyCostTRXBurned;
                result.totalEstimatedResourcesCostTRX += energyCostTRXBurned;
            } else {
                // delegate energy
                result.energyToDelegate = energyRequired;
                result.energyToDelegateTRXPrice = energyCostTRXDelegated;
                result.totalEstimatedResourcesCostTRX += energyCostTRXDelegated;
            }
        }
        log.info("Estimate: USDT for {} energy and {} bandwidth @ prices: energy: {} bandwidth: {}", energyRequired, bandwidthRequired, energyPriceMarket, bandwidthPriceMarket);
        return result;
    }

    /**
     * estimating resources for buying on external market for user
     *
     * @param energyRequired
     * @param bandwidthRequired
     * @return
     */
    public EstimationResult estimateResourcesForFillingOrder(long energyRequired, long bandwidthRequired) {
        return estimateResources(energyRequired, bandwidthRequired, false);
    }

    /**
     * estimation of resources cost in TRX for selling to user
     *
     * @param energyRequired
     * @param bandwidthRequired
     * @return
     */
    public long estimateResourcesCostInTRX(long energyRequired, long bandwidthRequired) {
        var result = estimateResources(energyRequired, bandwidthRequired, true);
        return result.totalEstimatedResourcesCostTRX;
    }

    public long newEnergyOrder(String targetWallet, long energy) {
        var orderID = -1L;
        try {
            var energyPriceBundle = getPriceForEnergyBundle(energy);
            log.info("Energy prices: current: {}, max: {}, bundle : {}", currentEnergyPrice, currentEnergyMaxPrice, energyPriceBundle);
            var energyPrice = currentEnergyPrice;

            var duration = ENERGY_RENT_PERIOD;
            var payment = energyPrice * energy * (duration + (duration < 86400 ? 86400 : 0)) / 86400;

            if (payment > currentCreditBalanceTRX) {
                log.info("payment = {}, currentCreditBalanceTRX = {}", payment, currentCreditBalanceTRX);
                log.error("Not enough credit balance to create new order");
                return -1;
            }

            var request = CreateOrderRequest.builder()
                    .market("Open")
                    .address(properties.getMarketWalletAddress())
                    .target(targetWallet)
                    .payment(payment)
                    .price(energyPrice)
                    .resource(0)        // 0 energy, 1 bandwidth
                    .duration(duration)
                    .partfill(Boolean.TRUE)
                    .bulk(Boolean.FALSE)
                    .apiKey(properties.getApiKey())
                    .build();

            var response = tronEnergyClient.createNewOrder(request);

            try {
                var responseMap = new ObjectMapper().readValue(response, Map.class);
                if (responseMap.containsKey("order")) {
                    orderID = (Long) responseMap.get("order");
                    log.info("Energy orderID: {} created for {}, energy: {}, price: {}, duration: {}", orderID, targetWallet, energy, energyPrice, duration);
                } else {
                    log.error("Error: {}", response);
                }
            } catch (Exception ex) {
                log.error("Error: {}", response);
            }
        } catch (Exception ex) {
            log.error("Error creating new order", ex);
        }
        return orderID;
    }

    private long getPriceForEnergyBundle(long energyAmount) {
        try {
            var response = tronEnergyClient.getInfo();

            var market = response.getMarket();
            currentlyAvailableEnergyAmount = market.getAvailableEnergy();
            currentlyAvailableBandwidthAmount = market.getAvailableBandwidth();

            // long energy required
            var energyStillRequired = energyAmount;
            var maxPriceEnergy = 0L;

            var availableEnergyByPrice = market.getAvailableEnergyByPrice();
            for (var energyPrice : availableEnergyByPrice) {
                var price = energyPrice.getPrice();
                var value = energyPrice.getValue();

                if (value >= energyStillRequired) {
                    maxPriceEnergy = price;
                    break;
                } else {
                    energyStillRequired -= value;
                }
            }

            var currentEnergyPrice = 0L;
            var price = response.getPrice();
            var openEnergy = price.getOpenEnergy();
            for (var openEnergyPrice : openEnergy) {
                long minDuration = openEnergyPrice.getMinDuration();
                if (minDuration == 86400) {
                    currentEnergyPrice = openEnergyPrice.getSuggestedPrice();
                }
            }

            log.info("Energy price for bundle {} suggestedPrice: {} / bundle level: {}", energyAmount, currentEnergyPrice, maxPriceEnergy);
            return Math.max(maxPriceEnergy, currentEnergyPrice);
        } catch (Exception ex) {
            log.error("Error loading info", ex);
        }
        return -1;
    }

    public Double getCurrentlyAvailableEnergyAmount() {
        return currentlyAvailableEnergyAmount;
    }

    public Double getCurrentlyAvailableBandwidthAmount() {
        return currentlyAvailableBandwidthAmount;
    }

    public String getServerAddress() {
        return serverAddress;
    }

}
