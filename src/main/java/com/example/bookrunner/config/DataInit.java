package com.example.bookrunner.config;

import com.example.bookrunner.enums.Role;
import com.example.bookrunner.model.Book;
import com.example.bookrunner.model.Cart;
import com.example.bookrunner.model.Category;
import com.example.bookrunner.model.User;
import com.example.bookrunner.repository.BookRepository;
import com.example.bookrunner.repository.CartRepository;
import com.example.bookrunner.repository.CategoryRepository;
import com.example.bookrunner.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInit implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final CategoryRepository categoryRepository;
    private final BookRepository bookRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("========== BẮT ĐẦU KIỂM TRA & KHỞI TẠO DỮ LIỆU BAN ĐẦU (DATA INIT) ==========");
        initUsers();
        Map<String, Category> categories = initCategories();
        initBooks(categories);
        log.info("========== HOÀN TẤT KHỞI TẠO DỮ LIỆU BAN ĐẦU ==========");
    }

    private void initUsers() {
        if (userRepository.count() > 0) {
            log.info("[DataInit] Bảng 'users' đã có dữ liệu. Bỏ qua khởi tạo tài khoản.");
            return;
        }

        log.info("[DataInit] Đang khởi tạo tài khoản người dùng mặc định...");

        // 1. Tài khoản Quản trị viên (Admin)
        User admin = User.builder()
                .username("admin")
                .email("admin@example.com")
                .password(passwordEncoder.encode("Password123"))
                .fullName("Quản Trị Viên")
                .phone("0988888888")
                .address("Hà Nội, Việt Nam")
                .role(Role.ROLE_ADMIN)
                .enabled(true)
                .tokenVersion(1L)
                .refreshTokenJti(UUID.randomUUID().toString())
                .build();
        User savedAdmin = userRepository.save(admin);
        cartRepository.save(Cart.builder().user(savedAdmin).build());

        // 2. Tài khoản Khách hàng thử nghiệm (Customer 01)
        User customer1 = User.builder()
                .username("customer01")
                .email("customer01@example.com")
                .password(passwordEncoder.encode("Password123"))
                .fullName("Nguyễn Văn A")
                .phone("0901234567")
                .address("123 Đường Cầu Giấy, Hà Nội")
                .role(Role.ROLE_CUSTOMER)
                .enabled(true)
                .tokenVersion(1L)
                .refreshTokenJti(UUID.randomUUID().toString())
                .build();
        User savedCustomer1 = userRepository.save(customer1);
        cartRepository.save(Cart.builder().user(savedCustomer1).build());

        // 3. Tài khoản Khách hàng thử nghiệm (Customer 02)
        User customer2 = User.builder()
                .username("customer02")
                .email("customer02@example.com")
                .password(passwordEncoder.encode("Password123"))
                .fullName("Trần Thị B")
                .phone("0912345678")
                .address("456 Đường Nguyễn Huệ, TP. Hồ Chí Minh")
                .role(Role.ROLE_CUSTOMER)
                .enabled(true)
                .tokenVersion(1L)
                .refreshTokenJti(UUID.randomUUID().toString())
                .build();
        User savedCustomer2 = userRepository.save(customer2);
        cartRepository.save(Cart.builder().user(savedCustomer2).build());

        log.info("[DataInit] Đã tạo thành công 3 tài khoản mặc định (admin, customer01, customer02) kèm giỏ hàng!");
    }

    private Map<String, Category> initCategories() {
        Map<String, Category> categoryMap = new HashMap<>();

        if (categoryRepository.count() > 0) {
            log.info("[DataInit] Bảng 'categories' đã có dữ liệu. Đang tải danh mục hiện có...");
            categoryRepository.findAll().forEach(cat -> categoryMap.put(cat.getSlug(), cat));
            return categoryMap;
        }

        log.info("[DataInit] Đang khởi tạo danh mục sách mẫu...");

        List<Category> categories = List.of(
                Category.builder()
                        .name("Lập trình & CNTT")
                        .slug("lap-trinh-cntt")
                        .description("Sách chuyên ngành công nghệ thông tin, lập trình, kiến trúc hệ thống và cơ sở dữ liệu.")
                        .build(),
                Category.builder()
                        .name("Kỹ năng sống & Phát triển bản thân")
                        .slug("ky-nang-song")
                        .description("Sách rèn luyện kỹ năng mềm, tư duy thành công, quản lý thời gian và phát triển bản thân.")
                        .build(),
                Category.builder()
                        .name("Kinh tế & Khởi nghiệp")
                        .slug("kinh-te-khoi-nghiep")
                        .description("Sách tài chính, đầu tư, quản trị kinh doanh, marketing và tư duy làm giàu.")
                        .build(),
                Category.builder()
                        .name("Văn học & Tiểu thuyết")
                        .slug("van-hoc-tieu-thuyet")
                        .description("Các tác phẩm văn học kinh điển, tiểu thuyết nổi tiếng trong nước và quốc tế.")
                        .build(),
                Category.builder()
                        .name("Khoa học & Đời sống")
                        .slug("khoa-hoc-doi-song")
                        .description("Sách khoa học tự nhiên, vũ trụ, sinh học và các phát minh vĩ đại của nhân loại.")
                        .build()
        );

        List<Category> savedCategories = categoryRepository.saveAll(categories);
        savedCategories.forEach(cat -> categoryMap.put(cat.getSlug(), cat));

        log.info("[DataInit] Đã tạo thành công {} danh mục mẫu!", savedCategories.size());
        return categoryMap;
    }

    private void initBooks(Map<String, Category> categories) {
        if (bookRepository.count() > 0) {
            log.info("[DataInit] Bảng 'books' đã có dữ liệu. Bỏ qua khởi tạo sách.");
            return;
        }

        log.info("[DataInit] Đang khởi tạo danh sách sách mẫu...");

        List<Book> books = List.of(
                Book.builder()
                        .title("Clean Code - Mã Sạch")
                        .author("Robert C. Martin")
                        .publisher("NXB Thông Tin & Truyền Thông")
                        .publicationYear(2020)
                        .isbn("978-6048037321")
                        .description("Cẩm nang hướng dẫn viết mã nguồn rõ ràng, dễ bảo trì và mở rộng dành cho lập trình viên chuyên nghiệp.")
                        .price(new BigDecimal("250000"))
                        .discountPrice(new BigDecimal("200000"))
                        .stockQuantity(50)
                        .pageCount(464)
                        .language("Tiếng Việt")
                        .averageRating(4.8)
                        .totalReviews(25)
                        .active(true)
                        .isFeatured(true)
                        .soldCount(120)
                        .coverImageUrl("https://images.unsplash.com/photo-1532012164546-f432f2e3777a?w=500")
                        .category(categories.get("lap-trinh-cntt"))
                        .build(),

                Book.builder()
                        .title("Design Patterns: Elements of Reusable Object-Oriented Software")
                        .author("Erich Gamma, Richard Helm, Ralph Johnson, John Vlissides")
                        .publisher("NXB Khoa Học Kỹ Thuật")
                        .publicationYear(2021)
                        .isbn("978-0201633610")
                        .description("23 mẫu thiết kế phần mềm hướng đối tượng kinh điển định hình ngành công nghiệp phần mềm thế giới.")
                        .price(new BigDecimal("320000"))
                        .discountPrice(new BigDecimal("280000"))
                        .stockQuantity(35)
                        .pageCount(395)
                        .language("Tiếng Anh")
                        .averageRating(4.9)
                        .totalReviews(18)
                        .active(true)
                        .isFeatured(true)
                        .soldCount(85)
                        .coverImageUrl("https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=500")
                        .category(categories.get("lap-trinh-cntt"))
                        .build(),

                Book.builder()
                        .title("Spring Boot in Action")
                        .author("Craig Walls")
                        .publisher("Manning Publications")
                        .publicationYear(2022)
                        .isbn("978-1617292545")
                        .description("Hướng dẫn thực chiến xây dựng ứng dụng Java Enterprise và Microservices tốc độ cao với Spring Boot.")
                        .price(new BigDecimal("290000"))
                        .discountPrice(null)
                        .stockQuantity(40)
                        .pageCount(264)
                        .language("Tiếng Anh")
                        .averageRating(4.5)
                        .totalReviews(10)
                        .active(true)
                        .isFeatured(false)
                        .soldCount(45)
                        .coverImageUrl("https://images.unsplash.com/photo-1512820790803-83ca734da794?w=500")
                        .category(categories.get("lap-trinh-cntt"))
                        .build(),

                Book.builder()
                        .title("Đắc Nhân Tâm (How to Win Friends and Influence People)")
                        .author("Dale Carnegie")
                        .publisher("NXB First News - Trí Việt")
                        .publicationYear(2021)
                        .isbn("978-6045881477")
                        .description("Cuốn sách nghệ thuật ứng xử, nghệ thuật giao tiếp và thu phục lòng người bán chạy nhất mọi thời đại.")
                        .price(new BigDecimal("120000"))
                        .discountPrice(new BigDecimal("96000"))
                        .stockQuantity(100)
                        .pageCount(320)
                        .language("Tiếng Việt")
                        .averageRating(4.9)
                        .totalReviews(89)
                        .active(true)
                        .isFeatured(true)
                        .soldCount(350)
                        .coverImageUrl("https://images.unsplash.com/photo-1543002588-bfa74002ed7e?w=500")
                        .category(categories.get("ky-nang-song"))
                        .build(),

                Book.builder()
                        .title("Tư Duy Nhanh Và Chậm (Thinking, Fast and Slow)")
                        .author("Daniel Kahneman")
                        .publisher("NXB Thế Giới")
                        .publicationYear(2020)
                        .isbn("978-6047721894")
                        .description("Tác phẩm kinh điển đoạt giải Nobel khám phá hai hệ thống tư duy chi phối cách con người ra quyết định.")
                        .price(new BigDecimal("210000"))
                        .discountPrice(new BigDecimal("175000"))
                        .stockQuantity(60)
                        .pageCount(600)
                        .language("Tiếng Việt")
                        .averageRating(4.7)
                        .totalReviews(42)
                        .active(true)
                        .isFeatured(true)
                        .soldCount(150)
                        .coverImageUrl("https://images.unsplash.com/photo-1497633762265-9d179a990aa6?w=500")
                        .category(categories.get("ky-nang-song"))
                        .build(),

                Book.builder()
                        .title("Nhà Giả Kim (The Alchemist)")
                        .author("Paulo Coelho")
                        .publisher("NXB Hội Nhà Văn")
                        .publicationYear(2022)
                        .isbn("978-6049899317")
                        .description("Câu chuyện ngụ ngôn phương Đông giàu triết lý về hành trình theo đuổi ước mơ và vận mệnh của mỗi con người.")
                        .price(new BigDecimal("89000"))
                        .discountPrice(new BigDecimal("72000"))
                        .stockQuantity(80)
                        .pageCount(228)
                        .language("Tiếng Việt")
                        .averageRating(4.8)
                        .totalReviews(120)
                        .active(true)
                        .isFeatured(true)
                        .soldCount(500)
                        .coverImageUrl("https://images.unsplash.com/photo-1516979187457-637abb4f9353?w=500")
                        .category(categories.get("van-hoc-tieu-thuyet"))
                        .build(),

                Book.builder()
                        .title("Rừng Na Uy (Norwegian Wood)")
                        .author("Haruki Murakami")
                        .publisher("NXB Hội Nhà Văn")
                        .publicationYear(2021)
                        .isbn("978-6049899454")
                        .description("Kiệt tác văn học Nhật Bản về tình yêu, sự mất mát và nỗi cô đơn của tuổi trẻ những năm 1960.")
                        .price(new BigDecimal("140000"))
                        .discountPrice(new BigDecimal("119000"))
                        .stockQuantity(45)
                        .pageCount(540)
                        .language("Tiếng Việt")
                        .averageRating(4.6)
                        .totalReviews(35)
                        .active(true)
                        .isFeatured(false)
                        .soldCount(110)
                        .coverImageUrl("https://images.unsplash.com/photo-1476275466078-4007374efbbe?w=500")
                        .category(categories.get("van-hoc-tieu-thuyet"))
                        .build(),

                Book.builder()
                        .title("Chiến Tranh Tiền Tệ")
                        .author("Song Hongbing")
                        .publisher("NXB Lao Động Xã Hội")
                        .publicationYear(2020)
                        .isbn("978-6045972885")
                        .description("Bức tranh toàn cảnh về lịch sử tài chính thế giới và những cuộc chiến ngầm giữa các đế chế ngân hàng.")
                        .price(new BigDecimal("185000"))
                        .discountPrice(new BigDecimal("150000"))
                        .stockQuantity(35)
                        .pageCount(480)
                        .language("Tiếng Việt")
                        .averageRating(4.3)
                        .totalReviews(22)
                        .active(true)
                        .isFeatured(false)
                        .soldCount(90)
                        .coverImageUrl("https://images.unsplash.com/photo-1590283603385-17ffb3a7f29f?w=500")
                        .category(categories.get("kinh-te-khoi-nghiep"))
                        .build(),

                Book.builder()
                        .title("Từ Tốt Đến Vĩ Đại (Good to Great)")
                        .author("Jim Collins")
                        .publisher("NXB Trẻ")
                        .publicationYear(2021)
                        .isbn("978-6041189447")
                        .description("Nghiên cứu mang tính đột phá về lý do tại sao một số công ty tạo ra bước nhảy vọt vượt bậc còn số khác thì không.")
                        .price(new BigDecimal("165000"))
                        .discountPrice(new BigDecimal("135000"))
                        .stockQuantity(55)
                        .pageCount(400)
                        .language("Tiếng Việt")
                        .averageRating(4.8)
                        .totalReviews(40)
                        .active(true)
                        .isFeatured(true)
                        .soldCount(175)
                        .coverImageUrl("https://images.unsplash.com/photo-1553484771-371a605b060b?w=500")
                        .category(categories.get("kinh-te-khoi-nghiep"))
                        .build(),

                Book.builder()
                        .title("Lược Sử Thời Gian (A Brief History of Time)")
                        .author("Stephen Hawking")
                        .publisher("NXB Trẻ")
                        .publicationYear(2020)
                        .isbn("978-6041164994")
                        .description("Tác phẩm kinh điển về vật lý vũ trụ học từ Vụ nổ lớn Big Bang đến Lỗ đen của thiên tài Stephen Hawking.")
                        .price(new BigDecimal("145000"))
                        .discountPrice(new BigDecimal("120000"))
                        .stockQuantity(50)
                        .pageCount(280)
                        .language("Tiếng Việt")
                        .averageRating(4.7)
                        .totalReviews(55)
                        .active(true)
                        .isFeatured(true)
                        .soldCount(200)
                        .coverImageUrl("https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=500")
                        .category(categories.get("khoa-hoc-doi-song"))
                        .build()
        );

        List<Book> savedBooks = bookRepository.saveAll(books);
        log.info("[DataInit] Đã tạo thành công {} cuốn sách mẫu!", savedBooks.size());
    }
}
