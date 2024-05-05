package io.transatron.transaction.manager.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration(proxyBeanMethods = false)
@PropertySource("classpath:transaction-manager-db.properties")
public class PostgreSQLConfiguration {
}
