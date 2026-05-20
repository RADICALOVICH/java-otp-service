package com.vpoluboyarov.otp.otp.notification;

import com.vpoluboyarov.otp.otp.OtpChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;

@Slf4j
@Component
public class FileNotificationChannel implements NotificationChannel {

    private final Path filePath;

    public FileNotificationChannel(@Value("${otp.file.path}") String filePath) {
        this.filePath = Path.of(filePath);
    }

    @Override
    public OtpChannel type() {
        return OtpChannel.FILE;
    }

    @Override
    public void sendCode(String destination, String code) {
        String line = String.format("%s | user='%s' | code=%s%n", Instant.now(), destination, code);
        try {
            Files.writeString(filePath, line, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            log.info("FILE channel: code written for user '{}'", destination);
        } catch (IOException e) {
            log.error("FILE channel: failed to write code for user '{}'", destination, e);
            throw new RuntimeException("Failed to write code to file", e);
        }
    }
}
