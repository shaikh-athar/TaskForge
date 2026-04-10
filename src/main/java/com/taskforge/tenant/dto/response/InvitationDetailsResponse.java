package com.taskforge.tenant.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InvitationDetailsResponse {
    private String email;
    private String role;
    private String tenantName;
    private String inviterName;
    private String inviterEmail;
}
