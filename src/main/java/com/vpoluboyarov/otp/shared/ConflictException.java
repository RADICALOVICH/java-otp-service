package com.vpoluboyarov.otp.shared;

public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
