package com.cinema.hyperCinema.config;

import com.cinema.hyperCinema.model.*;
import com.cinema.hyperCinema.repository.*;
import com.cinema.hyperCinema.util.SeatPricing;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Seed dữ liệu mẫu khi DB trống.
 * Chỉ chạy khi bảng Role chưa có record nào.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final GenreRepository genreRepository;
    private final BranchRepository branchRepository;
    private final HallRepository hallRepository;
    private final SeatRepository seatRepository;
    private final MovieRepository movieRepository;
    private final LanguageRepository languageRepository;
    private final MovieGenreRepository movieGenreRepository;
    private final ShowtimeRepository showtimeRepository;
    private final BookingRepository bookingRepository;
    private final TicketRepository ticketRepository;
    private final PaymentRepository paymentRepository;
    private final FoodItemRepository foodItemRepository;
    private final AuditLogRepository auditLogRepository;
    private final FoodOrderRepository foodOrderRepository;
    private final FoodOrderItemRepository foodOrderItemRepository;
    private final UserMembershipRepository userMembershipRepository;
    private final MembershipPlanRepository membershipPlanRepository;
    private final NotificationRepository notificationRepository;
    private final ReviewRepository reviewRepository;
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    private final Random random = new Random(42);

    public DataInitializer(
            RoleRepository roleRepository,
            UserRepository userRepository,
            GenreRepository genreRepository,
            BranchRepository branchRepository,
            HallRepository hallRepository,
            SeatRepository seatRepository,
            MovieRepository movieRepository,
            LanguageRepository languageRepository,
            MovieGenreRepository movieGenreRepository,
            ShowtimeRepository showtimeRepository,
            BookingRepository bookingRepository,
            TicketRepository ticketRepository,
            PaymentRepository paymentRepository,
            FoodItemRepository foodItemRepository,
            AuditLogRepository auditLogRepository,
            FoodOrderRepository foodOrderRepository,
            FoodOrderItemRepository foodOrderItemRepository,
            UserMembershipRepository userMembershipRepository,
            MembershipPlanRepository membershipPlanRepository,
            NotificationRepository notificationRepository,
            ReviewRepository reviewRepository,
            JdbcTemplate jdbcTemplate,
            PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.genreRepository = genreRepository;
        this.branchRepository = branchRepository;
        this.hallRepository = hallRepository;
        this.seatRepository = seatRepository;
        this.movieRepository = movieRepository;
        this.languageRepository = languageRepository;
        this.movieGenreRepository = movieGenreRepository;
        this.showtimeRepository = showtimeRepository;
        this.bookingRepository = bookingRepository;
        this.ticketRepository = ticketRepository;
        this.paymentRepository = paymentRepository;
        this.foodItemRepository = foodItemRepository;
        this.auditLogRepository = auditLogRepository;
        this.foodOrderRepository = foodOrderRepository;
        this.foodOrderItemRepository = foodOrderItemRepository;
        this.userMembershipRepository = userMembershipRepository;
        this.membershipPlanRepository = membershipPlanRepository;
        this.notificationRepository = notificationRepository;
        this.reviewRepository = reviewRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        boolean resetDb = false;
        if (resetDb) {
            System.out.println("=== Resetting database to clear foreign key mismatches ===");
            jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
            String[] tables = {
                "Audit_Log", "Booking", "Branch_Movie", "FoodOrder", "FoodOrderItem",
                "Hall", "Loyalty_Point", "Movie", "Movie_Genre", "Notification",
                "Payment", "Promotion", "Promotion_Usage", "Review", "Role",
                "Seat", "Seat_Reservation", "Showtime", "Ticket", "User", "User_Membership",
                "Branch", "Language", "Membership_Plan", "FoodItem"
            };
            for (String table : tables) {
                try {
                    jdbcTemplate.execute("DROP TABLE IF EXISTS `" + table + "`");
                } catch (Exception e) {
                    System.out.println("Failed to drop " + table + ": " + e.getMessage());
                }
            }
            jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");
            System.out.println("=== Database reset complete. Please restart the application. ===");
            System.exit(0);
        }

        if (userRepository.findByUsername("admin").isPresent()) {
            User admin = userRepository.findByUsername("admin").get();
            if (bookingRepository.count() == 0) {
                System.out.println("=== DataInitializer: admin present, but bookings missing. Seeding showtimes & bookings ===");
                List<User> users = userRepository.findAll();
                List<Branch> branches = branchRepository.findAll();
                List<Hall> halls = hallRepository.findAll();
                List<Seat> allSeats = seatRepository.findAll();
                List<Genre> genres = genreRepository.findAll();
                List<Language> languages = seedLanguages();
                List<Movie> movies = seedMovies(genres, languages);
                List<Showtime> showtimes = seedShowtimes(movies, halls);
                seedBookingsPaymentsAndFood(users, showtimes, allSeats);
                System.out.println("=== DataInitializer: Finished seeding showtimes & bookings ===");
            }
            seedPlansAndMemberships(userRepository.findAll());
            seedInitialNotifications(admin);
            seedReviews(movieRepository.findAll(), userRepository.findAll());
            return;
        }

        if (roleRepository.count() == 0) {
            System.out.println("=== DataInitializer: Bắt đầu seed dữ liệu mẫu ===");

            List<Role> roles = seedRoles();
            List<Genre> genres = seedGenres();
            List<User> users = seedUsers(roles);
            List<Branch> branches = seedBranches();
            assignBranchManagers(users, branches);
            List<Hall> halls = seedHalls(branches);
            List<Seat> allSeats = seedSeats(halls);
            seedFoodItems();
            ensureAuditLogTable();
            seedAuditLogs(users);

            List<Language> languages = seedLanguages();
            List<Movie> movies = seedMovies(genres, languages);
            List<Showtime> showtimes = seedShowtimes(movies, halls);
            seedBookingsPaymentsAndFood(users, showtimes, allSeats);

            // Seed plans, memberships and notifications
            seedPlansAndMemberships(users);
            User admin = users.stream().filter(u -> "admin".equalsIgnoreCase(u.getUsername())).findFirst().orElse(null);
            if (admin != null) {
                seedInitialNotifications(admin);
            }
            seedReviews(movies, users);
        }

        System.out.println("=== DataInitializer: Hoàn tất seed dữ liệu mẫu ===");
    }

    private void seedPlansAndMemberships(List<User> users) {
        if (membershipPlanRepository.count() > 0) return;

        MembershipPlan vip = new MembershipPlan();
        vip.setName("VIP Plan");
        vip.setDiscountPercent(java.math.BigDecimal.valueOf(10.0));
        vip.setPrice(150000);
        vip.setDurationDays(30);
        vip = membershipPlanRepository.save(vip);

        MembershipPlan gold = new MembershipPlan();
        gold.setName("Gold Plan");
        gold.setDiscountPercent(java.math.BigDecimal.valueOf(20.0));
        gold.setPrice(300000);
        gold.setDurationDays(90);
        gold = membershipPlanRepository.save(gold);

        // Assign to customer1 (Nguyễn Minh Tuấn) and customer2 (Trần Thu Hà)
        User customer1 = users.stream().filter(u -> "customer1".equalsIgnoreCase(u.getUsername())).findFirst().orElse(null);
        User customer2 = users.stream().filter(u -> "customer2".equalsIgnoreCase(u.getUsername())).findFirst().orElse(null);

        if (customer1 != null) {
            UserMembership um1 = new UserMembership();
            um1.setUser(customer1);
            um1.setPlan(vip);
            um1.setStartDate(LocalDate.now().minusDays(5));
            um1.setEndDate(LocalDate.now().plusDays(25));
            um1.setStatus("Active");
            userMembershipRepository.save(um1);
        }

        if (customer2 != null) {
            UserMembership um2 = new UserMembership();
            um2.setUser(customer2);
            um2.setPlan(gold);
            um2.setStartDate(LocalDate.now().minusDays(10));
            um2.setEndDate(LocalDate.now().plusDays(80));
            um2.setStatus("Active");
            userMembershipRepository.save(um2);
        }
    }

    private void seedInitialNotifications(User admin) {
        // If notifications are already seeded, look for existing English mock ones and update them to Vietnamese
        List<Notification> existing = notificationRepository.findAll();
        for (Notification n : existing) {
            if ("System Maintenance Scheduled".equalsIgnoreCase(n.getTitle())) {
                n.setTitle("Lịch bảo trì hệ thống");
                n.setMessage("Chúng tôi sẽ tiến hành bảo trì hệ thống vào ngày 26 tháng 5 năm 2026 từ 2:00 sáng đến 4:00 sáng (UTC). Các dịch vụ có thể tạm thời không khả dụng.");
                notificationRepository.save(n);
            } else if ("New Feature Available".equalsIgnoreCase(n.getTitle())) {
                n.setTitle("Tính năng mới khả dụng");
                n.setMessage("Hãy trải nghiệm tính năng phân tích thống kê mới của chúng tôi! Theo dõi và tối ưu hóa hiệu suất dữ liệu dễ dàng hơn bao giờ hết.");
                notificationRepository.save(n);
            } else if ("Security Alert".equalsIgnoreCase(n.getTitle())) {
                n.setTitle("Cảnh báo bảo mật");
                n.setMessage("Chúng tôi phát hiện đăng nhập từ một thiết bị mới. Nếu không phải bạn, vui lòng đổi mật khẩu và bảo mật tài khoản ngay lập tức.");
                notificationRepository.save(n);
            } else if ("Account Verification Required".equalsIgnoreCase(n.getTitle())) {
                n.setTitle("Yêu cầu xác minh tài khoản");
                n.setMessage("Vui lòng xác minh địa chỉ email của bạn để tiếp tục sử dụng tất cả các tính năng của tài khoản.");
                notificationRepository.save(n);
            }
        }

        if (notificationRepository.count() > 0) return;

        // Seed 4 mock notifications matching the wireframes/mockups
        Notification n1 = new Notification();
        n1.setUser(admin);
        n1.setTitle("Lịch bảo trì hệ thống");
        n1.setMessage("Chúng tôi sẽ tiến hành bảo trì hệ thống vào ngày 26 tháng 5 năm 2026 từ 2:00 sáng đến 4:00 sáng (UTC). Các dịch vụ có thể tạm thời không khả dụng.");
        n1.setType("System");
        n1.setRead(true);
        n1.setCreatedAt(LocalDateTime.now().minusHours(9));
        notificationRepository.save(n1);

        Notification n2 = new Notification();
        n2.setUser(admin);
        n2.setTitle("Tính năng mới khả dụng");
        n2.setMessage("Hãy trải nghiệm tính năng phân tích thống kê mới của chúng tôi! Theo dõi và tối ưu hóa hiệu suất dữ liệu dễ dàng hơn bao giờ hết.");
        n2.setType("Promotion");
        n2.setRead(false);
        n2.setCreatedAt(LocalDateTime.now().minusDays(1));
        notificationRepository.save(n2);

        Notification n3 = new Notification();
        n3.setUser(admin);
        n3.setTitle("Cảnh báo bảo mật");
        n3.setMessage("Chúng tôi phát hiện đăng nhập từ một thiết bị mới. Nếu không phải bạn, vui lòng đổi mật khẩu và bảo mật tài khoản ngay lập tức.");
        n3.setType("Alert");
        n3.setRead(true);
        n3.setCreatedAt(LocalDateTime.now().minusDays(1));
        notificationRepository.save(n3);

        Notification n4 = new Notification();
        n4.setUser(admin);
        n4.setTitle("Yêu cầu xác minh tài khoản");
        n4.setMessage("Vui lòng xác minh địa chỉ email của bạn để tiếp tục sử dụng tất cả các tính năng của tài khoản.");
        n4.setType("System");
        n4.setRead(true);
        n4.setCreatedAt(LocalDateTime.now().minusDays(5));
        notificationRepository.save(n4);
    }

    // ───────────────────── ROLES ─────────────────────
    private List<Role> seedRoles() {
        List<Role> roles = new ArrayList<>();
        for (String name : new String[]{"Admin", "Manager", "BranchManager", "Staff", "Customer"}) {
            Role r = roleRepository.findByName(name).orElseGet(() -> {
                Role newRole = new Role();
                newRole.setName(name);
                return roleRepository.save(newRole);
            });
            roles.add(r);
        }
        return roles;
    }

    // ───────────────────── GENRES ─────────────────────
    private List<Language> ensureLanguages() {
        if (languageRepository.count() > 0) {
            return languageRepository.findAll();
        }
        return seedLanguages();
    }

    private List<Language> seedLanguages() {
        List<Language> languages = new ArrayList<>();
        for (String name : new String[]{"Vietnamese", "English", "Japanese", "Korean"}) {
            Language language = new Language();
            language.setName(name);
            languages.add(languageRepository.save(language));
        }
        return languages;
    }

    private List<Genre> ensureGenres() {
        if (genreRepository.count() > 0) {
            return genreRepository.findAll();
        }
        return seedGenres();
    }

    private List<Genre> seedGenres() {
        List<Genre> genres = new ArrayList<>();
        for (String name : new String[]{
                "Hành động", "Kinh dị", "Hài", "Tình cảm",
                "Khoa học viễn tưởng", "Hoạt hình", "Tâm lý", "Phiêu lưu"}) {
            Genre g = new Genre();
            g.setName(name);
            genres.add(genreRepository.save(g));
        }
        return genres;
    }

    // ───────────────────── USERS ─────────────────────
    private List<Movie> ensureMovies(List<Language> languages) {
        if (movieRepository.count() > 0) {
            return movieRepository.findAll();
        }
        return seedMovies(languages);
    }

    private List<Movie> seedMovies(List<Language> languages) {
        List<Movie> movies = new ArrayList<>();
        if (languages.isEmpty()) {
            return movies;
        }

        Language vietnamese = languages.get(0);
        Language english = languages.size() > 1 ? languages.get(1) : vietnamese;
        Language japanese = languages.size() > 2 ? languages.get(2) : vietnamese;
        Language korean = languages.size() > 3 ? languages.get(3) : vietnamese;

        movies.add(createMovie("Lat Mat 8", 135,
                "A Vietnamese family drama with action and comedy elements.",
                LocalDate.of(2025, 4, 30), "NowShowing", vietnamese));
        movies.add(createMovie("Detective Conan: One-eyed Flashback", 110,
                "A mystery case follows Conan and the police into a dangerous mountain incident.",
                LocalDate.of(2025, 4, 18), "NowShowing", japanese));
        movies.add(createMovie("Doraemon: Nobita's Art World Tales", 105,
                "Nobita and friends enter a magical world hidden inside famous paintings.",
                LocalDate.of(2025, 5, 23), "NowShowing", japanese));
        movies.add(createMovie("Mission: Impossible - The Final Reckoning", 169,
                "Ethan Hunt faces a final mission against a global AI threat.",
                LocalDate.of(2025, 5, 21), "NowShowing", english));
        movies.add(createMovie("How to Train Your Dragon", 125,
                "A live-action fantasy adventure about Hiccup and Toothless.",
                LocalDate.of(2025, 6, 13), "ComingSoon", english));
        movies.add(createMovie("F1 The Movie", 155,
                "A former driver returns to Formula 1 to mentor a young racing talent.",
                LocalDate.of(2025, 6, 27), "ComingSoon", english));
        movies.add(createMovie("The Roundup: Punishment", 109,
                "Detective Ma Seok-do hunts a criminal network across borders.",
                LocalDate.of(2024, 4, 24), "Ended", korean));
        movies.add(createMovie("Godzilla x Kong: The New Empire", 115,
                "Two titans face a hidden threat from deep within the Hollow Earth.",
                LocalDate.of(2024, 3, 29), "Ended", english));

        return movies;
    }

    private Movie createMovie(String title, Integer duration, String description,
                              LocalDate releaseDate, String status, Language language) {
        Movie movie = new Movie();
        movie.setTitle(title);
        movie.setDuration(duration);
        movie.setDescription(description);
        movie.setReleaseDate(releaseDate);
        movie.setStatus(status);
        movie.setLanguageId(language.getLanguageId());
        movie.setCreatedAt(LocalDateTime.now());
        return movieRepository.save(movie);
    }

    private void ensureMovieGenres(List<Movie> movies, List<Genre> genres) {
        if (movies.isEmpty() || genres.isEmpty() || movieGenreRepository.count() > 0) {
            return;
        }

        List<MovieGenre> links = new ArrayList<>();
        for (int i = 0; i < movies.size(); i++) {
            Movie movie = movies.get(i);
            Genre firstGenre = genres.get(i % genres.size());
            Genre secondGenre = genres.get((i + 2) % genres.size());

            links.add(createMovieGenre(movie, firstGenre));
            if (!firstGenre.getGenreId().equals(secondGenre.getGenreId())) {
                links.add(createMovieGenre(movie, secondGenre));
            }
        }
        movieGenreRepository.saveAll(links);
    }

    private MovieGenre createMovieGenre(Movie movie, Genre genre) {
        MovieGenre link = new MovieGenre();
        link.setId(new MovieGenreId(movie.getMovieId(), genre.getGenreId()));
        link.setMovie(movie);
        link.setGenre(genre);
        return link;
    }

    private List<User> seedUsers(List<Role> roles) {
        String encodedPw = passwordEncoder.encode("123456");
        Role adminRole = roles.stream().filter(r -> r.getName().equalsIgnoreCase("Admin")).findFirst().orElse(roles.get(0));
        Role managerRole = roles.stream().filter(r -> r.getName().equalsIgnoreCase("Manager")).findFirst().orElse(roles.get(1));
        Role bmRole = roles.stream().filter(r -> r.getName().equalsIgnoreCase("BranchManager")).findFirst().orElse(roles.get(2));
        Role staffRole = roles.stream().filter(r -> r.getName().equalsIgnoreCase("Staff")).findFirst().orElse(roles.get(3));
        Role customerRole = roles.stream().filter(r -> r.getName().equalsIgnoreCase("Customer")).findFirst().orElse(roles.get(4));

        List<User> users = new ArrayList<>();

        // 1 Admin
        users.add(createUser("Nguyễn Văn Admin", "admin@hypercinema.vn", "admin", encodedPw, "0901000001", adminRole));

        // 2 Manager
        users.add(createUser("Trần Thị Quản Lý", "manager1@hypercinema.vn", "manager1", encodedPw, "0901000002", managerRole));
        users.add(createUser("Lê Văn Quản Lý", "manager2@hypercinema.vn", "manager2", encodedPw, "0901000003", managerRole));

        // 3 Branch Manager
        users.add(createUser("Phạm Thị Lan", "bm.hcm@hypercinema.vn", "bm_hcm", encodedPw, "0901000004", bmRole));
        users.add(createUser("Hoàng Văn Minh", "bm.hn@hypercinema.vn", "bm_hanoi", encodedPw, "0901000005", bmRole));
        users.add(createUser("Đặng Thị Hoa", "bm.dn@hypercinema.vn", "bm_danang", encodedPw, "0901000006", bmRole));

        // 5 Staff
        for (int i = 1; i <= 5; i++) {
            users.add(createUser("Nhân viên " + i, "staff" + i + "@hypercinema.vn", "staff" + i, encodedPw, "090200000" + i, staffRole));
        }

        // 10 Customer
        String[] customerNames = {
                "Nguyễn Minh Tuấn", "Trần Thu Hà", "Lê Hoàng Nam", "Phạm Thanh Tâm", "Vũ Thị Mai",
                "Đỗ Quốc Bảo", "Bùi Thị Lan", "Ngô Văn Đức", "Hồ Thị Ngọc", "Dương Minh Khôi"
        };
        for (int i = 0; i < 10; i++) {
            users.add(createUser(customerNames[i], "customer" + (i + 1) + "@gmail.com", "customer" + (i + 1), encodedPw, "093000000" + i, customerRole));
        }

        return users;
    }

    private User createUser(String fullName, String email, String username, String pw, String phone, Role role) {
        User u = new User();
        u.setFullName(fullName);
        u.setEmail(email);
        u.setUsername(username);
        u.setPasswordHash(pw);
        u.setPhone(phone);
        u.setRole(role);
        u.setStatus("Active");
        u.setEmailVerified(true);
        return userRepository.save(u);
    }

    // ───────────────────── BRANCHES ─────────────────────
    private List<Branch> seedBranches() {
        List<Branch> branches = new ArrayList<>();
        branches.add(createBranch("HyperCinema Quận 1", "123 Nguyễn Huệ, Quận 1", "Hồ Chí Minh", "028-3822-0001"));
        branches.add(createBranch("HyperCinema Hoàn Kiếm", "45 Tràng Tiền, Hoàn Kiếm", "Hà Nội", "024-3826-0002"));
        branches.add(createBranch("HyperCinema Hải Châu", "88 Trần Phú, Hải Châu", "Đà Nẵng", "023-6382-0003"));
        return branches;
    }

    private Branch createBranch(String name, String address, String city, String phone) {
        Branch b = new Branch();
        b.setName(name);
        b.setAddress(address);
        b.setCity(city);
        b.setPhone(phone);
        b.setStatus("Active");
        b.setOpeningTime(LocalTime.of(8, 0));
        b.setClosingTime(LocalTime.of(23, 30));
        return branchRepository.save(b);
    }

    // ───────────────────── ASSIGN BRANCH MANAGERS + STAFF ─────────────────────
    private void assignBranchManagers(List<User> users, List<Branch> branches) {
        // BM HCM (index 3) → branch 0
        User bmHcm = users.get(3);
        bmHcm.setBranch(branches.get(0));
        userRepository.save(bmHcm);

        // BM Hanoi (index 4) → branch 1
        User bmHn = users.get(4);
        bmHn.setBranch(branches.get(1));
        userRepository.save(bmHn);

        // BM Danang (index 5) → branch 2
        User bmDn = users.get(5);
        bmDn.setBranch(branches.get(2));
        userRepository.save(bmDn);

        // Staff 1,2 → HCM
        users.get(6).setBranch(branches.get(0));
        users.get(6).setManager(bmHcm);
        userRepository.save(users.get(6));
        users.get(7).setBranch(branches.get(0));
        users.get(7).setManager(bmHcm);
        userRepository.save(users.get(7));

        // Staff 3,4 → Hanoi
        users.get(8).setBranch(branches.get(1));
        users.get(8).setManager(bmHn);
        userRepository.save(users.get(8));
        users.get(9).setBranch(branches.get(1));
        users.get(9).setManager(bmHn);
        userRepository.save(users.get(9));

        // Staff 5 → Danang
        users.get(10).setBranch(branches.get(2));
        users.get(10).setManager(bmDn);
        userRepository.save(users.get(10));
    }

    // ───────────────────── HALLS ─────────────────────
    private List<Hall> seedHalls(List<Branch> branches) {
        List<Hall> halls = new ArrayList<>();
        String[] hallNames = {"Phòng 2D-A", "Phòng 3D-B", "Phòng IMAX"};
        for (Branch branch : branches) {
            for (String name : hallNames) {
                Hall h = new Hall();
                h.setBranch(branch);
                h.setName(name);
                h.setCapacity(50);
                String hallType = "2D";
                if (name.contains("3D")) {
                    hallType = "3D";
                } else if (name.contains("IMAX")) {
                    hallType = "IMAX";
                }
                h.setHallType(hallType);
                h.setHallType(name.contains("IMAX") ? "IMAX" : (name.contains("3D") ? "3D" : "2D"));
                h.setStatus("Active");
                halls.add(hallRepository.save(h));
            }
        }
        return halls;
    }

    // ───────────────────── SEATS ─────────────────────
    private List<Seat> seedSeats(List<Hall> halls) {
        List<Seat> allSeats = new ArrayList<>();
        String[] rows = {"A", "B", "C", "D", "E"};
        for (Hall hall : halls) {
            for (String row : rows) {
                for (int num = 1; num <= 10; num++) {
                    Seat s = new Seat();
                    s.setHall(hall);
                    s.setSeatRow(row);
                    s.setSeatNumber(num);
                    s.setType((row.equals("C") || row.equals("D")) ? "VIP" : "Standard");
                    allSeats.add(seatRepository.save(s));
                }
            }
        }
        return allSeats;
    }

    private void ensureShowtimes(List<Movie> movies, List<Hall> halls) {
        if (showtimeRepository.count() > 0 || movies.isEmpty() || halls.isEmpty()) {
            return;
        }

        List<Movie> activeMovies = movies.stream()
                .filter(movie -> "NowShowing".equals(movie.getStatus()))
                .toList();
        if (activeMovies.isEmpty()) {
            activeMovies = movies;
        }

        LocalDate firstDate = LocalDate.now();
        LocalTime[] times = {
                LocalTime.of(10, 0),
                LocalTime.of(13, 0),
                LocalTime.of(15, 45),
                LocalTime.of(18, 30),
                LocalTime.of(21, 15)
        };

        for (int day = 0; day < 7; day++) {
            for (int hallIndex = 0; hallIndex < halls.size(); hallIndex++) {
                Hall hall = halls.get(hallIndex);
                Movie movie = activeMovies.get((day + hallIndex) % activeMovies.size());
                for (int timeIndex = 0; timeIndex < times.length; timeIndex++) {
                    if (timeIndex > 2 && hallIndex % 2 == 1) {
                        continue;
                    }
                    LocalDateTime startTime = LocalDateTime.of(firstDate.plusDays(day), times[timeIndex]);
                    Showtime showtime = new Showtime();
                    showtime.setMovie(movie);
                    showtime.setHall(hall);
                    showtime.setStartTime(startTime);
                    showtime.setEndTime(startTime.plusMinutes(movie.getDuration() != null ? movie.getDuration() : 120));
                    showtime.setPrice(0);
                    showtimeRepository.save(showtime);
                }
            }
        }
    }

    // ───────────────────── FOOD ITEMS ─────────────────────
    private void seedFoodItems() {
        Object[][] foodData = {
                {"Bắp rang bơ (L)", 45000, "Food"},
                {"Bắp rang phô mai (L)", 55000, "Food"},
                {"Coca-Cola (L)", 30000, "Beverage"},
                {"Pepsi (L)", 30000, "Beverage"},
                {"Nước suối", 15000, "Beverage"},
                {"Combo Couple", 120000, "Combo"},
                {"Combo Gia đình", 180000, "Combo"},
                {"Hotdog", 35000, "Food"},
                {"Nachos phô mai", 40000, "Food"},
                {"Kem ốc quế", 25000, "Food"},
        };
        for (Object[] data : foodData) {
            FoodItem fi = new FoodItem();
            fi.setName((String) data[0]);
            fi.setPrice((Integer) data[1]);
            fi.setCategoryName((String) data[2]);
            fi.setDescription("");
            fi.setStock(100);
            fi.setIsAvailable(true);
            foodItemRepository.save(fi);
        }
    }

    // ───────────────────── LANGUAGES & MOVIES ─────────────────────

    private List<Movie> seedMovies(List<Genre> genres, List<Language> languages) {
        List<Movie> movies;
        if (movieRepository.count() == 0) {
            movies = new ArrayList<>();
            Language en = languages.stream().filter(l -> l.getName().equals("English")).findFirst().orElse(languages.get(0));
            Language vi = languages.stream().filter(l -> l.getName().equals("Vietnamese")).findFirst().orElse(languages.get(0));

            movies.add(createMovie("Avengers: Doomsday", 180, "The Avengers assemble to face Doctor Doom.", LocalDate.of(2026, 5, 1), "NowShowing", en.getLanguageId()));
            movies.add(createMovie("Lật Mặt 8", 120, "Hành trình gia đình đầy tiếng cười và nước mắt.", LocalDate.of(2026, 4, 15), "NowShowing", vi.getLanguageId()));
            movies.add(createMovie("Godzilla x Kong", 115, "The legendary titans unite to face a colossal threat.", LocalDate.of(2026, 4, 1), "NowShowing", en.getLanguageId()));
            movies.add(createMovie("Dune: Part Two", 166, "Paul Atreides unites with the Fremen to seek revenge.", LocalDate.of(2026, 3, 15), "NowShowing", en.getLanguageId()));
        } else {
            movies = movieRepository.findAll();
        }
        return movies;
    }

    private Movie createMovie(String title, int duration, String description, LocalDate releaseDate, String status, Integer languageId) {
        Movie m = new Movie();
        m.setTitle(title);
        m.setDuration(duration);
        m.setDescription(description);
        m.setReleaseDate(releaseDate);
        m.setStatus(status);
        m.setLanguageId(languageId);
        m.setPosterUrl("/images/movies/" + title.toLowerCase().replaceAll("[^a-z0-9]", "_") + ".jpg");
        m.setTrailerUrl("https://youtube.com");
        return movieRepository.save(m);
    }

    // ───────────────────── SHOWTIMES ─────────────────────
    private List<Showtime> seedShowtimes(List<Movie> movies, List<Hall> halls) {
        List<Showtime> showtimes = new ArrayList<>();
        if (showtimeRepository.count() == 0) {
            LocalDate today = LocalDate.now();
            int[] hours = {9, 12, 15, 18, 21};

            for (int day = -35; day <= 3; day++) {
                LocalDate date = today.plusDays(day);

                for (int hIdx = 0; hIdx < halls.size(); hIdx++) {
                    Hall hall = halls.get(hIdx);
                    for (int s = 0; s < 2; s++) {
                        Movie movie = movies.get((hIdx + s + Math.abs(day)) % movies.size());
                        int hour = hours[(hIdx + s) % hours.length];

                        LocalDateTime start = LocalDateTime.of(date, LocalTime.of(hour, 0));
                        LocalDateTime end = start.plusMinutes(movie.getDuration());

                        Showtime st = new Showtime();
                        st.setMovie(movie);
                        st.setHall(hall);
                        st.setStartTime(start);
                        st.setEndTime(end);
                        st.setPrice(80000 + random.nextInt(8) * 10000); // 80k - 150k
                        showtimes.add(showtimeRepository.save(st));
                    }
                }
            }
        } else {
            showtimes = showtimeRepository.findAll();
        }
        return showtimes;
    }

    // ───────────────────── BOOKINGS, PAYMENTS & FOOD ORDERS ─────────────────────
    private void seedBookingsPaymentsAndFood(List<User> users, List<Showtime> showtimes, List<Seat> seats) {
        if (bookingRepository.count() > 0) return;

        List<User> customers = users.stream()
                .filter(u -> u.getRole().getName().equalsIgnoreCase("Customer"))
                .toList();
        if (customers.isEmpty()) {
            customers = users;
        }

        List<FoodItem> foodItems = foodItemRepository.findAll();

        java.util.Map<Integer, List<Seat>> hallSeatsMap = new java.util.HashMap<>();
        for (Seat seat : seats) {
            hallSeatsMap.computeIfAbsent(seat.getHall().getHallId(), k -> new ArrayList<>()).add(seat);
        }

        String[] paymentMethods = {"VNPay", "VietQR", "Credit Card", "Cash"};
        LocalDateTime now = LocalDateTime.now();

        System.out.println("=== Seeding bookings, payments and food orders ===");
        int bookingCount = 0;
        for (Showtime st : showtimes) {
            if (st.getStartTime().isAfter(now)) {
                continue;
            }

            if (random.nextDouble() > 0.8) {
                continue;
            }

            List<Seat> hallSeats = hallSeatsMap.get(st.getHall().getHallId());
            if (hallSeats == null || hallSeats.isEmpty()) {
                continue;
            }

            double occupancy = 0.2 + random.nextDouble() * 0.7;
            int seatsToBookCount = (int) (hallSeats.size() * occupancy);

            List<Seat> shuffledSeats = new ArrayList<>(hallSeats);
            java.util.Collections.shuffle(shuffledSeats, random);

            int seatPointer = 0;
            while (seatPointer < seatsToBookCount && seatPointer < shuffledSeats.size()) {
                int numSeats = 1 + random.nextInt(3);
                if (seatPointer + numSeats > seatsToBookCount) {
                    numSeats = seatsToBookCount - seatPointer;
                }
                if (numSeats <= 0) break;

                User customer = customers.get(random.nextInt(customers.size()));

                boolean isCompleted = random.nextDouble() < 0.85;
                String status = isCompleted ? "Completed" : "Cancelled";

                long ticketPriceTotal = (long) st.getPrice() * numSeats;

                Booking booking = new Booking();
                booking.setUser(customer);
                booking.setShowtime(st);
                booking.setTotalPrice(ticketPriceTotal);
                booking.setStatus(status);
                booking.setCreatedAt(st.getStartTime().minusHours(1 + random.nextInt(48)));
                booking = bookingRepository.save(booking);

                for (int t = 0; t < numSeats; t++) {
                    Seat seat = shuffledSeats.get(seatPointer + t);
                    Ticket ticket = new Ticket();
                    ticket.setBooking(booking);
                    ticket.setSeat(seat);
                    ticket.setQrCode("QR-" + booking.getBookingId() + "-" + seat.getSeatRow() + seat.getSeatNumber());
                    ticket.setStatus(isCompleted ? "Active" : "Cancelled");
                    ticketRepository.save(ticket);
                }

                seatPointer += numSeats;

                if (isCompleted) {
                    Payment payment = new Payment();
                    payment.setBooking(booking);
                    payment.setAmount(ticketPriceTotal);
                    payment.setMethod(paymentMethods[random.nextInt(paymentMethods.length)]);
                    payment.setStatus("Completed");
                    payment.setCreatedAt(booking.getCreatedAt().plusMinutes(random.nextInt(15)));
                    paymentRepository.save(payment);

                    if (random.nextBoolean() && !foodItems.isEmpty()) {
                        FoodOrder foodOrder = new FoodOrder();
                        foodOrder.setBooking(booking);
                        foodOrder.setStatus("Completed");
                        foodOrder.setCreatedAt(booking.getCreatedAt());

                        List<FoodOrderItem> orderItems = new ArrayList<>();
                        int numItems = 1 + random.nextInt(3);
                        long foodTotal = 0;

                        List<FoodItem> shuffledFood = new ArrayList<>(foodItems);
                        java.util.Collections.shuffle(shuffledFood, random);

                        for (int f = 0; f < numItems && f < shuffledFood.size(); f++) {
                            FoodItem item = shuffledFood.get(f);
                            int qty = 1 + random.nextInt(2);

                            FoodOrderItem orderItem = new FoodOrderItem();
                            orderItem.setFoodOrder(foodOrder);
                            orderItem.setFoodItem(item);
                            orderItem.setItemId(item.getItemId());
                            orderItem.setQuantity(qty);
                            orderItem.setUnitPrice(item.getPrice());
                            orderItems.add(orderItem);

                            foodTotal += (long) item.getPrice() * qty;
                        }

                        foodOrder.setTotalAmount((int) foodTotal);
                        foodOrder.setItems(new ArrayList<>());
                        final FoodOrder savedFoodOrder = foodOrderRepository.save(foodOrder);

                        for (FoodOrderItem item : orderItems) {
                            item.setOrderId(savedFoodOrder.getOrderId());
                            savedFoodOrder.getItems().add(item);
                        }
                    }
                }

                bookingCount++;
            }
        }
        System.out.println("=== Seeded " + bookingCount + " bookings. ===");
    }

    // ───────────────────── AUDIT LOGS ─────────────────────
    private void ensureAuditLogTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS `Audit_Log` (
                    `log_id` INT AUTO_INCREMENT PRIMARY KEY,
                    `user_id` INT NOT NULL,
                    `entity_type` VARCHAR(100) NOT NULL,
                    `entity_id` INT NOT NULL,
                    `action` VARCHAR(50) NOT NULL,
                    `details` TEXT NOT NULL,
                    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
    }

    private void seedAuditLogs(List<User> users) {
        User admin = users.get(0);
        User manager = users.get(1);

        String[][] logData = {
                {"User", "1", "CREATE", "Tạo tài khoản manager1"},
                {"Branch", "1", "CREATE", "Tạo chi nhánh HyperCinema Quận 1"},
                {"Branch", "2", "CREATE", "Tạo chi nhánh HyperCinema Hoàn Kiếm"},
                {"Branch", "3", "CREATE", "Tạo chi nhánh HyperCinema Hải Châu"},
                {"Movie", "1", "CREATE", "Thêm phim Lật Mặt 8"},
                {"Movie", "2", "CREATE", "Thêm phim Avengers: Doomsday"},
                {"User", "4", "UPDATE", "Gán Branch Manager cho chi nhánh Quận 1"},
                {"User", "5", "UPDATE", "Gán Branch Manager cho chi nhánh Hoàn Kiếm"},
                {"Movie", "1", "UPDATE", "Đổi trạng thái phim sang NowShowing"},
                {"Branch", "1", "UPDATE", "Cập nhật giờ hoạt động chi nhánh"},
                {"User", "11", "CREATE", "Khách hàng mới đăng ký"},
                {"User", "12", "CREATE", "Khách hàng mới đăng ký"},
                {"Movie", "3", "CREATE", "Thêm phim Godzilla x Kong"},
                {"Branch", "2", "UPDATE", "Cập nhật thông tin chi nhánh Hoàn Kiếm"},
                {"User", "6", "UPDATE", "Gán Staff vào chi nhánh Quận 1"},
        };

        for (int i = 0; i < logData.length; i++) {
            AuditLog log = new AuditLog();
            log.setUser(i < 8 ? admin : manager);
            log.setEntityType(logData[i][0]);
            log.setEntityId(Integer.parseInt(logData[i][1]));
            log.setAction(logData[i][2]);
            log.setDetails(logData[i][3]);
            log.setCreatedAt(LocalDateTime.now().minusHours(logData.length - i));
            auditLogRepository.save(log);
        }
    }

    private void seedReviews(List<Movie> movies, List<User> users) {
        if (reviewRepository.count() > 0) return;

        List<Review> reviews = new ArrayList<>();
        List<User> customers = users.stream()
                .filter(u -> u.getRole() != null && "Customer".equalsIgnoreCase(u.getRole().getName()))
                .toList();

        if (customers.isEmpty() || movies.isEmpty()) return;

        for (Movie movie : movies) {
            if ("Avengers: Doomsday".equalsIgnoreCase(movie.getTitle())) {
                reviews.add(createReview(customers.get(0 % customers.size()), movie, 5, "Phim quá đỉnh! Robert Downey Jr đóng Doctor Doom quá xuất sắc, không bõ công chờ đợi!", 1));
                reviews.add(createReview(customers.get(1 % customers.size()), movie, 4, "Kỹ xảo hoành tráng, cốt truyện lôi cuốn. Đoạn kết thực sự gây bất ngờ lớn.", 2));
                reviews.add(createReview(customers.get(2 % customers.size()), movie, 3, "Hơi nhiều nhân vật nên xem hơi ngợp, nhưng tổng thể vẫn rất đáng xem ngoài rạp.", 3));
            } else if ("Lật Mặt 8".equalsIgnoreCase(movie.getTitle())) {
                reviews.add(createReview(customers.get(3 % customers.size()), movie, 5, "Phim của Lý Hải chưa bao giờ làm tôi thất vọng. Cực kỳ cảm động về gia đình!", 1));
                reviews.add(createReview(customers.get(4 % customers.size()), movie, 5, "Vừa hài hước vừa lấy đi nước mắt của khán giả. Rất khuyên mọi người nên đi xem cùng gia đình.", 2));
                reviews.add(createReview(customers.get(5 % customers.size()), movie, 4, "Cốt truyện tốt, diễn xuất của các diễn viên nhí rất tự nhiên và xúc động.", 4));
            } else if ("Godzilla x Kong".equalsIgnoreCase(movie.getTitle())) {
                reviews.add(createReview(customers.get(6 % customers.size()), movie, 4, "Đánh đấm cực kỳ đã mắt! Godzilla và Kong kết hợp đỉnh cao.", 2));
                reviews.add(createReview(customers.get(7 % customers.size()), movie, 3, "Cốt truyện hơi đơn giản, chủ yếu là xem kỹ xảo và quái thú đánh nhau.", 5));
                reviews.add(createReview(customers.get(8 % customers.size()), movie, 5, "Trải nghiệm âm thanh và hình ảnh tuyệt vời, xem phòng IMAX phê thôi rồi!", 6));
            } else if ("Dune: Part Two".equalsIgnoreCase(movie.getTitle())) {
                reviews.add(createReview(customers.get(9 % customers.size()), movie, 5, "Một siêu phẩm điện ảnh thực sự! Âm nhạc của Hans Zimmer quá xuất sắc.", 3));
                reviews.add(createReview(customers.get(0 % customers.size()), movie, 5, "Tráng lệ, hoành tráng và đầy chiều sâu. Diễn xuất của Timothée Chalamet cực kỳ ấn tượng.", 5));
                reviews.add(createReview(customers.get(1 % customers.size()), movie, 4, "Phim hơi dài nhưng không bị chán, bám rất sát nguyên tác truyện.", 7));
            }
        }
        reviewRepository.saveAll(reviews);
        System.out.println("=== DataInitializer: Seeded " + reviews.size() + " reviews ===");
    }

    private Review createReview(User user, Movie movie, Integer rating, String comment, int daysAgo) {
        Review r = new Review();
        r.setUser(user);
        r.setMovie(movie);
        r.setRating(rating);
        r.setComment(comment);
        r.setCreatedAt(LocalDateTime.now().minusDays(daysAgo).minusHours(random.nextInt(12)));
        return r;
    }
}
