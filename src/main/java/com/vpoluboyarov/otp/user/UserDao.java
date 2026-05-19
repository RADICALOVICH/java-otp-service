package com.vpoluboyarov.otp.user;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;

@Repository
public class UserDao {

    private final JdbcTemplate jdbcTemplate;

    public UserDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final RowMapper<User> ROW_MAPPER = (rs, rowNum) -> User.builder()
            .id(rs.getLong("id"))
            .login(rs.getString("login"))
            .passwordHash(rs.getString("password_hash"))
            .role(Role.valueOf(rs.getString("role")))
            .email(rs.getString("email"))
            .phone(rs.getString("phone"))
            .telegramChatId(rs.getString("telegram_chat_id"))
            .createdAt(rs.getObject("created_at", OffsetDateTime.class).toInstant())
            .updatedAt(rs.getObject("updated_at", OffsetDateTime.class).toInstant())
            .build();

    public Long insert(User user) {
        String sql = """
                INSERT INTO users (login, password_hash, role, email, phone, telegram_chat_id)
                VALUES (?, ?, ?, ?, ?, ?)
                RETURNING id
                """;
        return jdbcTemplate.queryForObject(
                sql,
                Long.class,
                user.getLogin(),
                user.getPasswordHash(),
                user.getRole().name(),
                user.getEmail(),
                user.getPhone(),
                user.getTelegramChatId()
        );
    }

    public Optional<User> findByLogin(String login) {
        String sql = "SELECT * FROM users WHERE login = ?";
        return jdbcTemplate.query(sql, ROW_MAPPER, login).stream().findFirst();
    }

    public boolean adminExists() {
        String sql = "SELECT EXISTS(SELECT 1 FROM users WHERE role = 'ADMIN')";
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(sql, Boolean.class));
    }
}
