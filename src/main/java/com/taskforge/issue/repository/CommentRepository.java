package com.taskforge.issue.repository;

import com.taskforge.issue.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CommentRepository extends JpaRepository<Comment, UUID> {
    List<Comment> findByIssueIdOrderByCreatedAtAsc(UUID issueId);

    List<Comment> findByIssueIdOrderByCreatedAtDesc(UUID issueId);
}
