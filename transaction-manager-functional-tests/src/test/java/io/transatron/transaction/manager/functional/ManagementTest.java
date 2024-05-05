package io.transatron.transaction.manager.functional;

import io.transatron.transaction.manager.functional.steps.OpenApiSteps;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ManagementTest extends BaseFunctionalTest {

    @Autowired
    private OpenApiSteps openApiSteps;

    @Test
    void shouldWriteOpenApiToFile() {
        openApiSteps.writeOpenApiDefinitionInYaml();
        openApiSteps.writeOpenApiDefinitionInAdoc();
    }

}
