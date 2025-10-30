package iuh.fit.se.sebook_backend.service;

import iuh.fit.se.sebook_backend.dto.AccountResponse;
import iuh.fit.se.sebook_backend.dto.AccountStatusUpdateRequest;
import iuh.fit.se.sebook_backend.entity.Account;
import iuh.fit.se.sebook_backend.entity.Role;
import iuh.fit.se.sebook_backend.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AccountManagementService {

    private final AccountRepository accountRepository;

    public AccountManagementService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    /**
     * Lấy tất cả tài khoản trong hệ thống
     */
    public List<AccountResponse> getAllAccounts() {
        return accountRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Cập nhật trạng thái (active/inactive) của tài khoản
     */
    @Transactional
    public AccountResponse updateAccountStatus(Long accountId, AccountStatusUpdateRequest request) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        account.setActive(request.isActive());
        Account savedAccount = accountRepository.save(account);
        return toDto(savedAccount);
    }

    private AccountResponse toDto(Account account) {
        return AccountResponse.builder()
                .id(account.getId())
                .username(account.getUsername())
                .email(account.getEmail())
                .isActive(account.isActive())
                .roles(account.getRoles().stream()
                        .map(Role::getName)
                        .collect(Collectors.toSet()))
                .build();
    }
}