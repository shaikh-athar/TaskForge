package com.taskforge.project.controller;

import com.taskforge.project.dto.CreateProjectRequest;
import com.taskforge.project.dto.ProjectResponse;
import com.taskforge.project.dto.UpdateProjectRequest;
import com.taskforge.project.service.ProjectService;
import com.taskforge.project.dto.request.AddProjectMemberRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping
    public List<ProjectResponse> getProjects() {
        return projectService.getProjects();
    }

    @GetMapping("/{projectIdOrKey}")
    public ProjectResponse getProject(@PathVariable String projectIdOrKey) {
        return projectService.getProject(projectIdOrKey);
    }

    @GetMapping("/{projectIdOrKey}/members")
    public List<com.taskforge.project.dto.response.ProjectMemberResponse> getProjectMembers(
            @PathVariable String projectIdOrKey) {
        return projectService.getProjectMembers(projectIdOrKey);
    }

    @PostMapping("/{projectIdOrKey}/members")
    @ResponseStatus(HttpStatus.CREATED)
    public void addProjectMember(
            @PathVariable String projectIdOrKey,
            @RequestBody @Valid AddProjectMemberRequest request) {
        projectService.addMember(projectIdOrKey, request.getEmail(), request.getRole());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectResponse createProject(@RequestBody @Valid CreateProjectRequest request) {
        return projectService.createProject(request);
    }

    @PatchMapping("/{projectIdOrKey}")
    public ProjectResponse updateProject(
            @PathVariable String projectIdOrKey,
            @RequestBody @Valid UpdateProjectRequest request) {
        return projectService.updateProject(projectIdOrKey, request);
    }

    @DeleteMapping("/{projectIdOrKey}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProject(@PathVariable String projectIdOrKey) {
        projectService.deleteProject(projectIdOrKey);
    }

    @GetMapping("/{projectIdOrKey}/insights")
    public com.taskforge.project.dto.ProjectInsightsResponse getInsights(@PathVariable String projectIdOrKey) {
        return projectService.getInsights(projectIdOrKey);
    }
}
