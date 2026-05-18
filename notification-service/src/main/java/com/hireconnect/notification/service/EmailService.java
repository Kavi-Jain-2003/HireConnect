package com.hireconnect.notification.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Sends styled HTML emails using JavaMailSender (SMTP via Gmail or any provider).
 *
 * Falls back to console logging if JavaMailSender is not configured,
 * so the service starts cleanly in dev/test without real SMTP credentials.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private static final String BRAND_COLOR = "#4F46E5"; // HireConnect indigo
    private static final String FROM_NAME   = "HireConnect";

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    public EmailService(ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.mailSenderProvider = mailSenderProvider;
    }

    // ── Public API ─────────────────────────────────────────────────────────

    /**
     * Send a plain text email (used by quick internal calls).
     */
    public void sendSimple(String to, String subject, String text) {
        send(to, subject, wrapPlainText(text));
    }

    /**
     * Application status changed — e.g. "SHORTLISTED", "INTERVIEW_SCHEDULED", "OFFERED".
     */
    public void sendApplicationStatusEmail(String to, String candidateName,
                                            String jobTitle, String newStatus) {
        String subject = "HireConnect — Application Update for " + jobTitle;
        String body = buildStatusEmailHtml(candidateName, jobTitle, newStatus);
        send(to, subject, body);
    }

    /**
     * Interview invitation email to candidate.
     */
    public void sendInterviewInvitationEmail(String to, String candidateName,
                                              String jobTitle, String scheduledAt,
                                              String mode, String meetLink) {
        String subject = "HireConnect — Interview Scheduled for " + jobTitle;
        String body = buildInterviewEmailHtml(candidateName, jobTitle, scheduledAt, mode, meetLink);
        send(to, subject, body);
    }

    /**
     * Welcome email after registration.
     */
    public void sendWelcomeEmail(String to, String name, String role) {
        String subject = "Welcome to HireConnect!";
        String body = buildWelcomeEmailHtml(name, role);
        send(to, subject, body);
    }

    // ── Internal send ──────────────────────────────────────────────────────

    private void send(String to, String subject, String htmlBody) {
        if (to == null || to.isBlank()) {
            log.warn("sendEmail skipped — recipient is blank");
            return;
        }

        JavaMailSender sender = mailSenderProvider.getIfAvailable();
        if (sender == null) {
            // Dev/test fallback — no SMTP configured
            log.info("==== EMAIL (no SMTP configured) ====");
            log.info("TO      : {}", to);
            log.info("SUBJECT : {}", subject);
            log.info("====================================");
            return;
        }

        try {
            MimeMessage mime = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, true, "UTF-8");
            helper.setFrom("noreply@hireconnect.io", FROM_NAME);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            sender.send(mime);
            log.info("Email sent → {} | {}", to, subject);
        } catch (MessagingException | java.io.UnsupportedEncodingException ex) {
            log.error("Failed to send email to {}: {}", to, ex.getMessage());
        }
    }

    // ── HTML templates ─────────────────────────────────────────────────────

    private String buildStatusEmailHtml(String name, String jobTitle, String status) {
        String statusLabel = formatStatus(status);
        String statusColor = statusColor(status);
        return baseTemplate(
                "Application Update",
                "<p>Hi <strong>" + esc(name) + "</strong>,</p>" +
                "<p>Your application for <strong>" + esc(jobTitle) + "</strong> has been updated.</p>" +
                "<p>New status: <span style='color:" + statusColor + ";font-weight:bold;font-size:16px;'>"
                + statusLabel + "</span></p>" +
                "<p>Log in to your dashboard to view full details and next steps.</p>"
        );
    }

    private String buildInterviewEmailHtml(String name, String jobTitle,
                                            String scheduledAt, String mode,
                                            String meetLink) {
        String linkHtml = (meetLink != null && !meetLink.isBlank())
                ? "<p><a href='" + esc(meetLink) + "' style='color:" + BRAND_COLOR + ";'>Join Meeting</a></p>"
                : "";
        return baseTemplate(
                "Interview Scheduled",
                "<p>Hi <strong>" + esc(name) + "</strong>,</p>" +
                "<p>Your interview for <strong>" + esc(jobTitle) + "</strong> has been scheduled.</p>" +
                "<table style='border-collapse:collapse;margin:16px 0;'>" +
                "<tr><td style='padding:4px 12px 4px 0;color:#6B7280;'>Date &amp; Time</td>" +
                "<td><strong>" + esc(scheduledAt) + "</strong></td></tr>" +
                "<tr><td style='padding:4px 12px 4px 0;color:#6B7280;'>Mode</td>" +
                "<td><strong>" + esc(mode) + "</strong></td></tr>" +
                "</table>" + linkHtml +
                "<p>Please confirm or reschedule from your candidate dashboard.</p>"
        );
    }

    private String buildWelcomeEmailHtml(String name, String role) {
        return baseTemplate(
                "Welcome to HireConnect!",
                "<p>Hi <strong>" + esc(name) + "</strong>,</p>" +
                "<p>Welcome to <strong>HireConnect</strong> — your platform for bridging talent with opportunity.</p>" +
                "<p>You're registered as a <strong>" + esc(role) + "</strong>. " +
                (role.equalsIgnoreCase("CANDIDATE")
                    ? "Start exploring job listings and applying today!"
                    : "Post your first job opening and find the right talent fast!") + "</p>"
        );
    }

    private String wrapPlainText(String text) {
        return baseTemplate("Notification", "<p>" + esc(text) + "</p>");
    }

    private String baseTemplate(String title, String content) {
        return "<!DOCTYPE html><html><body style='margin:0;padding:0;background:#F9FAFB;font-family:Arial,sans-serif;'>" +
               "<table width='100%' cellpadding='0' cellspacing='0'><tr><td align='center' style='padding:40px 0;'>" +
               "<table width='600' cellpadding='0' cellspacing='0' style='background:#fff;border-radius:8px;" +
               "box-shadow:0 1px 3px rgba(0,0,0,.1);overflow:hidden;'>" +
               // header
               "<tr><td style='background:" + BRAND_COLOR + ";padding:24px 32px;'>" +
               "<h1 style='margin:0;color:#fff;font-size:22px;'>HireConnect</h1>" +
               "<p style='margin:4px 0 0;color:#C7D2FE;font-size:13px;'>Bridging Talent with Opportunity</p>" +
               "</td></tr>" +
               // body
               "<tr><td style='padding:32px;color:#374151;font-size:15px;line-height:1.6;'>" +
               "<h2 style='color:#111827;margin-top:0;'>" + esc(title) + "</h2>" +
               content +
               "</td></tr>" +
               // footer
               "<tr><td style='background:#F3F4F6;padding:16px 32px;text-align:center;" +
               "color:#9CA3AF;font-size:12px;'>" +
               "© 2026 HireConnect · <a href='#' style='color:#9CA3AF;'>Unsubscribe</a>" +
               "</td></tr>" +
               "</table></td></tr></table></body></html>";
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private String formatStatus(String status) {
        if (status == null) return "Updated";
        return switch (status.toUpperCase()) {
            case "APPLIED"              -> "Applied ✓";
            case "SHORTLISTED"          -> "Shortlisted 🎯";
            case "INTERVIEW_SCHEDULED"  -> "Interview Scheduled 📅";
            case "OFFERED"              -> "Offered 🎉";
            case "REJECTED"             -> "Not Selected";
            default                     -> status;
        };
    }

    private String statusColor(String status) {
        if (status == null) return "#374151";
        return switch (status.toUpperCase()) {
            case "SHORTLISTED", "INTERVIEW_SCHEDULED" -> "#059669"; // green
            case "OFFERED"                            -> "#7C3AED"; // purple
            case "REJECTED"                           -> "#DC2626"; // red
            default                                   -> "#2563EB"; // blue
        };
    }
}
