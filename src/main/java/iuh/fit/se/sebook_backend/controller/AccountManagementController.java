package iuh.fit.se.sebook_backend.controller;

import iuh.fit.se.sebook_backend.dto.AccountResponse;
import iuh.fit.se.sebook_backend.dto.AccountStatusUpdateRequest;
import iuh.fit.se.sebook_backend.dto.UpdateAccountRequest;
import iuh.fit.se.sebook_backend.service.AccountManagementService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/accounts")
public class AccountManagementController {

    private final AccountManagementService accountManagementService;

    public AccountManagementController(AccountManagementService accountManagementService) {
        this.accountManagementService = accountManagementService;
    }

    /**
     * API lấy tất cả tài khoản
     */
    @GetMapping
    public ResponseEntity<List<AccountResponse>> getAllAccounts() {
        return ResponseEntity.ok(accountManagementService.getAllAccounts());
    }

    /**
     * API cập nhật trạng thái (kích hoạt / vô hiệu hóa)
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<AccountResponse> updateAccountStatus(@PathVariable Long id,
                                                               @RequestBody AccountStatusUpdateRequest request) {
        return ResponseEntity.ok(accountManagementService.updateAccountStatus(id, request));
    }

    /**
     * API lấy thông tin tài khoản của chính người dùng đang đăng nhập
     */
    @GetMapping("/me")
    public ResponseEntity<AccountResponse> getMyAccount() {
        return ResponseEntity.ok(accountManagementService.getMyAccount());
    }

    /**
     * API cập nhật thông tin tài khoản của chính người dùng đang đăng nhập
     */
    @PutMapping("/me")
    public ResponseEntity<AccountResponse> updateMyAccount(@RequestBody UpdateAccountRequest request) {
        return ResponseEntity.ok(accountManagementService.updateMyAccount(request));
    }

    /**
     * API cập nhật thông tin tài khoản khác (chỉ dành cho admin)
     */
    @PutMapping("/{id}")
    public ResponseEntity<AccountResponse> updateAccount(@PathVariable Long id,
                                                       @RequestBody UpdateAccountRequest request) {
        return ResponseEntity.ok(accountManagementService.updateAccount(id, request));
    }
}