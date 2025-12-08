package iuh.fit.se.sebook_backend.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class CloudinaryService {

    @Autowired
    private Cloudinary cloudinary;

    public String uploadImage(MultipartFile file) {
        try {
            Map<?, ?> uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap("resource_type", "auto")
            );
            return (String) uploadResult.get("secure_url");
        } catch (IOException e) {
            throw new RuntimeException("Lỗi khi upload ảnh lên Cloudinary: " + e.getMessage(), e);
        }
    }

    public void deleteImage(String imageUrl) {
        try {
            // Lấy public_id từ URL
            String publicId = extractPublicIdFromUrl(imageUrl);
            if (publicId != null) {
                cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            }
        } catch (IOException e) {
            throw new RuntimeException("Lỗi khi xóa ảnh từ Cloudinary: " + e.getMessage(), e);
        }
    }

    private String extractPublicIdFromUrl(String url) {
        try {
            // URL format: https://res.cloudinary.com/{cloud_name}/image/upload/{version}/{public_id}.{format}
            // Hoặc: https://res.cloudinary.com/{cloud_name}/image/upload/{public_id}.{format}
            String[] parts = url.split("/upload/");
            if (parts.length > 1) {
                String path = parts[1];
                // Loại bỏ version nếu có (format: v1234567890/...)
                if (path.contains("/")) {
                    String[] pathParts = path.split("/", 2);
                    path = pathParts[1];
                }
                // Loại bỏ extension
                int lastDot = path.lastIndexOf('.');
                if (lastDot > 0) {
                    return path.substring(0, lastDot);
                }
                return path;
            }
        } catch (Exception e) {
            // Nếu không parse được, trả về null
        }
        return null;
    }
}

