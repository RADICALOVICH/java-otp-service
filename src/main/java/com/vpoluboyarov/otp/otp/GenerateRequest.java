package com.vpoluboyarov.otp.otp;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GenerateRequest {

    @NotBlank
    @Size(max = 100)
    private String operationId;

    @NotNull
    private OtpChannel channel;
}
