package iuh.fit.se.sebook_backend.controller;

import iuh.fit.se.sebook_backend.dto.AddressDTO;
import iuh.fit.se.sebook_backend.dto.AddressRequestDTO;
import iuh.fit.se.sebook_backend.service.AccountManagementService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user/addresses")
public class AddressController {

    private final AccountManagementService accountManagementService;

    public AddressController(AccountManagementService accountManagementService) {
        this.accountManagementService = accountManagementService;
    }

    /**
     * Lấy danh sách địa chỉ của tài khoản đang đăng nhập
     */
    @GetMapping
    public ResponseEntity<List<AddressDTO>> getMyAddresses() {
        return ResponseEntity.ok(accountManagementService.getMyAddresses());
    }

    /**
     * Thêm địa chỉ mới
     */
    @PostMapping
    public ResponseEntity<AddressDTO> addAddress(@RequestBody AddressRequestDTO request) {
        return ResponseEntity.ok(accountManagementService.addAddress(request));
    }

    /**
     * Cập nhật địa chỉ
     */
    @PutMapping("/{id}")
    public ResponseEntity<AddressDTO> updateAddress(@PathVariable Long id, @RequestBody AddressRequestDTO request) {
        return ResponseEntity.ok(accountManagementService.updateAddress(id, request));
    }

    /**
     * Xóa địa chỉ
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteAddress(@PathVariable Long id) {
        accountManagementService.deleteAddress(id);
        return ResponseEntity.ok("Địa chỉ đã được xóa thành công");
    }

    /**
     * Đặt địa chỉ làm mặc định
     */
    @PutMapping("/{id}/set-default")
    public ResponseEntity<AddressDTO> setDefaultAddress(@PathVariable Long id) {
        return ResponseEntity.ok(accountManagementService.setDefaultAddress(id));
    }
}

