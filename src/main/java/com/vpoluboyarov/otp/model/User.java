package com.vpoluboyarov.otp.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    private Long id;
    private String login;
    private String passwordHash;
    private Role role;
    private String email;
    private String phone;
    private String telegramChatId;
    private Instant createdAt;
    private Instant updatedAt;
}
