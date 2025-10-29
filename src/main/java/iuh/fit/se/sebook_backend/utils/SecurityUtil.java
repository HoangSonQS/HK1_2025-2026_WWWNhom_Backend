package iuh.fit.se.sebook_backend.utils;

import iuh.fit.se.sebook_backend.entity.Account;
import iuh.fit.se.sebook_backend.repository.AccountRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtil {
    private final AccountRepository accountRepository;

    public SecurityUtil(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public Account getLoggedInAccount() {
        Jwt jwt = (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String username = jwt.getSubject();
        return accountRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Cannot find logged in user"));
    }
}