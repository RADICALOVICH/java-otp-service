package com.vpoluboyarov.otp.shared;

import com.vpoluboyarov.otp.user.Role;

public record AuthenticatedUser(Long id, String login, Role role) {
}
