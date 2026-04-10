package com.taskforge.common.service.impl;

import com.taskforge.common.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmtpEmailService implements EmailService {

    private final JavaMailSender mailSender;

    @Override
    @Async
    public void sendInvitationEmail(String to, String inviteUrl) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("You have been invited to join tenant on TaskForge");
            message.setText("You have been invited to join a tenant workspace on TaskForge.\n\n" +
                    "Click the link below to accept the invitation:\n" + inviteUrl + "\n\n" +
                    "This link will expire in 7 days.");
            mailSender.send(message);
            log.info("Invitation email sent to {}", to);
        } catch (Exception e) {
            log.error("Failed to send invitation email to {}", to, e);
        }
    }

    @Override
    @Async
    public void sendMembershipAddedEmail(String to, String tenantName) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("You have been added to a tenant on TaskForge");
            message.setText("You have been added to the tenant workspace '" + tenantName + "' on TaskForge.\n\n" +
                    "Log in to your dashboard to view the workspace.");
            mailSender.send(message);
            log.info("Membership added email sent to {}", to);
        } catch (Exception e) {
            log.error("Failed to send membership added email to {}", to, e);
        }
    }

    @Override
    @Async
    public void sendMemberRemovedEmail(String to, String tenantName) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("You have been removed from a tenant on TaskForge");
            message.setText("You have been removed from the tenant workspace '" + tenantName + "' on TaskForge.\n\n" +
                    "If you believe this is a mistake, please contact the workspace administrator.");
            mailSender.send(message);
            log.info("Member removed email sent to {}", to);
        } catch (Exception e) {
            log.error("Failed to send member removed email to {}", to, e);
        }
    }

    @Override
    @Async
    public void sendInvitationRevokedEmail(String to, String tenantName) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("Invitation Revoked - TaskForge");
            message.setText("Your invitation to join the tenant workspace '" + tenantName
                    + "' on TaskForge has been revoked.\n\n" +
                    "If you believe this is a mistake, please contact the workspace administrator.");
            mailSender.send(message);
            log.info("Invitation revoked email sent to {}", to);
        } catch (Exception e) {
            log.error("Failed to send invitation revoked email to {}", to, e);
        }
    }
}
