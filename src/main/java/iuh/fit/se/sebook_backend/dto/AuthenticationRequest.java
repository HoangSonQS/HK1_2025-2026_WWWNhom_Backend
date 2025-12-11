package iuh.fit.se.sebook_backend.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthenticationRequest {
    /**
     * Hỗ trợ đăng nhập bằng username hoặc email.
     * Chấp nhận các alias: username, email, identifier, login.
     */
    @JsonAlias({"username", "email", "identifier", "login"})
    private String username;
    private String password;
}