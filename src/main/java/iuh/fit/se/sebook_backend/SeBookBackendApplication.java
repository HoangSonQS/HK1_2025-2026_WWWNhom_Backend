package iuh.fit.se.sebook_backend;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SeBookBackendApplication {

    public static void main(String[] args) {
        // Load .env file trước khi Spring Boot khởi động
        try {
            Dotenv dotenv = Dotenv.configure()
                    .ignoreIfMissing()
                    .load();
            
            // Load các biến môi trường từ .env vào System properties
            dotenv.entries().forEach(entry -> {
                String key = entry.getKey();
                String value = entry.getValue();
                if (System.getProperty(key) == null) {
                    System.setProperty(key, value);
                }
            });
        } catch (Exception e) {
            // Nếu không tìm thấy file .env, sử dụng biến môi trường hệ thống
            System.out.println("Không tìm thấy file .env, sử dụng biến môi trường hệ thống");
        }
        
        SpringApplication.run(SeBookBackendApplication.class, args);
    }

}
