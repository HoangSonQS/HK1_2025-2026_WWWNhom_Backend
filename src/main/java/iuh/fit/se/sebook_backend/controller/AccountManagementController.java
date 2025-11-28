package iuh.fit.se.sebook_backend.controller;

import iuh.fit.se.sebook_backend.dto.AccountResponse;
import iuh.fit.se.sebook_backend.dto.AccountStatusUpdateRequest;
import iuh.fit.se.sebook_backend.dto.CreateStaffAccountRequest;
import iuh.fit.se.sebook_backend.dto.UpdateAccountRolesRequest;
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
     * API tạo tài khoản nhân viên mới
     */
    @PostMapping
    public ResponseEntity<AccountResponse> createStaffAccount(@RequestBody CreateStaffAccountRequest request) {
        return ResponseEntity.ok(accountManagementService.createStaffAccount(request));
    }

    /**
     * API cập nhật roles của tài khoản
     */
    @PutMapping("/{id}/roles")
    public ResponseEntity<AccountResponse> updateAccountRoles(@PathVariable Long id,
                                                              @RequestBody UpdateAccountRolesRequest request) {
        return ResponseEntity.ok(accountManagementService.updateAccountRoles(id, request));
    }
}