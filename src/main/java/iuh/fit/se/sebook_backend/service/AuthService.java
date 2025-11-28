package iuh.fit.se.sebook_backend.service;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import iuh.fit.se.sebook_backend.dto.*;
import iuh.fit.se.sebook_backend.entity.Account;
import iuh.fit.se.sebook_backend.entity.PasswordResetToken;
import iuh.fit.se.sebook_backend.entity.RefreshToken;
import iuh.fit.se.sebook_backend.entity.Role;
import iuh.fit.se.sebook_backend.repository.AccountRepository;
import iuh.fit.se.sebook_backend.repository.PasswordResetTokenRepository;
import iuh.fit.se.sebook_backend.repository.RefreshTokenRepository;
import iuh.fit.se.sebook_backend.repository.RoleRepository;
import iuh.fit.se.sebook_backend.utils.EmailSenderUtil;
import iuh.fit.se.sebook_backend.utils.SecurityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.UUID;

@Service
public class AuthService {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;


    @Value("${jwt.signerKey}")
    private String SIGNER_KEY;

    @Value("${jwt.valid-duration}")
    private long VALID_DURATION; // 1800 giây (30 phút)

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private EmailSenderUtil emailSenderUtil;

    @Autowired
    private SecurityUtil securityUtil;

    public void register(RegisterRequest request) {
        if (accountRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Tên đăng nhập đã tồn tại");
        }
        
        if (accountRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email đã được sử dụng");
        }

        Account account = new Account();
        account.setUsername(request.getUsername());
        account.setEmail(request.getEmail());
        account.setPassword(passwordEncoder.encode(request.getPassword()));
        account.setActive(true);

        Role userRole = roleRepository.findByName("CUSTOMER")
                .orElseThrow(() -> new RuntimeException("Role CUSTOMER not found"));
        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        account.setRoles(roles);

        accountRepository.save(account);
    }

    public AuthenticationResponse login(AuthenticationRequest request) {
        Account account = accountRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Tên đăng nhập hoặc mật khẩu không đúng"));

        if (!passwordEncoder.matches(request.getPassword(), account.getPassword())) {
            throw new IllegalArgumentException("Tên đăng nhập hoặc mật khẩu không đúng");
        }

        // Kiểm tra tài khoản có bị khóa không
        if (!account.isActive()) {
            throw new IllegalArgumentException("Tài khoản của bạn đã bị khóa. Vui lòng liên hệ quản trị viên.");
        }

        String accessToken = generateToken(account, VALID_DURATION, ChronoUnit.SECONDS);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(account.getUsername());

        return new AuthenticationResponse(accessToken, refreshToken.getToken());
    }

    /**
     * Hàm làm mới token
     */
    public AuthenticationResponse refreshToken(RefreshTokenRequest request) {
        // Xác minh refresh token (còn hạn, tồn tại)
        RefreshToken refreshToken = refreshTokenService.verifyToken(request.getRefreshToken());
        Account account = refreshToken.getAccount();

        // Tạo access token mới
        String newAccessToken = generateToken(account, VALID_DURATION, ChronoUnit.SECONDS);

        return new AuthenticationResponse(newAccessToken, request.getRefreshToken());
    }

    /**
     * GenerateToken để chấp nhận thời hạn.
     */
    private String generateToken(Account account, long duration, ChronoUnit unit) {
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS256);

        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .subject(account.getUsername())
                .issuer("sebook.com")
                .issueTime(new Date())
                .expirationTime(new Date(
                        Instant.now().plus(duration, unit).toEpochMilli()
                ))
                .claim("scope", buildScope(account))
                .build();

        SignedJWT signedJWT = new SignedJWT(header, jwtClaimsSet);

        try {
            JWSSigner signer = new MACSigner(SIGNER_KEY.getBytes());
            signedJWT.sign(signer);
            return signedJWT.serialize();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String buildScope(Account account) {
        StringJoiner joiner = new StringJoiner(" ");
        if (!account.getRoles().isEmpty()) {
            account.getRoles().forEach(role -> joiner.add(role.getName().toUpperCase()));
        }
        return joiner.toString();
    }

    /**
     * UC003: Đăng xuất - Vô hiệu hóa Refresh Token
     */
    @Transactional
    public void logout(String refreshToken) {
        Optional<RefreshToken> tokenOpt = refreshTokenRepository.findByToken(refreshToken);
        if (tokenOpt.isPresent()) {
            refreshTokenRepository.delete(tokenOpt.get());
        }
    }

    /**
     * UC004: Đổi mật khẩu
     */
    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        Account account = securityUtil.getLoggedInAccount();

        // Kiểm tra mật khẩu hiện tại
        if (!passwordEncoder.matches(request.getCurrentPassword(), account.getPassword())) {
            throw new IllegalArgumentException("Mật khẩu hiện tại không đúng");
        }

        // Kiểm tra mật khẩu mới không được trùng mật khẩu cũ
        if (passwordEncoder.matches(request.getNewPassword(), account.getPassword())) {
            throw new IllegalArgumentException("Mật khẩu mới không được trùng với mật khẩu cũ");
        }

        // Cập nhật mật khẩu mới
        account.setPassword(passwordEncoder.encode(request.getNewPassword()));
        accountRepository.save(account);
    }

