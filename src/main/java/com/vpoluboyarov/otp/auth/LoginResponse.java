package com.vpoluboyarov.otp.auth;

import com.vpoluboyarov.otp.user.UserResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private String token;
    private Instant expiresAt;
    private UserResponse user;
}
