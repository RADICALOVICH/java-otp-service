package com.vpoluboyarov.otp.admin;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateConfigRequest {

    @Min(4)
    @Max(10)
    private Integer codeLength;

    @Positive
    private Integer ttlSeconds;
}
