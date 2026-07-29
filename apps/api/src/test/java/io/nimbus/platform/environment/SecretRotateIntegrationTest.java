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
class SecretRotateIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AppServiceRepository appServiceRepository;

    @Test
    void rotateSecretIncrementsVersionAndReturnsOnce() throws Exception {
        MvcResult login = mockMvc.perform(post("/api/v1/auth/dev-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Rotate User","email":"rotate@nimbus.local"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = objectMapper.readTree(login.getResponse().getContentAsString());
        String token = body.path("data").path("accessToken").asText();
        String workspaceId = body.path("data").path("user").path("workspaceId").asText();
        UUID userId = UUID.fromString(body.path("data").path("user").path("id").asText());

        MvcResult project = mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Rotate Project","workspaceId":"%s"}
                                """.formatted(workspaceId)))
                .andExpect(status().isOk())
                .andReturn();
        UUID projectId = UUID.fromString(
                objectMapper.readTree(project.getResponse().getContentAsString())
                        .path("data").path("id").asText()
        );

        AppService service = AppService.create(
                projectId, UUID.fromString(workspaceId), "rotate-api", "svc",
                RuntimeType.SPRING_BOOT, null, EnvironmentType.DEV,
                1, null, null, false, null, userId
        );
        service.markReady();
        service = appServiceRepository.save(service);

        MvcResult envs = mockMvc.perform(get("/api/v1/services/{id}/environments", service.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        String envId = objectMapper.readTree(envs.getResponse().getContentAsString())
                .path("data").path("items").get(0).path("id").asText();

        MvcResult created = mockMvc.perform(post("/api/v1/environments/{id}/secrets", envId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"key":"API_TOKEN","value":"initial-secret"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.version").value(1))
                .andReturn();
        String secretId = objectMapper.readTree(created.getResponse().getContentAsString())
                .path("data").path("id").asText();

        MvcResult rotated = mockMvc.perform(post("/api/v1/secrets/{id}/rotate", secretId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"generateRandom":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.version").value(2))
                .andExpect(jsonPath("$.data.generated").value(true))
                .andExpect(jsonPath("$.data.plainValueOnce").isNotEmpty())
                .andReturn();

        String once = objectMapper.readTree(rotated.getResponse().getContentAsString())
                .path("data").path("plainValueOnce").asText();
        assertThat(once).isNotEqualTo("initial-secret");
        assertThat(once.length()).isGreaterThan(8);

        mockMvc.perform(get("/api/v1/environments/{id}/secrets", envId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].version").value(2))
                .andExpect(jsonPath("$.data[0].maskedValue").value("••••••••"));
    }
}
