package com.taskforge.common.service;

public interface EmailService {
    void sendInvitationEmail(String to, String inviteUrl);

    void sendMembershipAddedEmail(String to, String tenantName);

    void sendMemberRemovedEmail(String to, String tenantName);

    void sendInvitationRevokedEmail(String to, String tenantName);
}
