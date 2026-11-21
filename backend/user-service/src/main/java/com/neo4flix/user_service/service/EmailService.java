
package com.neo4flix.user_service.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final String from;

    public EmailService(
            JavaMailSender mailSender,
            @Value("${spring.mail.username}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    public void sendLoginOtp(String recipient, String username, String otp) {

        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(from);
        message.setTo(recipient);
        message.setSubject("Neo4flix - Login Verification Code");

        message.setText(
                "Hello " + username + ",\n\n"
                + "Your Neo4flix verification code is: " + otp + "\n\n"
                + "This code expires in 5 minutes.\n"
                + "You have a maximum of 5 attempts.\n\n"
                + "If you did not try to log in, please ignore this email.\n\n"
                + "Neo4flix Security Team"
        );

        mailSender.send(message);
    }
}
