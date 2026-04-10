package com.taskforge.issue.controller;

import com.taskforge.issue.dto.CreateIssueRequest;
import com.taskforge.issue.dto.IssueResponse;
import com.taskforge.issue.dto.UpdateIssueRequest;
import com.taskforge.issue.dto.CreateCommentRequest;
import com.taskforge.issue.dto.CommentResponse;
import com.taskforge.issue.dto.CreateWorkLogRequest;
import com.taskforge.issue.dto.WorkLogResponse;
import com.taskforge.issue.dto.IssueHistoryResponse;
import com.taskforge.issue.service.IssueService;
import com.taskforge.issue.service.CommentService;
import com.taskforge.issue.service.WorkLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class IssueController {

    private final IssueService issueService;
    private final CommentService commentService;
    private final WorkLogService workLogService;

    @GetMapping("/issues")
    public List<IssueResponse> getAllIssues() {
        return issueService.getAllIssues();
    }

    @GetMapping("/projects/{projectIdOrKey}/issues")
    public List<IssueResponse> getProjectIssues(@PathVariable String projectIdOrKey) {
        return issueService.getProjectIssues(projectIdOrKey);
    }

    @GetMapping("/issues/{issueId}")
    public IssueResponse getIssue(@PathVariable UUID issueId) {
        return issueService.getIssue(issueId);
    }

    @GetMapping("/issues/my")
    public List<IssueResponse> getMyIssues() {
        return issueService.getMyIssues();
    }

    @GetMapping("/issues/recent")
    public List<IssueResponse> getRecentWork() {
        return issueService.getRecentWork();
    }

    @GetMapping("/issues/starred")
    public List<IssueResponse> getStarredIssues() {
        return issueService.getStarredIssues();
    }

    @PostMapping("/issues")
    @ResponseStatus(HttpStatus.CREATED)
    public IssueResponse createIssue(@RequestBody @Valid CreateIssueRequest request) {
        return issueService.createIssue(request);
    }

    @PatchMapping("/issues/{issueId}")
    public IssueResponse updateIssue(
            @PathVariable UUID issueId,
            @RequestBody @Valid UpdateIssueRequest request) {
        return issueService.updateIssue(issueId, request);
    }

    @DeleteMapping("/issues/{issueId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteIssue(@PathVariable UUID issueId) {
        issueService.deleteIssue(issueId);
    }

    @PutMapping("/issues/{issueId}/move")
    public IssueResponse moveIssue(
            @PathVariable UUID issueId,
            @RequestBody com.taskforge.issue.dto.MoveIssueRequest request) {
        return issueService.moveIssue(issueId, request);
    }

    // --- Details Endpoints ---

    @PostMapping("/issues/{issueId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse addComment(@PathVariable UUID issueId, @RequestBody @Valid CreateCommentRequest request) {
        return commentService.addComment(issueId, request.getText());
    }

    @GetMapping("/issues/{issueId}/comments")
    public List<CommentResponse> getComments(@PathVariable UUID issueId) {
        return commentService.getComments(issueId);
    }

    @PostMapping("/issues/{issueId}/worklogs")
    @ResponseStatus(HttpStatus.CREATED)
    public WorkLogResponse logWork(@PathVariable UUID issueId, @RequestBody @Valid CreateWorkLogRequest request) {
        return workLogService.logWork(issueId, request.getTimeSpentMinutes(), request.getStartedAt(),
                request.getDescription());
    }

    @GetMapping("/issues/{issueId}/worklogs")
    public List<WorkLogResponse> getWorkLogs(@PathVariable UUID issueId) {
        return workLogService.getWorkLogs(issueId);
    }

    @GetMapping("/issues/{issueId}/history")
    public List<IssueHistoryResponse> getHistory(@PathVariable UUID issueId) {
        return issueService.getHistory(issueId);
    }

    @GetMapping("/issues/{issueId}/activity")
    public List<com.taskforge.issue.dto.ActivityItem> getActivity(@PathVariable UUID issueId) {
        return issueService.getActivity(issueId);
    }
}
