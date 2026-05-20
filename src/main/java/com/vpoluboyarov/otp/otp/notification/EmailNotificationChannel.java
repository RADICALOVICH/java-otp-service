package com.vpoluboyarov.otp.otp.notification;

import com.vpoluboyarov.otp.otp.OtpChannel;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import java.util.Properties;

@Slf4j
@Component
@PropertySource("classpath:email.properties")
public class EmailNotificationChannel implements NotificationChannel {

    private final String fromEmail;
    private final Session session;

    public EmailNotificationChannel(
            @Value("${email.username}") String username,
            @Value("${email.password}") String password,
            @Value("${email.from}") String fromEmail,
            @Value("${mail.smtp.host}") String smtpHost,
            @Value("${mail.smtp.port}") String smtpPort,
            @Value("${mail.smtp.auth}") String smtpAuth,
            @Value("${mail.smtp.starttls.enable}") String smtpStarttls) {

        this.fromEmail = fromEmail;

        Properties mailProps = new Properties();
        mailProps.put("mail.smtp.host", smtpHost);
        mailProps.put("mail.smtp.port", smtpPort);
        mailProps.put("mail.smtp.auth", smtpAuth);
        mailProps.put("mail.smtp.starttls.enable", smtpStarttls);

        this.session = Session.getInstance(mailProps, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });
    }

    @Override
    public OtpChannel type() {
        return OtpChannel.EMAIL;
    }

    @Override
    public void sendCode(String destination, String code) {
        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromEmail));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(destination));
            message.setSubject("Your OTP Code");
            message.setText("Your verification code is: " + code);
            Transport.send(message);
            log.info("EMAIL channel: code sent to {}", destination);
        } catch (MessagingException e) {
            log.error("EMAIL channel: failed to send to {}", destination, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }
}
