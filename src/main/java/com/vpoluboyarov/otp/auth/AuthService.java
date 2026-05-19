package com.vpoluboyarov.otp.auth;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.vpoluboyarov.otp.shared.ConflictException;
import com.vpoluboyarov.otp.user.Role;
import com.vpoluboyarov.otp.user.User;
import com.vpoluboyarov.otp.user.UserDao;
import com.vpoluboyarov.otp.user.UserResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AuthService {

    private static final int BCRYPT_COST = 10;

    private final UserDao userDao;

    public AuthService(UserDao userDao) {
        this.userDao = userDao;
    }

    public UserResponse register(RegisterRequest request) {
        log.info("Register attempt: login='{}', role={}", request.getLogin(), request.getRole());

        if (userDao.findByLogin(request.getLogin()).isPresent()) {
            log.warn("Register rejected: login '{}' already taken", request.getLogin());
            throw new ConflictException("login already taken");
        }
        if (request.getRole() == Role.ADMIN && userDao.adminExists()) {
            log.warn("Register rejected: admin already exists (attempt by '{}')", request.getLogin());
            throw new ConflictException("admin already exists");
        }

        String passwordHash = BCrypt.withDefaults()
                .hashToString(BCRYPT_COST, request.getPassword().toCharArray());

        User user = User.builder()
                .login(request.getLogin())
                .passwordHash(passwordHash)
                .role(request.getRole())
                .email(request.getEmail())
                .phone(request.getPhone())
                .telegramChatId(request.getTelegramChatId())
                .build();

        Long id = userDao.insert(user);
        log.info("User registered: id={}, login='{}', role={}", id, user.getLogin(), user.getRole());

        return UserResponse.builder()
                .id(id)
                .login(user.getLogin())
                .role(user.getRole())
                .email(user.getEmail())
                .phone(user.getPhone())
                .telegramChatId(user.getTelegramChatId())
                .build();
    }
}
