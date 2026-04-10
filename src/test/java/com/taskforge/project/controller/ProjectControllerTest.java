package com.taskforge.project.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskforge.membership.tenant.repository.TenantMembershipRepository;
import com.taskforge.project.dto.CreateProjectRequest;
import com.taskforge.project.dto.ProjectResponse;
import com.taskforge.project.dto.UpdateProjectRequest;
import com.taskforge.project.service.ProjectService;
import com.taskforge.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProjectController.class)
@AutoConfigureMockMvc(addFilters = false) // Disable security filters
class ProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProjectService projectService;

    // Mocks required for SecurityConfig and its dependencies
    @MockitoBean
    private TenantMembershipRepository tenantMembershipRepository;

    @MockitoBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createProject_ShouldReturnCreatedProject() throws Exception {
        CreateProjectRequest request = new CreateProjectRequest();
        request.setKey("TF");
        request.setName("TaskForge");

        ProjectResponse response = new ProjectResponse();
        response.setId(UUID.randomUUID());
        response.setKey("TF");
        response.setName("TaskForge");

        when(projectService.createProject(any(CreateProjectRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/projects")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.key").value("TF"))
                .andExpect(jsonPath("$.name").value("TaskForge"));
    }

    @Test
    void getProjects_ShouldReturnList() throws Exception {
        ProjectResponse project1 = new ProjectResponse();
        project1.setId(UUID.randomUUID());

        when(projectService.getProjects()).thenReturn(List.of(project1));

        mockMvc.perform(get("/api/v1/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").exists());
    }

    @Test
    void updateProject_ShouldReturnUpdatedProject() throws Exception {
        UUID projectId = UUID.randomUUID();
        UpdateProjectRequest request = new UpdateProjectRequest();
        request.setName("New Name");

        ProjectResponse response = new ProjectResponse();
        response.setId(projectId);
        response.setName("New Name");

        when(projectService.updateProject(ArgumentMatchers.eq(String.valueOf(projectId)),
                any(UpdateProjectRequest.class))).thenReturn(response);

        mockMvc.perform(patch("/api/v1/projects/{projectId}", projectId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Name"));
    }

    @Test
    void deleteProject_ShouldReturnNoContent() throws Exception {
        UUID projectId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/projects/{projectId}", projectId))
                .andExpect(status().isNoContent());

        verify(projectService).deleteProject(String.valueOf(projectId));
    }
}
