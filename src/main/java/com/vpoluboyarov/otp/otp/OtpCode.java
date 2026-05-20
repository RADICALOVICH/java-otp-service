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
public class OtpCode {

    private Long id;
    private Long userId;
    private String operationId;
    private String code;
    private OtpStatus status;
    private OtpChannel channel;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant expiresAt;
}
