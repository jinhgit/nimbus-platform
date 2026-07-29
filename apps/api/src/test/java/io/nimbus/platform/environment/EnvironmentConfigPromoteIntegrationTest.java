package io.nimbus.platform.environment;

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
class EnvironmentConfigPromoteIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AppServiceRepository appServiceRepository;

    @Test
    void variablesSecretsAndPromoteDevToStage() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/dev-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Promote Tester","email":"promote@nimbus.local"}
                                """))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode loginBody = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String token = loginBody.path("data").path("accessToken").asText();
        String workspaceId = loginBody.path("data").path("user").path("workspaceId").asText();
        UUID userId = UUID.fromString(loginBody.path("data").path("user").path("id").asText());

        MvcResult projectResult = mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Promote Project","workspaceId":"%s"}
                                """.formatted(workspaceId)))
                .andExpect(status().isOk())
                .andReturn();
        UUID projectId = UUID.fromString(
                objectMapper.readTree(projectResult.getResponse().getContentAsString())
                        .path("data").path("id").asText()
        );

        AppService service = AppService.create(
                projectId, UUID.fromString(workspaceId), "order-api", "svc",
                RuntimeType.SPRING_BOOT, null, EnvironmentType.DEV,
                1, null, null, false, null, userId
        );
        service.markReady();
        service = appServiceRepository.save(service);

        // ensure DEV env
        MvcResult listEnv = mockMvc.perform(get("/api/v1/services/{id}/environments", service.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].type").value("DEV"))
                .andReturn();
        String devEnvId = objectMapper.readTree(listEnv.getResponse().getContentAsString())
                .path("data").path("items").get(0).path("id").asText();

        // variable
        mockMvc.perform(post("/api/v1/environments/{id}/variables", devEnvId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"key":"LOG_LEVEL","value":"INFO"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.key").value("LOG_LEVEL"))
                .andExpect(jsonPath("$.data.value").value("INFO"));

        // secret masked — promote also records thin GitOps (SIMULATED without SCM)
        MvcResult secretCreate = mockMvc.perform(post("/api/v1/environments/{id}/secrets", devEnvId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"key":"DB_PASSWORD","value":"s3cret-pass"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.key").value("DB_PASSWORD"))
                .andExpect(jsonPath("$.data.maskedValue").value("••••••••"))
                .andReturn();
        String secretId = objectMapper.readTree(secretCreate.getResponse().getContentAsString())
                .path("data").path("id").asText();

        // list secrets never leaks plain
        mockMvc.perform(get("/api/v1/environments/{id}/secrets", devEnvId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].maskedValue").value("••••••••"))
                .andExpect(jsonPath("$.data[0].value").doesNotExist());

        // reveal
        mockMvc.perform(post("/api/v1/secrets/{id}/reveal", secretId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.value").value("s3cret-pass"));

        // invalid promote path
        mockMvc.perform(post("/api/v1/environments/{id}/promote", devEnvId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"target":"PRODUCTION"}
                                """))
                .andExpect(status().isBadRequest());

        // promote DEV → STAGE
        mockMvc.perform(post("/api/v1/environments/{id}/promote", devEnvId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"target":"STAGE"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.targetType").value("STAGE"))
                .andExpect(jsonPath("$.data.variablesCopied").value(1))
                .andExpect(jsonPath("$.data.secretsCopied").value(1))
                .andExpect(jsonPath("$.data.gitOpsMode").value("SIMULATED"))
                .andExpect(jsonPath("$.data.gitOpsHeadBranch").isNotEmpty())
                .andExpect(jsonPath("$.data.gitOpsBaseBranch").isNotEmpty());

        // STAGE exists with copied variable
        MvcResult envsAfter = mockMvc.perform(get("/api/v1/services/{id}/environments", service.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.count").value(2))
                .andReturn();
        JsonNode items = objectMapper.readTree(envsAfter.getResponse().getContentAsString())
                .path("data").path("items");
        String stageId = null;
        for (JsonNode item : items) {
            if ("STAGE".equals(item.path("type").asText())) {
                stageId = item.path("id").asText();
            }
        }
        assertThat(stageId).isNotBlank();

        mockMvc.perform(get("/api/v1/environments/{id}/variables", stageId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].key").value("LOG_LEVEL"))
                .andExpect(jsonPath("$.data[0].value").value("INFO"));

        mockMvc.perform(get("/api/v1/environments/{id}/secrets", stageId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].key").value("DB_PASSWORD"));

        // promotion history
        mockMvc.perform(get("/api/v1/services/{id}/promotions", service.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].sourceType").value("DEV"));

        // audit
        mockMvc.perform(get("/api/v1/audit")
                        .param("workspaceId", workspaceId)
                        .param("action", "PROMOTE_ENVIRONMENT")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].action").value("PROMOTE_ENVIRONMENT"));
    }
}
