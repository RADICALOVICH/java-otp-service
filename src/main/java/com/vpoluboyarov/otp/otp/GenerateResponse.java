package com.vpoluboyarov.otp.otp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateResponse {

    private Long codeId;
    private String operationId;
    private OtpChannel channel;
    private Instant expiresAt;
}
