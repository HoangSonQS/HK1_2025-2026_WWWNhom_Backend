package iuh.fit.se.sebook_backend.service;

import iuh.fit.se.sebook_backend.dto.AccountResponse;
import iuh.fit.se.sebook_backend.dto.AccountStatusUpdateRequest;
import iuh.fit.se.sebook_backend.dto.CreateStaffAccountRequest;
import iuh.fit.se.sebook_backend.dto.UpdateAccountRolesRequest;
import iuh.fit.se.sebook_backend.entity.Account;
import iuh.fit.se.sebook_backend.entity.Role;
import iuh.fit.se.sebook_backend.repository.AccountRepository;
import iuh.fit.se.sebook_backend.repository.RoleRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AccountManagementService {

    private final AccountRepository accountRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountManagementService(AccountRepository accountRepository, 
                                   RoleRepository roleRepository,
                                   PasswordEncoder passwordEncoder) {
        this.accountRepository = accountRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
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
     * Tạo tài khoản nhân viên mới (chỉ ADMIN)
     */
    @Transactional
    public AccountResponse createStaffAccount(CreateStaffAccountRequest request) {
        // Kiểm tra username đã tồn tại
        if (accountRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Tên đăng nhập đã tồn tại");
        }
        
        // Kiểm tra email đã tồn tại
        if (accountRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email đã được sử dụng");
        }

        Account account = new Account();
        account.setUsername(request.getUsername());
        account.setEmail(request.getEmail());
        account.setPassword(passwordEncoder.encode(request.getPassword()));
        account.setActive(true);

        // Thêm roles
        Set<Role> roles = new HashSet<>();
        for (String roleName : request.getRoles()) {
            Role role = roleRepository.findByName(roleName.toUpperCase())
                    .orElseThrow(() -> new IllegalArgumentException("Role không tồn tại: " + roleName));
            roles.add(role);
        }
        account.setRoles(roles);

        Account savedAccount = accountRepository.save(account);
        return toDto(savedAccount);
    }

    /**
     * Cập nhật roles của tài khoản
     */
    @Transactional
    public AccountResponse updateAccountRoles(Long accountId, UpdateAccountRolesRequest request) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        // Cập nhật roles
        Set<Role> roles = new HashSet<>();
        for (String roleName : request.getRoles()) {
            Role role = roleRepository.findByName(roleName.toUpperCase())
                    .orElseThrow(() -> new IllegalArgumentException("Role không tồn tại: " + roleName));
            roles.add(role);
        }
        account.setRoles(roles);

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