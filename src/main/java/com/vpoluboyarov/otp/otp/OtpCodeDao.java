package com.vpoluboyarov.otp.otp;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

@Repository
public class OtpCodeDao {

    private final JdbcTemplate jdbcTemplate;

    public OtpCodeDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final RowMapper<OtpCode> ROW_MAPPER = (rs, rowNum) -> OtpCode.builder()
            .id(rs.getLong("id"))
            .userId(rs.getLong("user_id"))
            .operationId(rs.getString("operation_id"))
            .code(rs.getString("code"))
            .status(OtpStatus.valueOf(rs.getString("status")))
            .channel(OtpChannel.valueOf(rs.getString("channel")))
            .createdAt(rs.getObject("created_at", OffsetDateTime.class).toInstant())
            .updatedAt(rs.getObject("updated_at", OffsetDateTime.class).toInstant())
            .expiresAt(rs.getObject("expires_at", OffsetDateTime.class).toInstant())
            .build();

    public Long insert(OtpCode code) {
        String sql = """
                INSERT INTO otp_codes (user_id, operation_id, code, status, channel, expires_at)
                VALUES (?, ?, ?, ?, ?, ?)
                RETURNING id
                """;
        return jdbcTemplate.queryForObject(
                sql,
                Long.class,
                code.getUserId(),
                code.getOperationId(),
                code.getCode(),
                code.getStatus().name(),
                code.getChannel().name(),
                OffsetDateTime.ofInstant(code.getExpiresAt(), ZoneOffset.UTC)
        );
    }

    public Optional<OtpCode> findActiveByUserAndOperation(Long userId, String operationId) {
        String sql = """
                SELECT * FROM otp_codes
                WHERE user_id = ? AND operation_id = ? AND status = 'ACTIVE'
                """;
        return jdbcTemplate.query(sql, ROW_MAPPER, userId, operationId).stream().findFirst();
    }

    public Optional<OtpCode> findActiveCodeForValidation(Long userId, String operationId, String code) {
        String sql = """
                SELECT * FROM otp_codes
                WHERE user_id = ? AND operation_id = ? AND code = ?
                  AND status = 'ACTIVE' AND expires_at > now()
                """;
        return jdbcTemplate.query(sql, ROW_MAPPER, userId, operationId, code).stream().findFirst();
    }

    public int markAsUsed(Long codeId) {
        String sql = """
                UPDATE otp_codes
                SET status = 'USED', updated_at = now()
                WHERE id = ? AND status = 'ACTIVE'
                """;
        return jdbcTemplate.update(sql, codeId);
    }

    public int markExpired() {
        String sql = """
                UPDATE otp_codes
                SET status = 'EXPIRED', updated_at = now()
                WHERE status = 'ACTIVE' AND expires_at <= now()
                """;
        return jdbcTemplate.update(sql);
    }
}
