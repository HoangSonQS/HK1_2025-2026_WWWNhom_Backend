package iuh.fit.se.sebook_backend;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class PasswordEncodingTest {

    @Test
    public void testSellerPasswordEncoding() {
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        
        // Mật khẩu gốc từ InitConfig
        String plainPassword = "seller123";
        
        // Mật khẩu đã encoding từ database
        String encodedPasswordFromDB = "$2a$10$A7qGQAfc7a4BpjDdjXIHs.rbts1mPjAtj1.GqFEn7z5itefRdmXxC";
        
        // Kiểm tra xem mật khẩu có khớp không
        boolean matches = encoder.matches(plainPassword, encodedPasswordFromDB);
        System.out.println("=== KIỂM TRA MẬT KHẨU SELLER ===");
        System.out.println("Mật khẩu gốc: " + plainPassword);
        System.out.println("Mật khẩu encoded từ DB: " + encodedPasswordFromDB);
        System.out.println("Kết quả kiểm tra: " + (matches ? "✅ KHỚP" : "❌ KHÔNG KHỚP"));
        
        // Tạo mật khẩu mới để so sánh
        String newEncoded = encoder.encode(plainPassword);
        System.out.println("Mật khẩu encoded mới (để so sánh): " + newEncoded);
        System.out.println("Mật khẩu mới có khớp với plain password không: " + encoder.matches(plainPassword, newEncoded));
        System.out.println();
        
        assertTrue(matches, "Mật khẩu seller phải khớp");
    }

    @Test
    public void testWarehousePasswordEncoding() {
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        
        // Mật khẩu gốc từ InitConfig
        String plainPassword = "warehouse123";
        
        // Mật khẩu đã encoding từ database
        String encodedPasswordFromDB = "$2a$10$CGVIxYvnZzGtcr6qcGaOcelv7gW2uEv7O4gGZiXbJXuIYoLve3oR2";
        
        // Kiểm tra xem mật khẩu có khớp không
        boolean matches = encoder.matches(plainPassword, encodedPasswordFromDB);
        System.out.println("=== KIỂM TRA MẬT KHẨU WAREHOUSE ===");
        System.out.println("Mật khẩu gốc: " + plainPassword);
        System.out.println("Mật khẩu encoded từ DB: " + encodedPasswordFromDB);
        System.out.println("Kết quả kiểm tra: " + (matches ? "✅ KHỚP" : "❌ KHÔNG KHỚP"));
        
        // Tạo mật khẩu mới để so sánh
        String newEncoded = encoder.encode(plainPassword);
        System.out.println("Mật khẩu encoded mới (để so sánh): " + newEncoded);
        System.out.println("Mật khẩu mới có khớp với plain password không: " + encoder.matches(plainPassword, newEncoded));
        System.out.println();
        
        assertTrue(matches, "Mật khẩu warehouse phải khớp");
    }
    
    @Test
    public void testPasswordEncoding() {
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        
        System.out.println("=== KIỂM TRA ENCODING MẬT KHẨU ===");
        System.out.println();
        
        // Test seller
        testSellerPasswordEncoding();
        
        // Test warehouse
        testWarehousePasswordEncoding();
    }
}
