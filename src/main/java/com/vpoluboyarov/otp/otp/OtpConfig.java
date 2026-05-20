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
public class OtpConfig {

    private int codeLength;
    private int ttlSeconds;
    private Instant updatedAt;
}
