package iuh.fit.se.sebook_backend.controller;

import iuh.fit.se.sebook_backend.dto.*;
import iuh.fit.se.sebook_backend.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.ok("User registered successfully");
    }

    @PostMapping("/token")
    public ResponseEntity<AuthenticationResponse> login(@RequestBody AuthenticationRequest request) {
        AuthenticationResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthenticationResponse> refreshToken(@RequestBody RefreshTokenRequest request) {
        AuthenticationResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(response);
    }

    /**
     * UC003: Đăng xuất - Vô hiệu hóa Refresh Token
     */
    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestBody RefreshTokenRequest request) {
        authService.logout(request.getRefreshToken());
        return ResponseEntity.ok("Logged out successfully");
    }

    /**
     * UC004: Đổi mật khẩu
     */
    @PostMapping("/change-password")
    public ResponseEntity<String> changePassword(@RequestBody ChangePasswordRequest request) {
        authService.changePassword(request);
        return ResponseEntity.ok("Password changed successfully");
    }

    /**
     * UC005: Gửi email đặt lại mật khẩu
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        authService.sendPasswordResetEmail(request);
        return ResponseEntity.ok("Password reset email sent successfully");
    }

    /**
     * UC005: Đặt lại mật khẩu bằng token
     */
    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok("Password reset successfully");
    }
}