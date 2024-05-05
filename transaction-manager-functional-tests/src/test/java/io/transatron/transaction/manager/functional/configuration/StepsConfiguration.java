package io.transatron.transaction.manager.functional.configuration;

import io.transatron.transation.manager.client.TronTransactionManagerClient;
import io.transatron.transaction.manager.functional.steps.OpenApiSteps;
import io.transatron.transaction.manager.functional.steps.ServiceSteps;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@TestConfiguration
public class StepsConfiguration {

    @Bean
    public ServiceSteps serviceSteps(TronTransactionManagerClient client) {
        return new ServiceSteps(client);
    }

    @Bean
    public OpenApiSteps openApiSteps(WebApplicationContext context) {
        return new OpenApiSteps(MockMvcBuilders.webAppContextSetup(context).build());
    }

}