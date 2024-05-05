package io.transatron.transaction.manager.functional.steps;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor
public class OpenApiSteps {

    public static final String DEFAULT_PLAIN_TARGET = "./target/open-api.yml";
    public static final String DEFAULT_ADOC_TARGET = "./target/open-api-definition.adoc";

    public static final String DEFAULT_API_DOCS_PATH = "/v3/api-docs";

    public static final String MACRO_TEMPLATE = """
        ++++
        <ac:structured-macro ac:name="open-api">
          <ac:plain-text-body>
            <![CDATA[%s]]>
          </ac:plain-text-body>
        </ac:structured-macro>
        ++++
        """;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final YAMLMapper yamlMapper = new YAMLMapper();

    private final MockMvc mockMvc;

    @Setter
    private String plainTarget = DEFAULT_PLAIN_TARGET;

    @Setter
    private String adocTarget = DEFAULT_ADOC_TARGET;

    @Setter
    private String apiDocsPath = DEFAULT_API_DOCS_PATH;

    @SneakyThrows
    public String getOpenApiDefinitionInYaml() {
        var json = mockMvc.perform(get(apiDocsPath))
                          .andExpect(status().isOk())
                          .andReturn()
                          .getResponse()
                          .getContentAsString();

        return asYaml(json);
    }

    public void writeOpenApiDefinitionInYaml() {
        write(getOpenApiDefinitionInYaml(), plainTarget);
    }

    public void writeOpenApiDefinitionInAdoc() {
        var yaml = getOpenApiDefinitionInYaml();
        var content = String.format(MACRO_TEMPLATE, yaml);
        write(content, adocTarget);
    }

    @SneakyThrows
    private void write(String content, String fileName) {
        var file = new File(fileName);

        if (!file.exists()) {
            file.getParentFile().mkdirs();
        }

        Files.writeString(
            file.getCanonicalFile().toPath(),
            content,
            StandardOpenOption.CREATE, StandardOpenOption.WRITE
        );
    }

    @SneakyThrows
    private String asYaml(String jsonString) {
        return yamlMapper.writeValueAsString(objectMapper.readTree(jsonString));
    }

}
