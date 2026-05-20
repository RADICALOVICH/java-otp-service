package com.vpoluboyarov.otp.otp.notification;

import com.vpoluboyarov.otp.otp.OtpChannel;

public interface NotificationChannel {

    OtpChannel type();

    void sendCode(String destination, String code);
}
