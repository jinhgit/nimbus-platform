package io.nimbus.platform.serviceapp;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ServiceTagsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AppServiceRepository appServiceRepository;

    @Test
    void updateAndFilterByTag() throws Exception {
        MvcResult login = mockMvc.perform(post("/api/v1/auth/dev-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Tag User","email":"tags@nimbus.local"}
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
                                {"name":"Tag Project","workspaceId":"%s"}
                                """.formatted(workspaceId)))
                .andExpect(status().isOk())
                .andReturn();
        UUID projectId = UUID.fromString(
                objectMapper.readTree(project.getResponse().getContentAsString())
                        .path("data").path("id").asText()
        );

        AppService service = AppService.create(
                projectId, UUID.fromString(workspaceId), "tag-api", "svc",
                RuntimeType.SPRING_BOOT, null, EnvironmentType.DEV,
                1, null, null, false, null, userId
        );
        service.markReady();
        service = appServiceRepository.save(service);

        mockMvc.perform(put("/api/v1/services/{id}/tags", service.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tags":["payment","critical","Payment"]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tags.length()").value(2))
                .andExpect(jsonPath("$.data.tags[0]").value("payment"));

        mockMvc.perform(get("/api/v1/services")
                        .param("workspaceId", workspaceId)
                        .param("tag", "payment")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("tag-api"));

        mockMvc.perform(get("/api/v1/services")
                        .param("workspaceId", workspaceId)
                        .param("tag", "missing-tag-xyz")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }
}
