package com.vpoluboyarov.otp.otp;

import com.vpoluboyarov.otp.otp.notification.NotificationChannel;
import com.vpoluboyarov.otp.shared.AuthenticatedUser;
import com.vpoluboyarov.otp.shared.BadRequestException;
import com.vpoluboyarov.otp.shared.ConflictException;
import com.vpoluboyarov.otp.shared.SecurityContext;
import com.vpoluboyarov.otp.user.User;
import com.vpoluboyarov.otp.user.UserDao;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OtpService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserDao userDao;
    private final OtpCodeDao otpCodeDao;
    private final OtpConfigDao otpConfigDao;
    private final Map<OtpChannel, NotificationChannel> channels;

    public OtpService(UserDao userDao,
                      OtpCodeDao otpCodeDao,
                      OtpConfigDao otpConfigDao,
                      List<NotificationChannel> channelList) {
        this.userDao = userDao;
        this.otpCodeDao = otpCodeDao;
        this.otpConfigDao = otpConfigDao;
        this.channels = channelList.stream()
                .collect(Collectors.toMap(NotificationChannel::type, c -> c));
    }

    public GenerateResponse generate(GenerateRequest request) {
        AuthenticatedUser current = SecurityContext.require();
        log.info("OTP generate: user='{}', operationId='{}', channel={}",
                current.login(), request.getOperationId(), request.getChannel());

        if (otpCodeDao.findActiveByUserAndOperation(current.id(), request.getOperationId()).isPresent()) {
            log.warn("OTP rejected: active code exists for user='{}', operationId='{}'",
                    current.login(), request.getOperationId());
            throw new ConflictException("active code already exists for this operation");
        }

        User user = userDao.findById(current.id())
                .orElseThrow(() -> new IllegalStateException("user vanished: id=" + current.id()));

        String destination = resolveDestination(user, request.getChannel());

        OtpConfig config = otpConfigDao.get();
        String code = generateNumericCode(config.getCodeLength());
        Instant expiresAt = Instant.now().plusSeconds(config.getTtlSeconds());

        OtpCode otpCode = OtpCode.builder()
                .userId(current.id())
                .operationId(request.getOperationId())
                .code(code)
                .status(OtpStatus.ACTIVE)
                .channel(request.getChannel())
                .expiresAt(expiresAt)
                .build();
        Long codeId = otpCodeDao.insert(otpCode);

        NotificationChannel channel = channels.get(request.getChannel());
        channel.sendCode(destination, code);

        log.info("OTP generated: codeId={}, user='{}', operationId='{}', channel={}, expiresAt={}",
                codeId, current.login(), request.getOperationId(), request.getChannel(), expiresAt);

        return GenerateResponse.builder()
                .codeId(codeId)
                .operationId(request.getOperationId())
                .channel(request.getChannel())
                .expiresAt(expiresAt)
                .build();
    }

    public void validate(ValidateRequest request) {
        AuthenticatedUser current = SecurityContext.require();
        log.info("OTP validate: user='{}', operationId='{}'", current.login(), request.getOperationId());

        Optional<OtpCode> codeOpt = otpCodeDao.findActiveCodeForValidation(
                current.id(), request.getOperationId(), request.getCode());

        if (codeOpt.isEmpty()) {
            log.warn("OTP validate rejected: invalid/expired code for user='{}', operationId='{}'",
                    current.login(), request.getOperationId());
            throw new BadRequestException("invalid or expired code");
        }

        OtpCode otpCode = codeOpt.get();
        int updated = otpCodeDao.markAsUsed(otpCode.getId());
        if (updated == 0) {
            log.warn("OTP validate race: code id={} already used by a parallel request", otpCode.getId());
            throw new BadRequestException("invalid or expired code");
        }

        log.info("OTP validated: codeId={}, user='{}', operationId='{}'",
                otpCode.getId(), current.login(), request.getOperationId());
    }

    private String resolveDestination(User user, OtpChannel channel) {
        String dest = switch (channel) {
            case EMAIL -> user.getEmail();
            case SMS -> user.getPhone();
            case TELEGRAM -> user.getTelegramChatId();
            case FILE -> user.getLogin();
        };
        if (dest == null || dest.isBlank()) {
            throw new BadRequestException("contact for channel " + channel + " is not set in profile");
        }
        return dest;
    }

    private String generateNumericCode(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }
}
