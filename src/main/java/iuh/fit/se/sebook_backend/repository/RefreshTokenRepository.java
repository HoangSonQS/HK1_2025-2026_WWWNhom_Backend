package iuh.fit.se.sebook_backend.repository;

import iuh.fit.se.sebook_backend.entity.Account;
import iuh.fit.se.sebook_backend.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    @Transactional
    void deleteByAccount(Account account);
}