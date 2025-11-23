package iuh.fit.se.sebook_backend.service;

import iuh.fit.se.sebook_backend.dto.AccountResponse;
import iuh.fit.se.sebook_backend.dto.AccountStatusUpdateRequest;
import iuh.fit.se.sebook_backend.dto.UpdateAccountRequest;
import iuh.fit.se.sebook_backend.entity.Account;
import iuh.fit.se.sebook_backend.entity.Role;
import iuh.fit.se.sebook_backend.repository.AccountRepository;
import iuh.fit.se.sebook_backend.utils.SecurityUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AccountManagementService {

    private final AccountRepository accountRepository;
    private final SecurityUtil securityUtil;

    public AccountManagementService(AccountRepository accountRepository, SecurityUtil securityUtil) {
        this.accountRepository = accountRepository;
        this.securityUtil = securityUtil;
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

    /**
     * Cập nhật thông tin tài khoản của chính người dùng đang đăng nhập
     */
    @Transactional
    public AccountResponse updateMyAccount(UpdateAccountRequest request) {
        Account account = securityUtil.getLoggedInAccount();

        // Kiểm tra username mới có bị trùng không (nếu thay đổi)
        if (request.getUsername() != null && !request.getUsername().equals(account.getUsername())) {
            if (accountRepository.findByUsername(request.getUsername()).isPresent()) {
                throw new IllegalArgumentException("Tên đăng nhập đã tồn tại");
            }
            account.setUsername(request.getUsername());
        }

        // Kiểm tra email mới có bị trùng không (nếu thay đổi)
        if (request.getEmail() != null && !request.getEmail().equals(account.getEmail())) {
            if (accountRepository.findByEmail(request.getEmail()).isPresent()) {
                throw new IllegalArgumentException("Email đã được sử dụng");
            }
            account.setEmail(request.getEmail());
        }

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