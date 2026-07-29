package io.nimbus.platform.audit;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuditLogIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void loginAndCreateProjectAreAudited() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/dev-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"감사테스터","email":"audit@nimbus.local"}
                                """))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode loginBody = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String accessToken = loginBody.path("data").path("accessToken").asText();
        String workspaceId = loginBody.path("data").path("user").path("workspaceId").asText();
        assertThat(workspaceId).isNotBlank();

        mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Audit Demo",
                                  "workspaceId": "%s",
                                  "description": "감사 로그 검증"
                                }
                                """.formatted(workspaceId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Audit Demo"));

        MvcResult auditResult = mockMvc.perform(get("/api/v1/audit")
                        .param("workspaceId", workspaceId)
                        .param("limit", "50")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        JsonNode auditBody = objectMapper.readTree(auditResult.getResponse().getContentAsString());
        JsonNode items = auditBody.path("data").path("items");
        assertThat(items.isArray()).isTrue();
        assertThat(items.size()).isGreaterThanOrEqualTo(2);

        boolean hasLogin = false;
        boolean hasCreateProject = false;
        for (JsonNode item : items) {
            String action = item.path("action").asText();
            if ("LOGIN".equals(action)) {
                hasLogin = true;
            }
            if ("CREATE_PROJECT".equals(action)) {
                hasCreateProject = true;
                assertThat(item.path("resourceName").asText()).isEqualTo("Audit Demo");
                assertThat(item.path("result").asText()).isEqualTo("SUCCESS");
            }
        }
        assertThat(hasLogin).isTrue();
        assertThat(hasCreateProject).isTrue();

        mockMvc.perform(get("/api/v1/audit")
                        .param("workspaceId", workspaceId)
                        .param("action", "CREATE_PROJECT")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].action").value("CREATE_PROJECT"));
    }
}
