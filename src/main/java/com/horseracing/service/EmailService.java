package com.horseracing.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendResetTokenEmail(String toEmail, String token) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Password Reset Request - Horse Racing System");
        message.setText("Hello,\n\n" +
                "You requested to reset your password. Please use the verification code below to continue:\n\n" +
                "VERIFICATION CODE: " + token + "\n\n" +
                "Note: This verification code will expire in 15 minutes.\n\n" +
                "Best regards,\n" +
                "Horse Racing System Administration");

        mailSender.send(message);
    }
}
