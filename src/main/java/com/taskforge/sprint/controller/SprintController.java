package com.taskforge.sprint.controller;

import com.taskforge.sprint.model.Sprint;
import com.taskforge.sprint.service.SprintService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class SprintController {

    private final SprintService sprintService;

    @PostMapping("/projects/{projectIdOrKey}/sprints")
    public ResponseEntity<Sprint> createSprint(
            @RequestAttribute("tenantId") UUID tenantId,
            @PathVariable String projectIdOrKey,
            @RequestBody Sprint sprint) {
        return ResponseEntity.ok(sprintService.createSprint(tenantId, projectIdOrKey, sprint));
    }

    @GetMapping("/projects/{projectIdOrKey}/sprints")
    public ResponseEntity<List<Sprint>> getSprints(
            @PathVariable String projectIdOrKey) {
        return ResponseEntity.ok(sprintService.getSprintsByProject(projectIdOrKey));
    }

    @PatchMapping("/sprints/{sprintId}")
    public ResponseEntity<Sprint> updateSprint(
            @PathVariable UUID sprintId,
            @RequestBody Sprint sprint) {
        return ResponseEntity.ok(sprintService.updateSprint(sprintId, sprint));
    }

    @DeleteMapping("/sprints/{sprintId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSprint(@PathVariable UUID sprintId) {
        sprintService.deleteSprint(sprintId);
    }
}
