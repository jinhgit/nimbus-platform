package io.nimbus.platform.wizard;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WizardFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void catalogWizardRecommendPreviewExecute() throws Exception {
        MvcResult login = mockMvc.perform(post("/api/v1/auth/dev-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Wizard User","email":"wizard@nimbus.local"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode loginBody = objectMapper.readTree(login.getResponse().getContentAsString());
        String token = loginBody.path("data").path("accessToken").asText();
        String workspaceId = loginBody.path("data").path("user").path("workspaceId").asText();

        MvcResult projectResult = mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Payment Platform","workspaceId":"%s","description":"demo"}
                                """.formatted(workspaceId)))
                .andExpect(status().isOk())
                .andReturn();
        String projectId = objectMapper.readTree(projectResult.getResponse().getContentAsString())
                .path("data").path("id").asText();

        MvcResult catalog = mockMvc.perform(get("/api/v1/catalog")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andReturn();
        JsonNode catalogData = objectMapper.readTree(catalog.getResponse().getContentAsString()).path("data");
        String templateId = null;
        for (JsonNode node : catalogData) {
            if ("SPRING_BOOT".equals(node.path("runtime").asText())) {
                templateId = node.path("id").asText();
                break;
            }
        }
        if (templateId == null) {
            templateId = catalogData.get(0).path("id").asText();
        }
        assertThat(templateId).isNotBlank();

        MvcResult wizardCreate = mockMvc.perform(post("/api/v1/service-wizard")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"projectId":"%s","serviceName":"payment-api","templateId":"%s"}
                                """.formatted(projectId, templateId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andReturn();
        String wizardId = objectMapper.readTree(wizardCreate.getResponse().getContentAsString())
                .path("data").path("id").asText();

        mockMvc.perform(patch("/api/v1/service-wizard/" + wizardId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"environmentType":"PRODUCTION","currentStep":3}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.environmentType").value("PRODUCTION"));

        mockMvc.perform(post("/api/v1/service-wizard/" + wizardId + "/recommend")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.runtime").value("SPRING_BOOT"))
                .andExpect(jsonPath("$.data.database").value("POSTGRES"))
                .andExpect(jsonPath("$.data.replicaCount").value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)));

        mockMvc.perform(post("/api/v1/service-wizard/" + wizardId + "/preview")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.repository").value("payment-api"))
                .andExpect(jsonPath("$.data.helmValues").isNotEmpty())
                .andExpect(jsonPath("$.data.deploymentYaml").isNotEmpty());

        mockMvc.perform(post("/api/v1/service-wizard/" + wizardId + "/execute")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PROVISIONING"));

        // poll until complete (async provision ~5s)
        boolean completed = false;
        for (int i = 0; i < 40; i++) {
            Thread.sleep(300);
            MvcResult status = mockMvc.perform(get("/api/v1/service-wizard/" + wizardId)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andReturn();
            String st = objectMapper.readTree(status.getResponse().getContentAsString())
                    .path("data").path("status").asText();
            if ("COMPLETED".equals(st)) {
                completed = true;
                assertThat(objectMapper.readTree(status.getResponse().getContentAsString())
                        .path("data").path("progress").asInt()).isEqualTo(100);
                break;
            }
        }
        assertThat(completed).isTrue();

        mockMvc.perform(get("/api/v1/services")
                        .param("projectId", projectId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("payment-api"))
                .andExpect(jsonPath("$.data[0].status").value("READY"));
    }
}
