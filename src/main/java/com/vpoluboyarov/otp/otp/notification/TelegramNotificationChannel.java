package com.vpoluboyarov.otp.otp.notification;

import com.vpoluboyarov.otp.otp.OtpChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@PropertySource("classpath:telegram.properties")
public class TelegramNotificationChannel implements NotificationChannel {

    private final String botToken;
    private final String apiUrl;
    private final HttpClient httpClient;

    public TelegramNotificationChannel(
            @Value("${telegram.bot.token}") String botToken,
            @Value("${telegram.api.url}") String apiUrl) {
        this.botToken = botToken;
        this.apiUrl = apiUrl;
        this.httpClient = HttpClient.newHttpClient();
    }

    @Override
    public OtpChannel type() {
        return OtpChannel.TELEGRAM;
    }

    @Override
    public void sendCode(String destination, String code) {
        String text = String.format("Your verification code is: %s", code);
        String url = String.format("%s/bot%s/sendMessage?chat_id=%s&text=%s",
                apiUrl, botToken, destination, urlEncode(text));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.error("TELEGRAM channel: API error. Status={}, body={}",
                        response.statusCode(), response.body());
                throw new RuntimeException("Telegram API returned " + response.statusCode());
            }
            log.info("TELEGRAM channel: code sent to chat_id={}", destination);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("TELEGRAM channel: interrupted while sending to chat_id={}", destination, e);
            throw new RuntimeException("Interrupted while sending Telegram message", e);
        } catch (IOException e) {
            log.error("TELEGRAM channel: IO error sending to chat_id={}", destination, e);
            throw new RuntimeException("Failed to send Telegram message", e);
        }
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
