package com.taskforge.issue.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskforge.issue.dto.CreateIssueRequest;
import com.taskforge.issue.dto.IssueResponse;
import com.taskforge.issue.dto.UpdateIssueRequest;
import com.taskforge.issue.model.Issue.IssuePriority;
import com.taskforge.issue.model.Issue.IssueStatus;
import com.taskforge.issue.model.Issue.IssueType;
import com.taskforge.issue.service.WorkLogService;
import com.taskforge.issue.service.IssueService;
import com.taskforge.issue.service.CommentService;
import com.taskforge.membership.tenant.repository.TenantMembershipRepository;
import com.taskforge.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = IssueController.class, excludeAutoConfiguration = {
        SecurityAutoConfiguration.class,
        OAuth2ResourceServerAutoConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class IssueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IssueService issueService;

    // Mocks required for SecurityConfig and its dependencies
    @MockitoBean
    private TenantMembershipRepository tenantMembershipRepository;

    @MockitoBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CommentService commentService;

    @MockitoBean
    private WorkLogService workLogService;

    @Test
    void createIssue_ShouldReturnCreatedIssue() throws Exception {
        UUID projectId = UUID.randomUUID();
        CreateIssueRequest request = new CreateIssueRequest();
        request.setProjectId(projectId);
        request.setTitle("Test Issue");
        request.setType(IssueType.TASK);
        request.setPriority(IssuePriority.MEDIUM);

        IssueResponse response = new IssueResponse();
        response.setId(UUID.randomUUID());
        response.setTitle("Test Issue");
        response.setStatus(IssueStatus.TODO);

        when(issueService.createIssue(any(CreateIssueRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/issues")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Test Issue"))
                .andExpect(jsonPath("$.status").value("TODO"));
    }

    @Test
    void getProjectIssues_ShouldReturnList() throws Exception {
        UUID projectId = UUID.randomUUID();
        IssueResponse issue1 = new IssueResponse();
        issue1.setId(UUID.randomUUID());

        when(issueService.getProjectIssues(String.valueOf(projectId))).thenReturn(List.of(issue1));

        mockMvc.perform(get("/api/v1/projects/{projectId}/issues", projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").exists());
    }

    @Test
    void updateIssue_ShouldReturnUpdatedIssue() throws Exception {
        UUID issueId = UUID.randomUUID();
        UpdateIssueRequest request = new UpdateIssueRequest();
        request.setStatus(IssueStatus.IN_PROGRESS);

        IssueResponse response = new IssueResponse();
        response.setId(issueId);
        response.setStatus(IssueStatus.IN_PROGRESS);

        when(issueService.updateIssue(eq(issueId), any(UpdateIssueRequest.class))).thenReturn(response);

        mockMvc.perform(patch("/api/v1/issues/{issueId}", issueId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void deleteIssue_ShouldReturnNoContent() throws Exception {
        UUID issueId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/issues/{issueId}", issueId))
                .andExpect(status().isNoContent());

        verify(issueService).deleteIssue(issueId);
    }
}
