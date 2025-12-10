package iuh.fit.se.sebook_backend.configs;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

import java.util.TimeZone;

/**
 * Cấu hình timezone mặc định cho ứng dụng là múi giờ Việt Nam (UTC+7)
 * 
 * Timezone này sẽ được áp dụng cho:
 * - JVM default timezone
 * - Tất cả các LocalDateTime, LocalDate, Instant trong ứng dụng
 * - Database connections (qua application.properties)
 * - Jackson JSON serialization (qua application.properties)
 */
@Configuration
public class TimezoneConfig {

    private static final Logger log = LoggerFactory.getLogger(TimezoneConfig.class);
    private static final String VIETNAM_TIMEZONE = "Asia/Ho_Chi_Minh";

    @PostConstruct
    public void init() {
        // Set timezone mặc định cho JVM
        // Điều này ảnh hưởng đến tất cả các LocalDateTime.now(), LocalDate.now(), Instant.now()
        TimeZone.setDefault(TimeZone.getTimeZone(VIETNAM_TIMEZONE));
        
        // Log để xác nhận timezone đã được set
        TimeZone defaultTZ = TimeZone.getDefault();
        log.info("✅ Đã cấu hình timezone mặc định: {} (UTC{})", 
                defaultTZ.getID(), 
                defaultTZ.getRawOffset() / (1000 * 60 * 60));
        log.info("   - Timezone hiện tại: {}", defaultTZ.getDisplayName());
    }
}

