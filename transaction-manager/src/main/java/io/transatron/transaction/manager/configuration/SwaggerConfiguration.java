package io.transatron.transaction.manager.configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.GroupedOpenApi;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;

@Configuration
@RequiredArgsConstructor
public class SwaggerConfiguration {

    private final Optional<BuildProperties> buildProperties;

    @Bean
    public GroupedOpenApi api() {
        return GroupedOpenApi.builder()
                             .group("public-api")
                             .packagesToScan("io.transatron.transaction.manager")
                             .pathsToMatch("/**")
                             .build();
    }

    @Bean
    public OpenAPI apiInfo() {
        return new OpenAPI()
                .info(new Info().version(buildProperties.map(BuildProperties::getVersion).orElse("UNKNOWN"))
                                .title("TronTransactionManager Service"));
    }

}