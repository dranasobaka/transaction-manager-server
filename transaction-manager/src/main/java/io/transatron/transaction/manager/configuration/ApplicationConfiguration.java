package io.transatron.transaction.manager.configuration;

import io.transatron.transaction.manager.web3.configuration.properties.TronProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@EnableFeignClients(basePackages = "io.transatron.transaction.manager")
@EnableConfigurationProperties({TronProperties.class})
@Configuration(proxyBeanMethods = false)
public class ApplicationConfiguration {

    @Bean
    public Clock clock() {
        return Clock.systemUTC().withZone(ZoneId.of("Etc/GMT"));
    }

}
