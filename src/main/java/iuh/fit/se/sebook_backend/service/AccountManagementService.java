package iuh.fit.se.sebook_backend.service;

import iuh.fit.se.sebook_backend.dto.AccountResponse;
import iuh.fit.se.sebook_backend.dto.AccountStatusUpdateRequest;
import iuh.fit.se.sebook_backend.dto.AddressDTO;
import iuh.fit.se.sebook_backend.dto.AddressRequestDTO;
import iuh.fit.se.sebook_backend.dto.UpdateAccountRequest;
import iuh.fit.se.sebook_backend.entity.Account;
import iuh.fit.se.sebook_backend.entity.Address;
import iuh.fit.se.sebook_backend.entity.Role;
import iuh.fit.se.sebook_backend.repository.AccountRepository;
import iuh.fit.se.sebook_backend.repository.AddressRepository;
import iuh.fit.se.sebook_backend.utils.SecurityUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AccountManagementService {

    private final AccountRepository accountRepository;
    private final AddressRepository addressRepository;
    private final SecurityUtil securityUtil;

    public AccountManagementService(AccountRepository accountRepository, AddressRepository addressRepository, SecurityUtil securityUtil) {
        this.accountRepository = accountRepository;
        this.addressRepository = addressRepository;
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
     * Lấy thông tin tài khoản của chính người dùng đang đăng nhập
     */
    public AccountResponse getMyAccount() {
        Account account = securityUtil.getLoggedInAccount();
        // Load addresses để đảm bảo có dữ liệu
        account.getAddresses().size(); // Trigger lazy loading
        return toDto(account);
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

        // Cập nhật thông tin cá nhân
        if (request.getFirstName() != null) {
            account.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            account.setLastName(request.getLastName());
        }
        if (request.getPhoneNumber() != null) {
            account.setPhoneNumber(request.getPhoneNumber());
        }

        Account savedAccount = accountRepository.save(account);
        return toDto(savedAccount);
    }

    /**
     * Thêm địa chỉ mới cho tài khoản đang đăng nhập
     */
    @Transactional
    public AddressDTO addAddress(AddressRequestDTO request) {
        Account account = securityUtil.getLoggedInAccount();
        
        Address address = new Address();
        address.setAccount(account);
        address.setAddressType(request.getAddressType() != null ? request.getAddressType() : "OTHER");
        address.setStreet(request.getStreet());
        address.setWard(request.getWard());
        address.setDistrict(request.getDistrict());
        address.setCity(request.getCity());
        address.setPhoneNumber(request.getPhoneNumber());
        address.setRecipientName(request.getRecipientName());
        
        // Nếu đặt làm mặc định hoặc đây là địa chỉ đầu tiên
        boolean shouldSetDefault = request.getIsDefault() != null && request.getIsDefault();
        if (shouldSetDefault || addressRepository.findByAccount(account).isEmpty()) {
            // Bỏ mặc định của các địa chỉ khác
            addressRepository.findByAccount(account).forEach(addr -> addr.setDefault(false));
            address.setDefault(true);
        } else {
            address.setDefault(false);
        }
        
        Address savedAddress = addressRepository.save(address);
        return toAddressDto(savedAddress);
    }

    /**
     * Cập nhật địa chỉ
     */
    @Transactional
    public AddressDTO updateAddress(Long addressId, AddressRequestDTO request) {
        Account account = securityUtil.getLoggedInAccount();
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new IllegalArgumentException("Địa chỉ không tồn tại"));
        
        // Kiểm tra địa chỉ thuộc về tài khoản đang đăng nhập
        if (!address.getAccount().getId().equals(account.getId())) {
            throw new IllegalArgumentException("Bạn không có quyền cập nhật địa chỉ này");
        }
        
        if (request.getAddressType() != null) {
            address.setAddressType(request.getAddressType());
        }
        if (request.getStreet() != null) {
            address.setStreet(request.getStreet());
        }
        if (request.getWard() != null) {
            address.setWard(request.getWard());
        }
        if (request.getDistrict() != null) {
            address.setDistrict(request.getDistrict());
        }
        if (request.getCity() != null) {
            address.setCity(request.getCity());
        }
        if (request.getPhoneNumber() != null) {
            address.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getRecipientName() != null) {
            address.setRecipientName(request.getRecipientName());
        }
        
        // Xử lý đặt làm mặc định
        if (request.getIsDefault() != null && request.getIsDefault()) {
            // Bỏ mặc định của các địa chỉ khác
            addressRepository.findByAccount(account).forEach(addr -> {
                if (!addr.getId().equals(addressId)) {
                    addr.setDefault(false);
                }
            });
            address.setDefault(true);
        }
        
        Address savedAddress = addressRepository.save(address);
        return toAddressDto(savedAddress);
    }

    /**
     * Xóa địa chỉ
     */
    @Transactional
    public void deleteAddress(Long addressId) {
        Account account = securityUtil.getLoggedInAccount();
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new IllegalArgumentException("Địa chỉ không tồn tại"));
        
        // Kiểm tra địa chỉ thuộc về tài khoản đang đăng nhập
        if (!address.getAccount().getId().equals(account.getId())) {
            throw new IllegalArgumentException("Bạn không có quyền xóa địa chỉ này");
        }
        
        addressRepository.delete(address);
    }

    /**
     * Đặt địa chỉ làm mặc định
     */
    @Transactional
    public AddressDTO setDefaultAddress(Long addressId) {
        Account account = securityUtil.getLoggedInAccount();
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new IllegalArgumentException("Địa chỉ không tồn tại"));
        
        // Kiểm tra địa chỉ thuộc về tài khoản đang đăng nhập
        if (!address.getAccount().getId().equals(account.getId())) {
            throw new IllegalArgumentException("Bạn không có quyền thay đổi địa chỉ này");
        }
        
        // Bỏ mặc định của các địa chỉ khác
        addressRepository.findByAccount(account).forEach(addr -> {
            if (!addr.getId().equals(addressId)) {
                addr.setDefault(false);
                addressRepository.save(addr);
            }
        });
        
        address.setDefault(true);
        Address savedAddress = addressRepository.save(address);
        return toAddressDto(savedAddress);
    }

    /**
     * Lấy danh sách địa chỉ của tài khoản đang đăng nhập
     */
    public List<AddressDTO> getMyAddresses() {
        Account account = securityUtil.getLoggedInAccount();
        return addressRepository.findByAccount(account).stream()
                .map(this::toAddressDto)
                .collect(Collectors.toList());
    }

    private AccountResponse toDto(Account account) {
        List<AddressDTO> addressDTOs = account.getAddresses() != null 
                ? account.getAddresses().stream()
                        .map(this::toAddressDto)
                        .collect(Collectors.toList())
                : List.of();
        
        return AccountResponse.builder()
                .id(account.getId())
                .username(account.getUsername())
                .email(account.getEmail())
                .isActive(account.isActive())
                .firstName(account.getFirstName())
                .lastName(account.getLastName())
                .phoneNumber(account.getPhoneNumber())
                .roles(account.getRoles().stream()
                        .map(Role::getName)
                        .collect(Collectors.toSet()))
                .addresses(addressDTOs)
                .build();
    }

    private AddressDTO toAddressDto(Address address) {
        return AddressDTO.builder()
                .id(address.getId())
                .addressType(address.getAddressType())
                .isDefault(address.isDefault())
                .street(address.getStreet())
                .ward(address.getWard())
                .district(address.getDistrict())
                .city(address.getCity())
                .phoneNumber(address.getPhoneNumber())
                .recipientName(address.getRecipientName())
                .build();
    }
}