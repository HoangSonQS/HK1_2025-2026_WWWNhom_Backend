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

        // Xóa toàn bộ token cũ của người dùng để tránh trùng lặp/NonUniqueResult
        refreshTokenRepository.deleteByAccount(account);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setAccount(account);
        refreshToken.setExpiryDate(Instant.now().plus(REFRESHABLE_DURATION, ChronoUnit.DAYS));
        refreshToken.setToken(UUID.randomUUID().toString());

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