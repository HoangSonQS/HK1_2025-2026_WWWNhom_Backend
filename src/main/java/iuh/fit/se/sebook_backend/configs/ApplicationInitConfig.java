package iuh.fit.se.sebook_backend.configs;

import iuh.fit.se.sebook_backend.entity.Account;
import iuh.fit.se.sebook_backend.entity.Role;
import iuh.fit.se.sebook_backend.repository.AccountRepository;
import iuh.fit.se.sebook_backend.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

@Configuration
public class ApplicationInitConfig {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner initDatabase() {
        return args -> {
            // Tạo các quyền nếu chưa tồn tại
            Role adminRole = createRoleIfNotFound("ADMIN");
            Role sellerStaffRole = createRoleIfNotFound("SELLER_STAFF");
            Role warehouseStaffRole = createRoleIfNotFound("WAREHOUSE_STAFF");
            Role customerRole = createRoleIfNotFound("CUSTOMER");

            // Tạo tài khoản Admin nếu chưa tồn tại
            createAccountIfNotFound("admin", "admin@sebook.com", "admin123", Set.of(adminRole));

            // Tạo tài khoản Nhân viên bán hàng
            createAccountIfNotFound("seller", "seller@sebook.com", "seller123", Set.of(sellerStaffRole));

            // Tạo tài khoản Nhân viên kho
            createAccountIfNotFound("warehouse", "warehouse@sebook.com", "warehouse123", Set.of(warehouseStaffRole));

            // Tạo tài khoản Khách hàng
            createAccountIfNotFound("customer", "customer@sebook.com", "customer123", Set.of(customerRole));
        };
    }

    private Role createRoleIfNotFound(String name) {
        return roleRepository.findByName(name).orElseGet(() -> {
            Role newRole = new Role();
            newRole.setName(name);
            return roleRepository.save(newRole);
        });
    }

    private void createAccountIfNotFound(String username, String email, String password, Set<Role> roles) {
        if (!accountRepository.findByUsername(username).isPresent()) {
            Account account = new Account();
            account.setUsername(username);
            account.setEmail(email);
            account.setPassword(passwordEncoder.encode(password));
            account.setRoles(roles);
            account.setActive(true);
            accountRepository.save(account);
        }
    }
}