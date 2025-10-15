package iuh.fit.se.sebook_backend.controller;

import iuh.fit.se.sebook_backend.dto.AuthenticationRequest;
import iuh.fit.se.sebook_backend.dto.AuthenticationResponse;
import iuh.fit.se.sebook_backend.dto.RegisterRequest;
import iuh.fit.se.sebook_backend.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
        String token = authService.login(request);
        return ResponseEntity.ok(new AuthenticationResponse(token));
    }
}