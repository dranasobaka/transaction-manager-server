package io.transatron.transaction.manager.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration(proxyBeanMethods = false)
public class ApplicationConfiguration {

    @Bean
    public Clock clock() {
        return Clock.systemUTC().withZone(ZoneId.of("Etc/GMT"));
    }

}
