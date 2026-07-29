package io.nimbus.platform.deployment;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import io.nimbus.platform.catalog.domain.RuntimeType;
import io.nimbus.platform.serviceapp.domain.AppService;
import io.nimbus.platform.serviceapp.domain.EnvironmentType;
import io.nimbus.platform.serviceapp.repository.AppServiceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DeploymentAndSecretSyncIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AppServiceRepository appServiceRepository;

    @Test
    void promoteRecordsDeploymentAndSecretSyncIsSimulated() throws Exception {
        MvcResult login = mockMvc.perform(post("/api/v1/auth/dev-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Deploy Tester","email":"deploy-d@nimbus.local"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode loginBody = objectMapper.readTree(login.getResponse().getContentAsString());
        String token = loginBody.path("data").path("accessToken").asText();
        String workspaceId = loginBody.path("data").path("user").path("workspaceId").asText();
        UUID userId = UUID.fromString(loginBody.path("data").path("user").path("id").asText());

        MvcResult projectResult = mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Deploy Project","workspaceId":"%s"}
                                """.formatted(workspaceId)))
                .andExpect(status().isOk())
                .andReturn();
        UUID projectId = UUID.fromString(
                objectMapper.readTree(projectResult.getResponse().getContentAsString())
                        .path("data").path("id").asText()
        );

        AppService service = AppService.create(
                projectId, UUID.fromString(workspaceId), "billing-api", "svc",
                RuntimeType.SPRING_BOOT, null, EnvironmentType.DEV,
                1, null, null, false, null, userId
        );
        service.markReady();
        service = appServiceRepository.save(service);

        MvcResult envs = mockMvc.perform(get("/api/v1/services/{id}/environments", service.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        String devEnvId = objectMapper.readTree(envs.getResponse().getContentAsString())
                .path("data").path("items").get(0).path("id").asText();

        mockMvc.perform(post("/api/v1/environments/{id}/secrets", devEnvId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"key":"API_TOKEN","value":"tok-123"}
                                """))
                .andExpect(status().isOk());

        // GitHub secret sync — no SCM → SIMULATED
        mockMvc.perform(post("/api/v1/environments/{id}/secrets/sync-github", devEnvId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mode").value("SIMULATED"))
                .andExpect(jsonPath("$.data.succeeded").value(1))
                .andExpect(jsonPath("$.data.items[0].key").value("API_TOKEN"));

        // Promote DEV → STAGE creates deployment history
        mockMvc.perform(post("/api/v1/environments/{id}/promote", devEnvId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"target":"STAGE"}
                                """))
                .andExpect(status().isOk());

        MvcResult deps = mockMvc.perform(get("/api/v1/services/{id}/deployments", service.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.count").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.items[0].trigger").value("ENVIRONMENT_PROMOTE"))
                .andReturn();

        assertThat(objectMapper.readTree(deps.getResponse().getContentAsString())
                .path("data").path("items").get(0).path("environmentType").asText())
                .isEqualTo("STAGE");

        mockMvc.perform(get("/api/v1/services/{id}/timeline", service.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));

        mockMvc.perform(get("/api/v1/audit")
                        .param("workspaceId", workspaceId)
                        .param("action", "SYNC_GITHUB_SECRETS")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].action").value("SYNC_GITHUB_SECRETS"));
    }
}
