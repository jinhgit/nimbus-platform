package io.nimbus.platform.dashboard;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import io.nimbus.platform.workspace.domain.WorkspaceMember;
import io.nimbus.platform.workspace.domain.WorkspaceRole;
import io.nimbus.platform.workspace.repository.WorkspaceMemberRepository;
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

/**
 * Dashboard overview + Viewer RBAC (Promote/Project mutate 차단).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DashboardRbacIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WorkspaceMemberRepository memberRepository;

    @Test
    void dashboardOverviewAndViewerCannotCreateProject() throws Exception {
        MvcResult ownerLogin = mockMvc.perform(post("/api/v1/auth/dev-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Dash Owner","email":"dash-owner@nimbus.local"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode ownerBody = objectMapper.readTree(ownerLogin.getResponse().getContentAsString());
        String ownerToken = ownerBody.path("data").path("accessToken").asText();
        String workspaceId = ownerBody.path("data").path("user").path("workspaceId").asText();
        assertThat(workspaceId).isNotBlank();

        mockMvc.perform(get("/api/v1/dashboard/overview")
                        .param("workspaceId", workspaceId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.canMutate").value(true))
                .andExpect(jsonPath("$.data.workspaceRole").value("OWNER"))
                .andExpect(jsonPath("$.data.counts.projects").isNumber())
                .andExpect(jsonPath("$.data.counts.environments").isNumber())
                .andExpect(jsonPath("$.data.counts.failedSagas").isNumber())
                .andExpect(jsonPath("$.data.counts.auditEvents").isNumber())
                .andExpect(jsonPath("$.data.recentPromotes").isArray())
                .andExpect(jsonPath("$.data.failedSagas").isArray())
                .andExpect(jsonPath("$.data.recentAudits").isArray());

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.canMutate").value(true))
                .andExpect(jsonPath("$.data.workspaceRole").value("OWNER"));

        mockMvc.perform(get("/api/v1/auth/permissions")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.canMutate").value(true))
                .andExpect(jsonPath("$.data.permissions").isArray())
                .andExpect(jsonPath("$.data.permissions").value(org.hamcrest.Matchers.hasItem("PROMOTE")));

        // Viewer 멤버 생성 후 프로젝트 생성 차단
        MvcResult viewerLogin = mockMvc.perform(post("/api/v1/auth/dev-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Dash Viewer","email":"dash-viewer@nimbus.local"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode viewerBody = objectMapper.readTree(viewerLogin.getResponse().getContentAsString());
        String viewerToken = viewerBody.path("data").path("accessToken").asText();
        UUID viewerUserId = UUID.fromString(viewerBody.path("data").path("user").path("id").asText());

        memberRepository.save(WorkspaceMember.create(
                UUID.fromString(workspaceId),
                viewerUserId,
                WorkspaceRole.VIEWER,
                null
        ));

        // switch viewer to owner workspace (new tokens)
        MvcResult switchLogin = mockMvc.perform(patch("/api/v1/auth/workspace")
                        .header("Authorization", "Bearer " + viewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"workspaceId":"%s"}
                                """.formatted(workspaceId)))
                .andExpect(status().isOk())
                .andReturn();
        String switchedToken = objectMapper.readTree(switchLogin.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();

        mockMvc.perform(get("/api/v1/dashboard/overview")
                        .param("workspaceId", workspaceId)
                        .header("Authorization", "Bearer " + switchedToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.canMutate").value(false))
                .andExpect(jsonPath("$.data.workspaceRole").value("VIEWER"));

        mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", "Bearer " + switchedToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Viewer Denied","workspaceId":"%s"}
                                """.formatted(workspaceId)))
                .andExpect(status().isForbidden());
    }
}
