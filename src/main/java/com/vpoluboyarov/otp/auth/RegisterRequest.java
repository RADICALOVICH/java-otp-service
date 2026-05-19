package com.vpoluboyarov.otp.auth;

import com.vpoluboyarov.otp.user.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank
    @Size(max = 50)
    private String login;

    @NotBlank
    @Size(min = 6, max = 72)
    private String password;

    @NotNull
    private Role role;

    private String email;
    private String phone;
    private String telegramChatId;
}
