package com.taskforge.page.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class UpdatePageRequest {
    private String title;
    private String content;
    private String icon;
    private UUID parentId;
}
