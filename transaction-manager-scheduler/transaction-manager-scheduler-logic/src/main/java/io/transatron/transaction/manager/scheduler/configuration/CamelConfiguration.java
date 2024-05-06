package io.transatron.transaction.manager.scheduler.configuration;

import io.transatron.transaction.manager.repository.OrderRepository;
import io.transatron.transaction.manager.repository.ResourceAddressRepository;
import io.transatron.transaction.manager.scheduler.configuration.properties.SedaConfigurationProperties;
import io.transatron.transaction.manager.scheduler.configuration.properties.WalletsProperties;
import io.transatron.transaction.manager.scheduler.processor.CreateTronEnergyOrderProcessor;
import io.transatron.transaction.manager.scheduler.processor.FulfillTransactionsProcessor;
import io.transatron.transaction.manager.tronenergy.TronEnergyManager;
import io.transatron.transaction.manager.web3.ResourceProviderService;
import io.transatron.transaction.manager.web3.TronTransactionHandler;
import io.transatron.transaction.manager.web3.api.TronHttpApiFeignClient;
import io.transatron.transaction.manager.web3.configuration.properties.TronProperties;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class CamelConfiguration {

    public static final String CREATE_TRON_ENERGY_ORDER_SEDA_NAME = "create_tron_energy_order_seda";
    public static final String FULFILL_ORDER_SEDA_NAME = "fulfill_order_seda";

    public static final String SEDA_CREATE_TRON_ENERGY_ORDER_ROUTE_ID = "SedaCreateTronEnergyOrderRouteId";
    public static final String SEDA_FULFILL_ORDER_ROUTE_ID = "SedaFulfillOrderRouteId";

    @Bean
    @ConfigurationProperties("transaction-manager.create-tron-energy-order.seda")
    public SedaConfigurationProperties createTronEnergyOrderSedaProperties() {
        return sedaConfiguration(CREATE_TRON_ENERGY_ORDER_SEDA_NAME);
    }

    @Bean
    @ConfigurationProperties("transaction-manager.fulfill-order.seda")
    public SedaConfigurationProperties fulfillOrderSedaProperties() {
        return sedaConfiguration(FULFILL_ORDER_SEDA_NAME);
    }

    @Bean
    public CreateTronEnergyOrderProcessor createTronEnergyOrderProcessor(TronEnergyManager tronEnergyManager) {
        return new CreateTronEnergyOrderProcessor(tronEnergyManager);
    }

    @Bean
    public FulfillTransactionsProcessor fulfillTransactionsProcessor(OrderRepository orderRepository,
                                                                     ResourceAddressRepository resourceAddressRepository,
                                                                     ResourceProviderService resourceProviderService,
                                                                     TronHttpApiFeignClient tronHttpApiClient,
                                                                     TronTransactionHandler tronTransactionHandler,
                                                                     WalletsProperties walletsProperties,
                                                                     TronProperties tronProperties) {
        return new FulfillTransactionsProcessor(orderRepository,
                                                resourceAddressRepository,
                                                resourceProviderService,
                                                tronHttpApiClient,
                                                tronTransactionHandler,
                                                walletsProperties,
                                                tronProperties);
    }

    @Bean
    public RoutesBuilder processorRoutesBuilder(SedaConfigurationProperties createTronEnergyOrderSedaProperties,
                                                SedaConfigurationProperties fulfillOrderSedaProperties,
                                                CreateTronEnergyOrderProcessor createTronEnergyOrderProcessor,
                                                FulfillTransactionsProcessor fulfillTransactionsProcessor) {
        return new RouteBuilder() {
            @Override
            public void configure() {
                // process
                from(createTronEnergyOrderSedaProperties.getUri())
                    .process(createTronEnergyOrderProcessor)
                    .routeId(SEDA_CREATE_TRON_ENERGY_ORDER_ROUTE_ID);
                
                from(fulfillOrderSedaProperties.getUri())
                    .process(fulfillTransactionsProcessor)
                    .routeId(SEDA_FULFILL_ORDER_ROUTE_ID);
            }
        };
    }

    private SedaConfigurationProperties sedaConfiguration(String name) {
        var sedaConfiguration = new SedaConfigurationProperties();
        sedaConfiguration.setName(name);
        return sedaConfiguration;
    }

}
