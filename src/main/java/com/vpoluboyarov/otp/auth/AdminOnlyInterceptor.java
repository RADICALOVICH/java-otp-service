package com.vpoluboyarov.otp.auth;

import com.vpoluboyarov.otp.shared.AuthenticatedUser;
import com.vpoluboyarov.otp.shared.ForbiddenException;
import com.vpoluboyarov.otp.shared.SecurityContext;
import com.vpoluboyarov.otp.user.Role;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AdminOnlyInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod hm)) {
            return true;
        }
        AdminOnly annotation = hm.getMethodAnnotation(AdminOnly.class);
        if (annotation == null) {
            return true;
        }
        AuthenticatedUser user = SecurityContext.require();
        if (user.role() != Role.ADMIN) {
            throw new ForbiddenException("admin only");
        }
        return true;
    }
}
