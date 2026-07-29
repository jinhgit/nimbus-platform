package io.nimbus.platform.workspace;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WorkspaceMemberIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void inviteAndChangeRoleToViewer() throws Exception {
        MvcResult ownerLogin = mockMvc.perform(post("/api/v1/auth/dev-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Owner","email":"member-owner@nimbus.local"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode ownerBody = objectMapper.readTree(ownerLogin.getResponse().getContentAsString());
        String ownerToken = ownerBody.path("data").path("accessToken").asText();
        String workspaceId = ownerBody.path("data").path("user").path("workspaceId").asText();

        mockMvc.perform(get("/api/v1/workspaces/{id}/members", workspaceId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));

        // invite new email → auto-create user
        MvcResult invite = mockMvc.perform(post("/api/v1/workspaces/{id}/members/invite", workspaceId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"member-viewer@nimbus.local","role":"DEVELOPER"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("member-viewer@nimbus.local"))
                .andExpect(jsonPath("$.data.role").value("DEVELOPER"))
                .andReturn();
        String memberId = objectMapper.readTree(invite.getResponse().getContentAsString())
                .path("data").path("id").asText();

        mockMvc.perform(patch("/api/v1/workspaces/{ws}/members/{mid}", workspaceId, memberId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"role":"VIEWER"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("VIEWER"));

        mockMvc.perform(get("/api/v1/workspaces/{id}/members", workspaceId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.email=='member-viewer@nimbus.local')].role")
                        .value(org.hamcrest.Matchers.hasItem("VIEWER")));
    }
}