    /**
     * UC005: Gửi email đặt lại mật khẩu
     */
    @Transactional
    public void sendPasswordResetEmail(ForgotPasswordRequest request) {
        Account account = accountRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Tên đăng nhập không tồn tại"));

        // Kiểm tra email có khớp không
        if (!account.getEmail().equals(request.getEmail())) {
            throw new IllegalArgumentException("Email không khớp với tên đăng nhập");
        }

        // Tìm token cũ hoặc tạo mới
        PasswordResetToken resetToken = passwordResetTokenRepository.findByAccount(account)
                .orElse(new PasswordResetToken());

        // Cập nhật hoặc tạo mới token (hết hạn sau 1 giờ)
        resetToken.setAccount(account);
        resetToken.setToken(UUID.randomUUID().toString());
        resetToken.setExpiryDate(Instant.now().plus(1, ChronoUnit.HOURS));
        passwordResetTokenRepository.save(resetToken);

        // Gửi email
        String subject = "Đặt lại mật khẩu SEBook";
        String resetLink = "http://localhost:5173/password/reset?token=" + resetToken.getToken() + "&username=" + account.getUsername();
        String body = "<h1>Đặt lại mật khẩu</h1>" +
                "<p>Xin chào " + account.getUsername() + ",</p>" +
                "<p>Bạn đã yêu cầu đặt lại mật khẩu. Vui lòng click vào link sau để đặt lại mật khẩu:</p>" +
                "<p><a href=\"" + resetLink + "\">" + resetLink + "</a></p>" +
                "<p>Link này sẽ hết hạn sau 1 giờ.</p>" +
                "<p>Nếu bạn không yêu cầu đặt lại mật khẩu, vui lòng bỏ qua email này.</p>" +
                "<p>Trân trọng,<br/>Đội ngũ SEBook</p>";

        try {
            emailSenderUtil.sendEmail(account.getEmail(), subject, body);
            System.out.println("Password reset email sent successfully to: " + account.getEmail());
        } catch (Exception e) {
            System.err.println("⚠️ WARNING: Failed to send password reset email to " + account.getEmail());
            System.err.println("Error: " + e.getMessage());
            System.err.println("Reset token: " + resetToken.getToken());
            System.err.println("Reset link: " + resetLink);
            e.printStackTrace();
            
            // Trong môi trường development, có thể cho phép tiếp tục
            // Nhưng trong production nên throw exception
            // Tạm thời log và tiếp tục để test
            // throw new RuntimeException("Không thể gửi email. Vui lòng kiểm tra cấu hình email. Token: " + resetToken.getToken(), e);
        }
    }

    /**
     * UC005: Đặt lại mật khẩu bằng token
     */
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new IllegalArgumentException("Token đặt lại mật khẩu không hợp lệ"));

        // Kiểm tra token còn hạn không
        if (resetToken.getExpiryDate().compareTo(Instant.now()) < 0) {
            passwordResetTokenRepository.delete(resetToken);
            throw new IllegalArgumentException("Token đặt lại mật khẩu đã hết hạn");
        }

        // Kiểm tra username có khớp không
        if (!resetToken.getAccount().getUsername().equals(request.getUsername())) {
            throw new IllegalArgumentException("Tên đăng nhập không khớp với token");
        }

        // Kiểm tra mật khẩu mới không được trùng mật khẩu cũ
        Account account = resetToken.getAccount();
        if (passwordEncoder.matches(request.getNewPassword(), account.getPassword())) {
            throw new IllegalArgumentException("Mật khẩu mới không được trùng với mật khẩu cũ");
        }

        // Cập nhật mật khẩu mới
        account.setPassword(passwordEncoder.encode(request.getNewPassword()));
        accountRepository.save(account);

        // Xóa token sau khi đã sử dụng
        passwordResetTokenRepository.delete(resetToken);
    }
}