package com.vpoluboyarov.otp.admin;

import com.vpoluboyarov.otp.auth.AdminOnly;
import com.vpoluboyarov.otp.otp.OtpConfig;
import com.vpoluboyarov.otp.user.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @AdminOnly
    @GetMapping("/config")
    public OtpConfig getConfig() {
        return adminService.getConfig();
    }

    @AdminOnly
    @PatchMapping("/config")
    public OtpConfig updateConfig(@Valid @RequestBody UpdateConfigRequest request) {
        return adminService.updateConfig(request);
    }

    @AdminOnly
    @GetMapping("/users")
    public List<UserResponse> listUsers() {
        return adminService.listNonAdmins();
    }

    @AdminOnly
    @DeleteMapping("/users/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable Long id) {
        adminService.deleteUser(id);
    }
}
