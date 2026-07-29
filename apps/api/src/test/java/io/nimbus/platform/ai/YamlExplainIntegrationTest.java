package io.nimbus.platform.ai;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class YamlExplainIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void explainDeploymentYamlWithRuleEngine() throws Exception {
        MvcResult login = mockMvc.perform(post("/api/v1/auth/dev-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Yaml User","email":"yaml-explain@nimbus.local"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        String token = objectMapper.readTree(login.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();

        String yaml = """
                apiVersion: apps/v1
                kind: Deployment
                metadata:
                  name: payment-api
                spec:
                  replicas: 1
                  template:
                    spec:
                      containers:
                        - name: app
                          image: nginx:latest
                          ports:
                            - containerPort: 8080
                """;

        MvcResult res = mockMvc.perform(post("/api/v1/ai/yaml/explain")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": %s,
                                  "kind": "AUTO",
                                  "serviceName": "payment-api",
                                  "environmentType": "PRODUCTION"
                                }
                                """.formatted(objectMapper.writeValueAsString(yaml))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.provider").value("rule-engine"))
                .andExpect(jsonPath("$.data.detectedKind").value("DEPLOYMENT"))
                .andExpect(jsonPath("$.data.highlights.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)))
                .andReturn();

        JsonNode data = objectMapper.readTree(res.getResponse().getContentAsString()).path("data");
        assertThat(data.path("summary").asText()).containsIgnoringCase("payment");
        assertThat(data.path("risks").isArray()).isTrue();
        assertThat(data.path("risks").size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void emptyContentRejected() throws Exception {
        MvcResult login = mockMvc.perform(post("/api/v1/auth/dev-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Yaml User2","email":"yaml-explain2@nimbus.local"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        String token = objectMapper.readTree(login.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();

        mockMvc.perform(post("/api/v1/ai/yaml/explain")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"   "}
                                """))
                .andExpect(status().isBadRequest());
    }
}
