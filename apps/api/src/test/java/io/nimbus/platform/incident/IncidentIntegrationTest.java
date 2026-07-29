package io.nimbus.platform.incident;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class IncidentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void listScanAndAiStatus() throws Exception {
        MvcResult login = mockMvc.perform(post("/api/v1/auth/dev-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Inc User","email":"inc@nimbus.local"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = objectMapper.readTree(login.getResponse().getContentAsString());
        String token = body.path("data").path("accessToken").asText();
        String workspaceId = body.path("data").path("user").path("workspaceId").asText();

        mockMvc.perform(get("/api/v1/incidents")
                        .param("workspaceId", workspaceId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());

        mockMvc.perform(get("/api/v1/incidents/counts")
                        .param("workspaceId", workspaceId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.open").isNumber());

        mockMvc.perform(post("/api/v1/incidents/scan")
                        .param("workspaceId", workspaceId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.scanned").isNumber())
                .andExpect(jsonPath("$.data.opened").isNumber());

        mockMvc.perform(get("/api/v1/ai/status")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.configuredProvider").value("rule"))
                .andExpect(jsonPath("$.data.activeProvider").value("rule-engine"));
    }
}
