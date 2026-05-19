package com.vpoluboyarov.otp.shared;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        long startMs = System.currentTimeMillis();
        try {
            chain.doFilter(request, response);
        } finally {
            long durationMs = System.currentTimeMillis() - startMs;
            log.info("{} {} -> {} ({} ms)",
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    durationMs);
        }
    }
}
