package iuh.fit.se.sebook_backend.service;

import iuh.fit.se.sebook_backend.entity.Account;
import iuh.fit.se.sebook_backend.entity.RefreshToken;
import iuh.fit.se.sebook_backend.repository.AccountRepository;
import iuh.fit.se.sebook_backend.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenService {

    @Value("${jwt.refreshable-duration}")
    private long REFRESHABLE_DURATION; // 14

    private final RefreshTokenRepository refreshTokenRepository;
    private final AccountRepository accountRepository;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, AccountRepository accountRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.accountRepository = accountRepository;
    }

    /**
     * Tạo một refresh token mới cho người dùng.
     * Xóa token cũ nếu có.
     */
    @Transactional
    public RefreshToken createRefreshToken(String username) {
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        // Xóa token cũ của người dùng nếu tồn tại
        Optional<RefreshToken> oldTokenOpt = refreshTokenRepository.findByAccount(account);

        RefreshToken refreshToken;
        if (oldTokenOpt.isPresent()) {
            // Nếu có token cũ, chỉ cập nhật nó
            refreshToken = oldTokenOpt.get();
        } else {
            // Nếu không, tạo token mới
            refreshToken = new RefreshToken();
            refreshToken.setAccount(account);
        }

        // Cập nhật/Đặt các giá trị mới
        refreshToken.setExpiryDate(Instant.now().plus(REFRESHABLE_DURATION, ChronoUnit.DAYS));
        refreshToken.setToken(UUID.randomUUID().toString());

        // Lưu (cả trường hợp tạo mới và cập nhật)
        return refreshTokenRepository.save(refreshToken);
    }

    /**
     * Xác minh token (còn hạn và tồn tại)
     */
    public RefreshToken verifyToken(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Refresh token is not in database"));

        if (refreshToken.getExpiryDate().compareTo(Instant.now()) < 0) {
            refreshTokenRepository.delete(refreshToken);
            throw new IllegalArgumentException("Refresh token was expired. Please sign in again");
        }

        return refreshToken;
    }
}