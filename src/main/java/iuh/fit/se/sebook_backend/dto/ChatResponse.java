package iuh.fit.se.sebook_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {
    private String response; // Câu trả lời từ AI
    private List<String> suggestedBooks; // Danh sách sách được đề xuất
    private List<String> sources; // Danh sách nguồn tham khảo (tên sách)
    private String conversationId; // ID cuộc hội thoại
}

