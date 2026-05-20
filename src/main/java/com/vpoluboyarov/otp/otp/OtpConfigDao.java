package com.vpoluboyarov.otp.otp;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;

@Repository
public class OtpConfigDao {

    private final JdbcTemplate jdbcTemplate;

    public OtpConfigDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final RowMapper<OtpConfig> ROW_MAPPER = (rs, rowNum) -> OtpConfig.builder()
            .codeLength(rs.getInt("code_length"))
            .ttlSeconds(rs.getInt("ttl_seconds"))
            .updatedAt(rs.getObject("updated_at", OffsetDateTime.class).toInstant())
            .build();

    public OtpConfig get() {
        String sql = "SELECT code_length, ttl_seconds, updated_at FROM otp_config WHERE id = 1";
        return jdbcTemplate.queryForObject(sql, ROW_MAPPER);
    }
}
