package com.vpoluboyarov.otp.otp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OtpExpiryScheduler {

    private final OtpCodeDao otpCodeDao;

    public OtpExpiryScheduler(OtpCodeDao otpCodeDao) {
        this.otpCodeDao = otpCodeDao;
    }

    @Scheduled(fixedDelayString = "${otp.scheduler.expire-interval-ms}")
    public void markExpired() {
        int updated = otpCodeDao.markExpired();
        if (updated > 0) {
            log.info("Scheduler: marked {} OTP code(s) as EXPIRED", updated);
        } else {
            log.debug("Scheduler: no expired codes to mark");
        }
    }
}
