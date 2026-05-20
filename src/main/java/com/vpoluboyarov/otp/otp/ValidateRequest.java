package com.vpoluboyarov.otp.otp;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidateRequest {

    @NotBlank
    @Size(max = 100)
    private String operationId;

    @NotBlank
    @Size(max = 10)
    private String code;
}
