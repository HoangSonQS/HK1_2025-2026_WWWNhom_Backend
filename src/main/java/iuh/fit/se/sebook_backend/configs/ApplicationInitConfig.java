package iuh.fit.se.sebook_backend.configs;

import iuh.fit.se.sebook_backend.entity.Account;
import iuh.fit.se.sebook_backend.entity.Role;
import iuh.fit.se.sebook_backend.repository.AccountRepository;
import iuh.fit.se.sebook_backend.repository.RoleRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Configuration
public class ApplicationInitConfig {

    private final AccountRepository accountRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @PersistenceContext
    private EntityManager entityManager;

    public ApplicationInitConfig(AccountRepository accountRepository,
                                 RoleRepository roleRepository,
                                 PasswordEncoder passwordEncoder) {
        this.accountRepository = accountRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Bean
    @Transactional
    public CommandLineRunner initDatabase() {
        return args -> {
            // Reset sequence để tránh conflict với dữ liệu đã có
            resetSequenceIfNeeded();

            // Tạo các quyền - tìm theo tên đúng với database (case-insensitive)
            Role adminRole = createRoleIfNotFound("admin", "ADMIN");
            Role sellerStaffRole = createRoleIfNotFound("seller", "SELLER_STAFF");
            Role warehouseStaffRole = createRoleIfNotFound("warehouse", "WAREHOUSE_STAFF");
            Role customerRole = createRoleIfNotFound("customer", "CUSTOMER");

            // Tạo các tài khoản
            createAccountIfNotFound("admin", "admin@sebook.com", "admin123", Set.of(adminRole));
            createAccountIfNotFound("seller", "seller@sebook.com", "seller123", Set.of(sellerStaffRole));
            createAccountIfNotFound("warehouse", "warehouse@sebook.com", "warehouse123", Set.of(warehouseStaffRole));
            createAccountIfNotFound("customer", "customer@sebook.com", "customer123", Set.of(customerRole));
        };
    }

    private void resetSequenceIfNeeded() {
        try {
            // Reset sequence cho bảng roles (PostgreSQL IDENTITY sử dụng sequence tự động)
            Long maxId = roleRepository.findAll().stream()
                    .mapToLong(Role::getId)
                    .max()
                    .orElse(0L);
            if (maxId > 0) {
                // Thử reset với các tên sequence có thể có
                String[] possibleSequences = {
                    "roles_id_seq",
                    "public.roles_id_seq"
                };
                
                for (String seqName : possibleSequences) {
                    try {
                        entityManager.createNativeQuery(
                                "SELECT setval('" + seqName + "', (SELECT COALESCE(MAX(id), 1) FROM roles))"
                        ).executeUpdate();
                        System.out.println("Reset sequence: " + seqName);
                        break;
                    } catch (Exception e) {
                        // Thử sequence tiếp theo
                    }
                }
            }
        } catch (Exception e) {
            // Nếu sequence không tồn tại hoặc có lỗi, bỏ qua (không ảnh hưởng đến logic chính)
            System.out.println("Could not reset sequence (this is normal if using IDENTITY): " + e.getMessage());
        }
    }

    private Role createRoleIfNotFound(String dbName, String codeName) {
        // Tìm role theo tên trong database (case-insensitive)
        return roleRepository.findByName(dbName)
                .or(() -> roleRepository.findByName(codeName))
                .or(() -> {
                    // Nếu không tìm thấy, thử tìm case-insensitive trong tất cả roles
                    return roleRepository.findAll().stream()
                            .filter(r -> r.getName().equalsIgnoreCase(dbName) || r.getName().equalsIgnoreCase(codeName))
                            .findFirst();
                })
                .orElseGet(() -> {
                    // Chỉ tạo mới nếu thực sự không tồn tại
                    try {
                        Role role = new Role();
                        role.setName(codeName); // Sử dụng tên code (uppercase) cho roles mới
                        Role savedRole = roleRepository.save(role);
                        System.out.println("Created role: " + savedRole.getName());
                        return savedRole;
                    } catch (org.springframework.dao.DataIntegrityViolationException e) {
                        // Nếu có lỗi duplicate key (sequence conflict), thử tìm lại sau khi flush
                        entityManager.flush();
                        entityManager.clear();
                        return roleRepository.findAll().stream()
                                .filter(r -> r.getName().equalsIgnoreCase(dbName) || r.getName().equalsIgnoreCase(codeName))
                                .findFirst()
                                .orElseThrow(() -> new RuntimeException("Cannot create or find role: " + codeName + ". Error: " + e.getMessage()));
                    } catch (Exception e) {
                        // Nếu có lỗi khác, thử tìm lại
                        System.err.println("Error creating role " + codeName + ", trying to find existing: " + e.getMessage());
                        entityManager.flush();
                        entityManager.clear();
                        return roleRepository.findAll().stream()
                                .filter(r -> r.getName().equalsIgnoreCase(dbName) || r.getName().equalsIgnoreCase(codeName))
                                .findFirst()
                                .orElseThrow(() -> new RuntimeException("Cannot create or find role: " + codeName + ". Error: " + e.getMessage()));
                    }
                });
    }

    private void createAccountIfNotFound(String username, String email, String password, Set<Role> roles) {
        accountRepository.findByUsername(username)
                .orElseGet(() -> {
                    Account account = new Account();
                    account.setUsername(username);
                    account.setEmail(email);
                    account.setPassword(passwordEncoder.encode(password));
                    account.setRoles(roles);
                    account.setActive(true);
                    Account savedAccount = accountRepository.save(account);
                    System.out.println("Created account: " + savedAccount.getUsername());
                    return savedAccount;
                });
    }
}
