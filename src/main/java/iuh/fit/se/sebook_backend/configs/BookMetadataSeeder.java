package iuh.fit.se.sebook_backend.configs;

import iuh.fit.se.sebook_backend.entity.Book;
import iuh.fit.se.sebook_backend.repository.BookRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Bổ sung description và metadata (năm XB, trọng lượng, kích thước, số trang, hình thức)
 * cho các sách đang thiếu thông tin. Chỉ điền nếu giá trị đang null/blank/0 để không
 * ghi đè dữ liệu đã có.
 */
@Component
public class BookMetadataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BookMetadataSeeder.class);

    private static final int DEFAULT_YEAR = 2020;
    private static final int DEFAULT_WEIGHT = 300; // gram
    private static final String DEFAULT_DIMENSIONS = "13 x 20 cm";
    private static final int DEFAULT_PAGE_COUNT = 300;
    private static final String DEFAULT_FORMAT = "Bìa mềm";

    private final BookRepository bookRepository;

    public BookMetadataSeeder(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        Map<Long, String> descriptions = buildDescriptions();

        int updated = 0;
        for (Book book : bookRepository.findAll()) {
            boolean changed = false;

            // Description
            if ((book.getDescription() == null || book.getDescription().isBlank())
                    && descriptions.containsKey(book.getId())) {
                book.setDescription(descriptions.get(book.getId()));
                changed = true;
            }

            // Metadata: chỉ điền nếu thiếu
            if (book.getPublicationYear() == null) {
                book.setPublicationYear(DEFAULT_YEAR);
                changed = true;
            }
            if (book.getWeightGrams() == null) {
                book.setWeightGrams(DEFAULT_WEIGHT);
                changed = true;
            }
            if (book.getPackageDimensions() == null || book.getPackageDimensions().isBlank()) {
                book.setPackageDimensions(DEFAULT_DIMENSIONS);
                changed = true;
            }
            if (book.getPageCount() == null) {
                book.setPageCount(DEFAULT_PAGE_COUNT);
                changed = true;
            }
            if (book.getFormat() == null || book.getFormat().isBlank()) {
                book.setFormat(DEFAULT_FORMAT);
                changed = true;
            }

            if (changed) {
                bookRepository.save(book);
                updated++;
            }
        }

        if (updated > 0) {
            log.info("✅ Seeder đã cập nhật description/metadata cho {} sách.", updated);
        }
    }

    private Map<Long, String> buildDescriptions() {
        return Map.ofEntries(
                Map.entry(1L, "Tập hợp các bức thư gửi miền Nam của Lê Duẩn, phản ánh chiến lược và tâm tư trong kháng chiến."),
                Map.entry(2L, "Truyện tranh vui nhộn về mèo máy Doraemon và cậu bé Nobita với vô số bảo bối tương lai."),
                Map.entry(3L, "Những chuyến phiêu lưu tuổi học trò dí dỏm của nhóm bạn trong Kính Vạn Hoa."),
                Map.entry(4L, "Hành trình chăn cừu Santiago đi tìm kho báu và khám phá ý nghĩa cuộc sống."),
                Map.entry(5L, "Hồi ký Nguyễn Chí Vịnh về người thầy và chặng đường trưởng thành trong quân đội."),
                Map.entry(6L, "Cẩm nang khởi nghiệp từ con số 0, chia sẻ kinh nghiệm tạo sản phẩm và tìm cơ hội thị trường."),
                Map.entry(7L, "Nguyên tắc viết mã sạch, dễ đọc, dễ bảo trì từ Robert C. Martin."),
                Map.entry(8L, "Lời khuyên thực dụng giúp lập trình viên nâng cao kỹ năng và cộng tác hiệu quả."),
                Map.entry(9L, "23 mẫu thiết kế kinh điển giúp hệ thống linh hoạt và dễ mở rộng."),
                Map.entry(10L, "Bài học tài chính cá nhân qua câu chuyện hai người cha, thay đổi tư duy về tiền bạc."),
                Map.entry(11L, "Cuộc phiêu lưu đầu tiên của Harry Potter tại Hogwarts và cuộc đối đầu với Voldemort."),
                Map.entry(12L, "Kỹ thuật tái cấu trúc mã để cải thiện thiết kế mà không thay đổi hành vi."),
                Map.entry(13L, "Kiến trúc phần mềm sạch, tách biệt tầng, dễ kiểm thử và bảo trì."),
                Map.entry(14L, "Sổ tay best practices để xây dựng phần mềm quy mô lớn của Steve McConnell."),
                Map.entry(15L, "Giáo trình thuật toán toàn diện về cấu trúc dữ liệu và kỹ thuật giải quyết bài toán."),
                Map.entry(16L, "Học design pattern qua ví dụ minh họa trực quan, dễ tiếp cận."),
                Map.entry(17L, "Áp dụng Domain-Driven Design để gắn chặt business vào kiến trúc phần mềm."),
                Map.entry(18L, "Nhập môn JavaScript, giải thích các khái niệm nền tảng một cách súc tích."),
                Map.entry(19L, "Hướng dẫn lập trình đồng thời an toàn và hiệu quả trong Java."),
                Map.entry(20L, "Hướng dẫn thực hành Spring, từ core, REST đến bảo mật."),
                Map.entry(21L, "Nhập môn SQL, truy vấn và tối ưu hóa dữ liệu quan hệ."),
                Map.entry(22L, "189 câu hỏi phỏng vấn kèm giải thích thuật toán và mẹo coding interview."),
                Map.entry(23L, "Thực hành Java hiệu quả với các best practices và lỗi thường gặp cần tránh."),
                Map.entry(24L, "Kiến trúc hệ thống dữ liệu lớn, lưu trữ, stream và đảm bảo tính nhất quán."),
                Map.entry(25L, "Bài học quản lý dự án phần mềm, huyền thoại “tháng người-mít”."),
                Map.entry(26L, "Cách thử nghiệm nhanh, học từ khách hàng và scale sản phẩm trong khởi nghiệp."),
                Map.entry(27L, "Tìm lý do “vì sao” để dẫn dắt tổ chức và truyền cảm hứng bền vững."),
                Map.entry(28L, "Xây dựng thói quen nhỏ dẫn tới thay đổi lớn và bền vững."),
                Map.entry(29L, "Hai hệ thống tư duy nhanh/chậm và ảnh hưởng của chúng đến quyết định."),
                Map.entry(30L, "Bài học khởi nghiệp công nghệ tạo giá trị đột phá từ Peter Thiel."),
                Map.entry(31L, "Chiến lược đại dương xanh: tạo thị trường mới thay vì cạnh tranh đẫm máu."),
                Map.entry(32L, "Nghiên cứu yếu tố giúp doanh nghiệp từ tốt trở nên vĩ đại."),
                Map.entry(33L, "Những nguyên tắc tâm lý chi phối tiền bạc và quản lý cảm xúc tài chính."),
                Map.entry(34L, "Nguyên tắc sống và đầu tư của Ray Dalio với quy trình ra quyết định minh bạch."),
                Map.entry(35L, "Câu chuyện về công lý và lòng nhân ái ở miền Nam nước Mỹ qua mắt cô bé Scout."),
                Map.entry(36L, "Bức tranh xã hội toàn trị u ám và hành trình phản kháng của Winston Smith."),
                Map.entry(37L, "Bi kịch và hào nhoáng thời Jazz Age qua số phận Jay Gatsby."),
                Map.entry(38L, "Hành trình tâm linh tìm kho báu và khám phá bản ngã của Santiago."),
                Map.entry(39L, "Chuyến du hành của Hoàng tử Bé khám phá tình yêu và sự trưởng thành."),
                Map.entry(40L, "Cuộc phiêu lưu của Bilbo Baggins tới Núi Cô Đơn cùng đoàn người lùn."),
                Map.entry(41L, "Tình bạn và chuộc lỗi trong bối cảnh Afghanistan biến động."),
                Map.entry(42L, "Chuyện tình tuổi trẻ cô đơn, day dứt của Murakami ở Tokyo."),
                Map.entry(43L, "Những vụ án ly kỳ của thám tử Sherlock Holmes và bác sĩ Watson."),
                Map.entry(44L, "Chuyện tình châm biếm tầng lớp quý tộc Anh giữa Elizabeth Bennet và Mr. Darcy."),
                Map.entry(45L, "Nhật ký hài hước của cậu bé Greg Heffley ở trường trung học."),
                Map.entry(46L, "Tuyển tập truyện ngắn Doraemon dí dỏm cùng các bảo bối thần kỳ."),
                Map.entry(47L, "Mở đầu chuỗi phiêu lưu Kính Vạn Hoa của nhóm bạn tuổi học trò."),
                Map.entry(48L, "Khám phá vũ trụ, thời gian và lỗ đen qua ngòi bút Stephen Hawking."),
                Map.entry(49L, "Lược sử loài người từ săn bắt, nông nghiệp tới kỷ nguyên dữ liệu."),
                Map.entry(50L, "Hướng dẫn thiên văn phổ thông, du hành vũ trụ cùng Carl Sagan."),
                Map.entry(51L, "Thuyết gene ích kỷ và cách tiến hóa định hình hành vi sinh học."),
                Map.entry(52L, "Tương lai loài người trước AI, sinh học và những lựa chọn tiến hóa mới."),
                Map.entry(53L, "Lịch sử thân mật của gene và tác động của di truyền tới y học."),
                Map.entry(54L, "Tóm tắt nhanh về vũ trụ và vật lý thiên văn cho người bận rộn."),
                Map.entry(55L, "Chiến lược làm việc sâu, tập trung để nâng cao hiệu suất."),
                Map.entry(56L, "Thiết kế sản phẩm gây nghiện với mô hình hook và trải nghiệm người dùng.")
        );
    }
}

