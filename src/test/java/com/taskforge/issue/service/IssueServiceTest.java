package com.taskforge.issue.service;

import com.taskforge.common.exception.ApiException;
import com.taskforge.common.tenant.TenantContext;
import com.taskforge.issue.dto.UpdateIssueRequest;
import com.taskforge.issue.model.Issue;
import com.taskforge.issue.model.Issue.IssueStatus;
import com.taskforge.issue.repository.IssueRepository;
import com.taskforge.membership.project.model.ProjectMembership;
import com.taskforge.membership.project.model.ProjectMembership.ProjectRole;
import com.taskforge.membership.project.repository.ProjectMembershipRepository;
import com.taskforge.project.repository.ProjectRepository;
import com.taskforge.issue.repository.IssueHistoryRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IssueServiceTest {

    @Mock
    private IssueRepository issueRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectMembershipRepository projectMembershipRepository;

    @Mock
    private com.taskforge.membership.tenant.repository.TenantMembershipRepository tenantMembershipRepository;

    @Mock
    private IssueHistoryRepository issueHistoryRepository;

    @InjectMocks
    private IssueService issueService;

    private UUID tenantId;
    private UUID projectId;
    private UUID userId;
    private UUID issueId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        projectId = UUID.randomUUID();
        userId = UUID.randomUUID();
        issueId = UUID.randomUUID();

        // Mock TenantContext
        TenantContext.set(tenantId);

        // Mock SecurityContext
        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        Jwt jwt = mock(Jwt.class);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(jwt);
        when(jwt.getSubject()).thenReturn(userId.toString());

        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void updateIssue_shouldThrowException_whenNonQAMovesFromTestingToDone() {
        // Arrange
        UpdateIssueRequest request = new UpdateIssueRequest();
        request.setStatus(IssueStatus.DONE);

        Issue issue = new Issue();
        issue.setId(issueId);
        issue.setTenantId(tenantId);
        issue.setProjectId(projectId);
        issue.setStatus(IssueStatus.IN_TESTING);

        when(issueRepository.findByTenantIdAndId(tenantId, issueId)).thenReturn(Optional.of(issue));

        ProjectMembership membership = new ProjectMembership();
        membership.setRole(ProjectRole.CONTRIBUTOR);
        when(projectMembershipRepository.findByTenantIdAndProjectIdAndUserId(tenantId, projectId, userId))
                .thenReturn(Optional.of(membership));

        // Act & Assert
        assertThrows(ApiException.class, () -> issueService.updateIssue(issueId, request),
                "Should throw ApiException for workflow violation");
    }

    @Test
    void updateIssue_shouldSucceed_whenQAMovesFromTestingToDone() {
        // Arrange
        UpdateIssueRequest request = new UpdateIssueRequest();
        request.setStatus(IssueStatus.DONE);

        Issue issue = new Issue();
        issue.setId(issueId);
        issue.setTenantId(tenantId);
        issue.setProjectId(projectId);
        issue.setStatus(IssueStatus.IN_TESTING);

        when(issueRepository.findByTenantIdAndId(tenantId, issueId)).thenReturn(Optional.of(issue));
        when(issueRepository.save(any(Issue.class))).thenReturn(issue);

        ProjectMembership membership = new ProjectMembership();
        membership.setRole(ProjectRole.QA);
        when(projectMembershipRepository.findByTenantIdAndProjectIdAndUserId(tenantId, projectId, userId))
                .thenReturn(Optional.of(membership));

        // Act & Assert
        assertDoesNotThrow(() -> issueService.updateIssue(issueId, request));
    }

    @Test
    void updateIssue_shouldSucceed_whenManagerMovesFromTestingToDone() {
        // Arrange
        UpdateIssueRequest request = new UpdateIssueRequest();
        request.setStatus(IssueStatus.DONE);

        Issue issue = new Issue();
        issue.setId(issueId);
        issue.setTenantId(tenantId);
        issue.setProjectId(projectId);
        issue.setStatus(IssueStatus.IN_TESTING);

        when(issueRepository.findByTenantIdAndId(tenantId, issueId)).thenReturn(Optional.of(issue));
        when(issueRepository.save(any(Issue.class))).thenReturn(issue);

        ProjectMembership membership = new ProjectMembership();
        membership.setRole(ProjectRole.MANAGER);
        when(projectMembershipRepository.findByTenantIdAndProjectIdAndUserId(tenantId, projectId, userId))
                .thenReturn(Optional.of(membership));

        // Act & Assert
        assertDoesNotThrow(() -> issueService.updateIssue(issueId, request));
    }

    @Test
    void updateIssue_shouldSucceed_whenTenantAdminMovesFromTestingToDone() {
        // Arrange
        UpdateIssueRequest request = new UpdateIssueRequest();
        request.setStatus(IssueStatus.DONE);

        Issue issue = new Issue();
        issue.setId(issueId);
        issue.setTenantId(tenantId);
        issue.setProjectId(projectId);
        issue.setStatus(IssueStatus.IN_TESTING);

        when(issueRepository.findByTenantIdAndId(tenantId, issueId)).thenReturn(Optional.of(issue));
        when(issueRepository.save(any(Issue.class))).thenReturn(issue);

        // Tenant Admin Mock
        com.taskforge.membership.tenant.model.TenantMembership tenantMembership = new com.taskforge.membership.tenant.model.TenantMembership();
        tenantMembership.setRole("ADMIN");
        when(tenantMembershipRepository.findByTenantIdAndUserId(tenantId, userId))
                .thenReturn(Optional.of(tenantMembership));

        // Act & Assert
        assertDoesNotThrow(() -> issueService.updateIssue(issueId, request),
                "Tenant Admin should bypass project check");
    }
}
