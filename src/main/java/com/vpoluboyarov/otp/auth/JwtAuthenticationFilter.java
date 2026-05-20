package com.vpoluboyarov.otp.auth;

import com.vpoluboyarov.otp.shared.AuthenticatedUser;
import com.vpoluboyarov.otp.shared.SecurityContext;
import com.vpoluboyarov.otp.shared.UnauthorizedException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.util.Set;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/api/auth/register",
            "/api/auth/login",
            "/api/ping"
    );

    private final JwtService jwtService;
    private final HandlerExceptionResolver resolver;

    public JwtAuthenticationFilter(JwtService jwtService,
                                   @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
        this.jwtService = jwtService;
        this.resolver = resolver;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String path = request.getRequestURI();

        if (PUBLIC_PATHS.contains(path)) {
            chain.doFilter(request, response);
            return;
        }

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            resolver.resolveException(request, response, null,
                    new UnauthorizedException("missing or invalid Authorization header"));
            return;
        }

        String token = header.substring(BEARER_PREFIX.length()).trim();

        try {
            AuthenticatedUser user = jwtService.parse(token);
            try {
                SecurityContext.set(user);
                log.debug("Authenticated: id={}, login='{}', role={}", user.id(), user.login(), user.role());
                chain.doFilter(request, response);
            } finally {
                SecurityContext.clear();
            }
        } catch (UnauthorizedException e) {
            resolver.resolveException(request, response, null, e);
        }
    }
}
