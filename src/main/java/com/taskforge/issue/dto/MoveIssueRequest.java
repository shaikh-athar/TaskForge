package com.taskforge.issue.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class MoveIssueRequest {
    private UUID sprintId;
    private Integer targetIndex;
}
