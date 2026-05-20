package com.vpoluboyarov.otp.admin;

import com.vpoluboyarov.otp.otp.OtpConfig;
import com.vpoluboyarov.otp.otp.OtpConfigDao;
import com.vpoluboyarov.otp.shared.AuthenticatedUser;
import com.vpoluboyarov.otp.shared.BadRequestException;
import com.vpoluboyarov.otp.shared.ForbiddenException;
import com.vpoluboyarov.otp.shared.NotFoundException;
import com.vpoluboyarov.otp.shared.SecurityContext;
import com.vpoluboyarov.otp.user.UserDao;
import com.vpoluboyarov.otp.user.UserResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class AdminService {

    private final OtpConfigDao otpConfigDao;
    private final UserDao userDao;

    public AdminService(OtpConfigDao otpConfigDao, UserDao userDao) {
        this.otpConfigDao = otpConfigDao;
        this.userDao = userDao;
    }

    public OtpConfig getConfig() {
        log.debug("Admin: get config");
        return otpConfigDao.get();
    }

    public OtpConfig updateConfig(UpdateConfigRequest request) {
        if (request.getCodeLength() == null && request.getTtlSeconds() == null) {
            throw new BadRequestException("at least one of codeLength or ttlSeconds must be provided");
        }
        log.info("Admin: update config codeLength={}, ttlSeconds={}",
                request.getCodeLength(), request.getTtlSeconds());
        OtpConfig updated = otpConfigDao.update(request.getCodeLength(), request.getTtlSeconds());
        log.info("Admin: config updated to codeLength={}, ttlSeconds={}",
                updated.getCodeLength(), updated.getTtlSeconds());
        return updated;
    }

    public List<UserResponse> listNonAdmins() {
        log.debug("Admin: list non-admin users");
        return userDao.findAllNonAdmins().stream()
                .map(UserResponse::from)
                .toList();
    }

    public void deleteUser(Long id) {
        AuthenticatedUser current = SecurityContext.require();
        log.info("Admin: delete user id={} by admin id={} login='{}'", id, current.id(), current.login());

        if (current.id().equals(id)) {
            log.warn("Admin: rejected self-deletion attempt id={}", id);
            throw new ForbiddenException("cannot delete yourself");
        }

        int deleted = userDao.deleteById(id);
        if (deleted == 0) {
            log.warn("Admin: user not found id={}", id);
            throw new NotFoundException("user not found");
        }

        log.info("Admin: deleted user id={}", id);
    }
}
