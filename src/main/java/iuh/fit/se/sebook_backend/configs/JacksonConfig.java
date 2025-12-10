package iuh.fit.se.sebook_backend.configs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.util.TimeZone;

/**
 * Cấu hình Jackson ObjectMapper để đảm bảo timezone được áp dụng đúng cho JSON serialization
 */
@Configuration
public class JacksonConfig {

    private static final Logger log = LoggerFactory.getLogger(JacksonConfig.class);
    private static final String VIETNAM_TIMEZONE = "Asia/Ho_Chi_Minh";

    @Bean
    @Primary
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        // TimeZone.getTimeZone() không bao giờ trả về null, nhưng để an toàn ta vẫn kiểm tra
        TimeZone vietnamTimeZone = TimeZone.getTimeZone(VIETNAM_TIMEZONE);
        if (vietnamTimeZone == null) {
            vietnamTimeZone = TimeZone.getDefault();
            log.warn("⚠️ Không tìm thấy timezone {}, sử dụng timezone mặc định: {}", 
                    VIETNAM_TIMEZONE, vietnamTimeZone.getID());
        }
        
        ObjectMapper objectMapper = builder
                .modules(new JavaTimeModule())
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .timeZone(vietnamTimeZone)
                .build();
        
        log.info("✅ Đã cấu hình Jackson ObjectMapper với timezone: {}", vietnamTimeZone.getID());
        
        return objectMapper;
    }
}

