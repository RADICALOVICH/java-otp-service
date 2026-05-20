package com.vpoluboyarov.otp.otp;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/otp")
public class OtpController {

    private final OtpService otpService;

    public OtpController(OtpService otpService) {
        this.otpService = otpService;
    }

    @PostMapping("/generate")
    public ResponseEntity<GenerateResponse> generate(@Valid @RequestBody GenerateRequest request) {
        GenerateResponse response = otpService.generate(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/validate")
    public ResponseEntity<Map<String, String>> validate(@Valid @RequestBody ValidateRequest request) {
        otpService.validate(request);
        return ResponseEntity.ok(Map.of("status", "OK"));
    }
}
