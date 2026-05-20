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
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.Properties;

@Slf4j
@Component
public class EmailNotificationChannel implements NotificationChannel {

    private final String username;
    private final String password;
    private final String fromEmail;
    private final Session session;

    public EmailNotificationChannel() {
        Properties config = loadConfig();
        this.username = config.getProperty("email.username");
        this.password = config.getProperty("email.password");
        this.fromEmail = config.getProperty("email.from");
        this.session = Session.getInstance(config, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });
    }

    private Properties loadConfig() {
        Properties props = new Properties();
        try (InputStream is = EmailNotificationChannel.class.getClassLoader()
                .getResourceAsStream("email.properties")) {
            if (is == null) {
                throw new IllegalStateException("email.properties not found in classpath");
            }
            props.load(is);
            return props;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load email configuration", e);
        }
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
