package com.vpoluboyarov.otp.service;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.vpoluboyarov.otp.dao.UserDao;
import com.vpoluboyarov.otp.dto.RegisterRequest;
import com.vpoluboyarov.otp.dto.UserResponse;
import com.vpoluboyarov.otp.exception.ConflictException;
import com.vpoluboyarov.otp.model.Role;
import com.vpoluboyarov.otp.model.User;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private static final int BCRYPT_COST = 10;

    private final UserDao userDao;

    public AuthService(UserDao userDao) {
        this.userDao = userDao;
    }

    public UserResponse register(RegisterRequest request) {
        if (userDao.findByLogin(request.getLogin()).isPresent()) {
            throw new ConflictException("login already taken");
        }
        if (request.getRole() == Role.ADMIN && userDao.adminExists()) {
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
