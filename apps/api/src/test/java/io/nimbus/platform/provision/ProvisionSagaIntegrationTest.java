package io.nimbus.platform.provision;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProvisionSagaIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void executeCreatesSagaWithStepsAndCompletes() throws Exception {
        MvcResult login = mockMvc.perform(post("/api/v1/auth/dev-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Saga User","email":"saga@nimbus.local"}
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
                                {"name":"Saga Project","workspaceId":"%s"}
                                """.formatted(workspaceId)))
                .andExpect(status().isOk())
                .andReturn();
        String projectId = objectMapper.readTree(projectResult.getResponse().getContentAsString())
                .path("data").path("id").asText();

        MvcResult catalog = mockMvc.perform(get("/api/v1/catalog")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        String templateId = objectMapper.readTree(catalog.getResponse().getContentAsString())
                .path("data").get(0).path("id").asText();

        MvcResult wizardCreate = mockMvc.perform(post("/api/v1/service-wizard")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"projectId":"%s","serviceName":"saga-api","templateId":"%s"}
                                """.formatted(projectId, templateId)))
                .andExpect(status().isOk())
                .andReturn();
        String wizardId = objectMapper.readTree(wizardCreate.getResponse().getContentAsString())
                .path("data").path("id").asText();

        mockMvc.perform(patch("/api/v1/service-wizard/{id}", wizardId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"runtime":"SPRING_BOOT","environmentType":"DEV","replicaCount":1}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/service-wizard/{id}/recommend", wizardId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/service-wizard/{id}/preview", wizardId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        MvcResult exec = mockMvc.perform(post("/api/v1/service-wizard/{id}/execute", wizardId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.jobId").isNotEmpty())
                .andReturn();
        String sagaId = objectMapper.readTree(exec.getResponse().getContentAsString())
                .path("data").path("jobId").asText();
        assertThat(sagaId).isNotBlank();

        // poll until COMPLETED or timeout
        String status = "PROVISIONING";
        for (int i = 0; i < 40; i++) {
            Thread.sleep(250);
            MvcResult st = mockMvc.perform(get("/api/v1/service-wizard/{id}/status", wizardId)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andReturn();
            status = objectMapper.readTree(st.getResponse().getContentAsString())
                    .path("data").path("status").asText();
            if ("COMPLETED".equals(status) || "FAILED".equals(status)) {
                break;
            }
        }
        assertThat(status).isEqualTo("COMPLETED");

        MvcResult saga = mockMvc.perform(get("/api/v1/service-wizard/{id}/saga", wizardId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.attempt").value(1))
                .andExpect(jsonPath("$.data.steps.length()").value(6))
                .andReturn();

        JsonNode steps = objectMapper.readTree(saga.getResponse().getContentAsString())
                .path("data").path("steps");
        for (JsonNode step : steps) {
            assertThat(step.path("status").asText()).isEqualTo("SUCCESS");
        }

        // retry only FAILED
        mockMvc.perform(post("/api/v1/service-wizard/{id}/retry", wizardId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }
}
