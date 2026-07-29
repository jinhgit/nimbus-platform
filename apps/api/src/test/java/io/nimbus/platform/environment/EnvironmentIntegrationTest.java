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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EnvironmentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AppServiceRepository appServiceRepository;

    @Test
    void createListUpdateArchiveAndAudit() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/dev-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Env Tester","email":"env@nimbus.local"}
                                """))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode loginBody = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String accessToken = loginBody.path("data").path("accessToken").asText();
        String workspaceId = loginBody.path("data").path("user").path("workspaceId").asText();
        UUID userId = UUID.fromString(loginBody.path("data").path("user").path("id").asText());

        MvcResult projectResult = mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Env Project",
                                  "workspaceId": "%s"
                                }
                                """.formatted(workspaceId)))
                .andExpect(status().isOk())
                .andReturn();
        UUID projectId = UUID.fromString(
                objectMapper.readTree(projectResult.getResponse().getContentAsString())
                        .path("data").path("id").asText()
        );

        AppService service = AppService.create(
                projectId,
                UUID.fromString(workspaceId),
                "payment-api",
                "test service",
                RuntimeType.SPRING_BOOT,
                null,
                EnvironmentType.DEV,
                2,
                "POSTGRESQL",
                "REDIS",
                true,
                null,
                userId
        );
        service.markReady();
        service = appServiceRepository.save(service);
        UUID serviceId = service.getId();

        // list auto-creates default DEV
        mockMvc.perform(get("/api/v1/services/{id}/environments", serviceId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.count").value(1))
                .andExpect(jsonPath("$.data.items[0].type").value("DEV"))
                .andExpect(jsonPath("$.data.items[0].status").value("READY"))
                .andExpect(jsonPath("$.data.items[0].namespace").value("payment-api-dev"));

        // create STAGE
        MvcResult stageResult = mockMvc.perform(post("/api/v1/services/{id}/environments", serviceId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "STAGE",
                                  "replicaCount": 2,
                                  "domain": "stage.payment.local"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.type").value("STAGE"))
                .andExpect(jsonPath("$.data.gitOpsBranch").value("staging"))
                .andReturn();

        String stageId = objectMapper.readTree(stageResult.getResponse().getContentAsString())
                .path("data").path("id").asText();

        // duplicate STAGE fails
        mockMvc.perform(post("/api/v1/services/{id}/environments", serviceId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type": "STAGE"}
                                """))
                .andExpect(status().isConflict());

        // update
        mockMvc.perform(patch("/api/v1/environments/{id}", stageId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"replicaCount": 3, "cpu": "500m", "memory": "512Mi"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.replicaCount").value(3))
                .andExpect(jsonPath("$.data.cpu").value("500m"));

        // health
        mockMvc.perform(get("/api/v1/environments/{id}/health", stageId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.healthStatus").isNotEmpty());

        // archive
        mockMvc.perform(post("/api/v1/environments/{id}/archive", stageId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ARCHIVED"));

        // list order DEV then STAGE
        MvcResult listResult = mockMvc.perform(get("/api/v1/services/{id}/environments", serviceId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.count").value(2))
                .andReturn();
        JsonNode items = objectMapper.readTree(listResult.getResponse().getContentAsString())
                .path("data").path("items");
        assertThat(items.get(0).path("type").asText()).isEqualTo("DEV");
        assertThat(items.get(1).path("type").asText()).isEqualTo("STAGE");

        // audit has CREATE_ENVIRONMENT
        mockMvc.perform(get("/api/v1/audit")
                        .param("workspaceId", workspaceId)
                        .param("action", "CREATE_ENVIRONMENT")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].action").value("CREATE_ENVIRONMENT"));
    }
}
