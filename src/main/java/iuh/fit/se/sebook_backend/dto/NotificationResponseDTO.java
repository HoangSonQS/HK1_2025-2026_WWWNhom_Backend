package iuh.fit.se.sebook_backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class NotificationResponseDTO {
    private Long id;
    private String title;
    private String content;
    private LocalDateTime createdAt;
    
    @JsonProperty("isRead")
    private boolean isRead;
    
    private String senderName;
}