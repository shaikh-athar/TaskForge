package com.taskforge.issue.service;

import com.taskforge.common.exception.ApiException;
import com.taskforge.common.tenant.TenantContext;
import com.taskforge.issue.dto.CommentResponse;
import com.taskforge.issue.model.Comment;
import com.taskforge.issue.repository.CommentRepository;
import com.taskforge.issue.repository.IssueRepository;
import com.taskforge.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final IssueRepository issueRepository;
    private final UserRepository userRepository;

    @Transactional
    public CommentResponse addComment(UUID issueId, String text) {
        UUID tenantId = TenantContext.get();
        if (tenantId == null)
            throw new ApiException("TENANT_CONTEXT_MISSING", "Tenant context missing");

        if (!issueRepository.existsByTenantIdAndId(tenantId, issueId)) {
            throw new ApiException("ISSUE_NOT_FOUND", "Issue not found");
        }

        Comment comment = new Comment();
        comment.setIssueId(issueId);
        comment.setUserId(getCurrentUserId());
        comment.setText(text);
        comment.setCreatedBy(comment.getUserId());
        comment.setUpdatedBy(comment.getUserId());

        comment = commentRepository.save(comment);
        return mapToResponse(comment);
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> getComments(UUID issueId) {
        UUID tenantId = TenantContext.get();
        if (tenantId == null)
            throw new ApiException("TENANT_CONTEXT_MISSING", "Tenant context missing");

        if (!issueRepository.existsByTenantIdAndId(tenantId, issueId)) {
            throw new ApiException("ISSUE_NOT_FOUND", "Issue not found");
        }

        return commentRepository.findByIssueIdOrderByCreatedAtAsc(issueId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    private CommentResponse mapToResponse(Comment comment) {
        CommentResponse response = new CommentResponse();
        response.setId(comment.getId());
        response.setIssueId(comment.getIssueId());
        response.setUserId(comment.getUserId());
        response.setText(comment.getText());
        response.setCreatedAt(comment.getCreatedAt());
        response.setUpdatedAt(comment.getUpdatedAt());

        userRepository.findById(comment.getUserId()).ifPresent(user -> {
            response.setUserName(user.getDisplayName());
            response.setUserAvatarUrl(user.getAvatarUrl());
        });

        return response;
    }

    private UUID getCurrentUserId() {
        Jwt jwt = (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return UUID.fromString(jwt.getSubject());
    }
}
