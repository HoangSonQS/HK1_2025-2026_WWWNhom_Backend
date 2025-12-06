package iuh.fit.se.sebook_backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatRequest {
    @NotBlank(message = "Message không được để trống")
    private String message;
    
    private String conversationId; // Optional - để duy trì cuộc hội thoại
}

