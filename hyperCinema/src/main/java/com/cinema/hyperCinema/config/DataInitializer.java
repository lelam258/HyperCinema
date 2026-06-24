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
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
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
        }

        List<Language> languages = ensureLanguages();
        List<Genre> currentGenres = ensureGenres();
        List<Movie> movies = ensureMovies(languages);
        ensureMovieGenres(movies, currentGenres);
        ensureShowtimes(movies, hallRepository.findAll());

        System.out.println("=== DataInitializer: Hoàn tất seed dữ liệu mẫu ===");
    }

    // ───────────────────── ROLES ─────────────────────
    private List<Role> seedRoles() {
        List<Role> roles = new ArrayList<>();
        for (String name : new String[]{"Admin", "Manager", "BranchManager", "Staff", "Customer"}) {
            Role r = new Role();
            r.setName(name);
            roles.add(roleRepository.save(r));
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
                {"Bắp rang bơ (L)", 45000, "Bắp rang"},
                {"Bắp rang phô mai (L)", 55000, "Bắp rang"},
                {"Coca-Cola (L)", 30000, "Nước uống"},
                {"Pepsi (L)", 30000, "Nước uống"},
                {"Nước suối", 15000, "Nước uống"},
                {"Combo Couple", 120000, "Combo"},
                {"Combo Gia đình", 180000, "Combo"},
                {"Hotdog", 35000, "Snack"},
                {"Nachos phô mai", 40000, "Snack"},
                {"Kem ốc quế", 25000, "Snack"},
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

    // ───────────────────── BOOKINGS + PAYMENTS ─────────────────────
    private void seedBookingsAndPayments(List<User> users, List<Showtime> showtimes, List<Seat> allSeats) {
        List<User> customers = users.subList(11, Math.min(21, users.size()));
        String[] statuses = {"Confirmed", "Confirmed", "Confirmed", "Completed", "Completed", "Cancelled"};
        String[] methods = {"VNPay", "VietQR", "Cash", "VNPay", "VietQR"};

        int seatIdx = 0;
        for (int i = 0; i < 50 && i < showtimes.size(); i++) {
            Showtime st = showtimes.get(i % showtimes.size());
            User customer = customers.get(i % customers.size());
            String bookingStatus = statuses[i % statuses.length];

            int ticketCount = 1 + random.nextInt(3);
            List<Seat> bookingSeats = new ArrayList<>(ticketCount);
            for (int t = 0; t < ticketCount && seatIdx + t < allSeats.size(); t++) {
                bookingSeats.add(allSeats.get((seatIdx + t) % allSeats.size()));
            }
            long totalPrice = bookingSeats.stream()
                    .mapToLong(seat -> SeatPricing.priceFor(seat.getType()))
                    .sum();

            Booking booking = new Booking();
            booking.setUser(customer);
            booking.setShowtime(st);
            booking.setTotalPrice(totalPrice);
            booking.setStatus(bookingStatus);
            booking = bookingRepository.save(booking);

            for (int t = 0; t < bookingSeats.size() && seatIdx < allSeats.size(); t++) {
                Ticket ticket = new Ticket();
                ticket.setBooking(booking);
                ticket.setSeat(bookingSeats.get(t));
                ticket.setQrCode("QR-" + booking.getBookingId() + "-" + t);
                ticket.setStatus("Cancelled".equals(bookingStatus) ? "Cancelled" : "Active");
                ticketRepository.save(ticket);
                seatIdx++;
            }

            if (!"Cancelled".equals(bookingStatus)) {
                Payment payment = new Payment();
                payment.setBooking(booking);
                payment.setAmount(totalPrice);
                payment.setMethod(methods[i % methods.length]);
                payment.setStatus("Completed");
                paymentRepository.save(payment);
            }
        }
    }

    // ───────────────────── AUDIT LOGS ─────────────────────
    private void ensureAuditLogTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS `audit_log` (
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
}

