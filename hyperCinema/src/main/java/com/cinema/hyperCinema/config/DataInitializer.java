package com.cinema.hyperCinema.config;

import com.cinema.hyperCinema.model.*;
import com.cinema.hyperCinema.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Seed dữ liệu mẫu khi DB trống.
 * Chỉ chạy khi bảng Role chưa có record nào.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final int REPORT_BOOKING_SEED_TARGET = 1_000;
    private static final int REPORT_COVERAGE_SEED_DAYS = 7;
    private static final int REPORT_COVERAGE_MAX_BOOKING_SEATS = 6;
    private static final int REPORT_FOOD_ORDER_SEED_TARGET = 60;
    private static final int REPORT_FOOD_ORDER_SEED_DAYS = 30;

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
    private final FoodOrderRepository foodOrderRepository;
    private final FoodOrderItemRepository foodOrderItemRepository;
    private final AuditLogRepository auditLogRepository;
    private final MembershipPlanRepository membershipPlanRepository;
    private final NotificationRepository notificationRepository;
    private final ReviewRepository reviewRepository;
    private final UserMembershipRepository userMembershipRepository;
    private final LoyaltyPointRepository loyaltyPointRepository;
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
            FoodOrderRepository foodOrderRepository,
            FoodOrderItemRepository foodOrderItemRepository,
            AuditLogRepository auditLogRepository,
            MembershipPlanRepository membershipPlanRepository,
            NotificationRepository notificationRepository,
            ReviewRepository reviewRepository,
            UserMembershipRepository userMembershipRepository,
            LoyaltyPointRepository loyaltyPointRepository,
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
        this.foodOrderRepository = foodOrderRepository;
        this.foodOrderItemRepository = foodOrderItemRepository;
        this.auditLogRepository = auditLogRepository;
        this.membershipPlanRepository = membershipPlanRepository;
        this.notificationRepository = notificationRepository;
        this.reviewRepository = reviewRepository;
        this.userMembershipRepository = userMembershipRepository;
        this.loyaltyPointRepository = loyaltyPointRepository;
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
        List<Hall> currentHalls = hallRepository.findAll();
        ensureSeats(currentHalls);
        ensureShowtimes(movies, currentHalls);
        ensureFoodItems();
        ensureSeedBranchAssignments();
        ensureSeedCustomers();
        ensureMembershipData();
        ensureBookingsAndPayments();
        userRepository.findByUsername("admin").ifPresent(this::seedInitialNotifications);
        seedReviews(movieRepository.findAll(), userRepository.findAll());

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

    private void ensureSeedBranchAssignments() {
        List<Branch> branches = branchRepository.findAll().stream()
                .sorted((left, right) -> left.getBranchId().compareTo(right.getBranchId()))
                .toList();
        if (branches.size() < 3) {
            return;
        }

        Role managerRole = roleRepository.findAll().stream()
                .filter(role -> "Manager".equalsIgnoreCase(role.getName()))
                .findFirst()
                .orElse(null);
        Role staffRole = roleRepository.findAll().stream()
                .filter(role -> "Staff".equalsIgnoreCase(role.getName()))
                .findFirst()
                .orElse(null);
        if (managerRole == null || staffRole == null) {
            return;
        }

        String encodedPw = passwordEncoder.encode("123456");
        User managerHcm = ensureSeedUser("Quan ly HCM", "manager1@hypercinema.vn", "manager1",
                encodedPw, "0901000002", managerRole);
        User managerHn = ensureSeedUser("Quan ly Ha Noi", "manager2@hypercinema.vn", "manager2",
                encodedPw, "0901000003", managerRole);
        User managerDn = ensureSeedUser("Quan ly Da Nang", "manager3@hypercinema.vn", "manager3",
                encodedPw, "0901000007", managerRole);

        assignSeedManager(managerHcm, branches.get(0));
        assignSeedManager(managerHn, branches.get(1));
        assignSeedManager(managerDn, branches.get(2));

        assignSeedStaff(ensureSeedUser("Nhan vien 1", "staff1@hypercinema.vn", "staff1",
                encodedPw, "0902000001", staffRole), branches.get(0), managerHcm);
        assignSeedStaff(ensureSeedUser("Nhan vien 2", "staff2@hypercinema.vn", "staff2",
                encodedPw, "0902000002", staffRole), branches.get(0), managerHcm);
        assignSeedStaff(ensureSeedUser("Nhan vien 3", "staff3@hypercinema.vn", "staff3",
                encodedPw, "0902000003", staffRole), branches.get(1), managerHn);
        assignSeedStaff(ensureSeedUser("Nhan vien 4", "staff4@hypercinema.vn", "staff4",
                encodedPw, "0902000004", staffRole), branches.get(1), managerHn);
        assignSeedStaff(ensureSeedUser("Nhan vien 5", "staff5@hypercinema.vn", "staff5",
                encodedPw, "0902000005", staffRole), branches.get(2), managerDn);
    }

    private User ensureSeedUser(String fullName,
                                String email,
                                String username,
                                String encodedPw,
                                String phone,
                                Role role) {
        return userRepository.findByUsername(username)
                .orElseGet(() -> createUser(fullName, email, username, encodedPw, phone, role));
    }

    private void assignSeedManager(User manager, Branch branch) {
        manager.setBranch(branch);
        manager.setManager(null);
        manager.setStatus("Active");
        userRepository.save(manager);
    }

    private void assignSeedStaff(User staff, Branch branch, User manager) {
        staff.setBranch(branch);
        staff.setManager(manager);
        staff.setStatus("Active");
        userRepository.save(staff);
    }

    private void ensureSeedCustomers() {
        Role customerRole = roleRepository.findAll().stream()
                .filter(role -> "Customer".equalsIgnoreCase(role.getName()))
                .findFirst()
                .orElse(null);
        if (customerRole == null) {
            return;
        }

        String encodedPw = passwordEncoder.encode("123456");
        String[] customerNames = {
                "Nguyen Minh Tuan", "Tran Thu Ha", "Le Hoang Nam", "Pham Thanh Tam", "Vu Thi Mai",
                "Do Quoc Bao", "Bui Thi Lan", "Ngo Van Duc", "Ho Thi Ngoc", "Duong Minh Khoi"
        };
        for (int i = 0; i < customerNames.length; i++) {
            User customer = ensureSeedUser(customerNames[i],
                    "customer" + (i + 1) + "@gmail.com",
                    "customer" + (i + 1),
                    encodedPw,
                    "093000000" + i,
                    customerRole);
            customer.setStatus("Active");
            customer.setEmailVerified(true);
            userRepository.save(customer);
        }
    }

    private void ensureMembershipData() {
        MembershipPlan silver = ensureMembershipPlan("Silver", "5.00", 99000, 30);
        MembershipPlan gold = ensureMembershipPlan("Gold", "10.00", 199000, 60);
        MembershipPlan platinum = ensureMembershipPlan("Platinum", "15.00", 399000, 90);

        ensureCustomerMembership("customer1", gold, LocalDate.now().minusDays(10), LocalDate.now().plusDays(50), 1200);
        ensureCustomerMembership("customer2", silver, LocalDate.now().minusDays(5), LocalDate.now().plusDays(25), 650);
        ensureCustomerMembership("customer3", platinum, LocalDate.now().minusDays(20), LocalDate.now().plusDays(70), 2600);
        ensureCustomerPointsOnly("customer4", 320);
    }

    private MembershipPlan ensureMembershipPlan(String name, String discountPercent, Integer price, Integer durationDays) {
        return membershipPlanRepository.findAll().stream()
                .filter(plan -> plan.getName() != null && plan.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElseGet(() -> {
                    MembershipPlan plan = new MembershipPlan();
                    plan.setName(name);
                    plan.setDiscountPercent(new BigDecimal(discountPercent));
                    plan.setPrice(price);
                    plan.setDurationDays(durationDays);
                    return membershipPlanRepository.save(plan);
                });
    }

    private void ensureCustomerMembership(String username,
                                          MembershipPlan plan,
                                          LocalDate startDate,
                                          LocalDate endDate,
                                          int points) {
        userRepository.findByUsername(username).ifPresent(user -> {
            boolean hasActiveMembership = userMembershipRepository
                    .findActiveByUserIdWithPlan(user.getUserId(), "ACTIVE", LocalDate.now())
                    .stream()
                    .anyMatch(existing -> existing.getPlan() != null
                            && existing.getPlan().getName() != null
                            && existing.getPlan().getName().equalsIgnoreCase(plan.getName()));
            if (!hasActiveMembership) {
                UserMembership membership = new UserMembership();
                membership.setUser(user);
                membership.setPlan(plan);
                membership.setStartDate(startDate);
                membership.setEndDate(endDate);
                membership.setStatus("ACTIVE");
                userMembershipRepository.save(membership);
            }
            ensureCustomerPoints(user, points);
        });
    }

    private void ensureCustomerPointsOnly(String username, int points) {
        userRepository.findByUsername(username).ifPresent(user -> ensureCustomerPoints(user, points));
    }

    private void ensureCustomerPoints(User user, int points) {
        Long currentPoints = loyaltyPointRepository.sumPointsByUserId(user.getUserId());
        if (currentPoints != null && currentPoints > 0) {
            return;
        }
        LoyaltyPoint loyaltyPoint = new LoyaltyPoint();
        loyaltyPoint.setUser(user);
        loyaltyPoint.setPoints(points);
        loyaltyPoint.setType("EARNED");
        loyaltyPointRepository.save(loyaltyPoint);
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
                h.setTicketPrice(defaultTicketPriceForHall(name));
                h.setStatus("Active");
                Hall saved = hallRepository.save(h);
                ensureSeatTypePrices(saved);
                halls.add(saved);
            }
        }
        return halls;
    }

    private void ensureSeatTypePrices(Hall hall) {
        Integer existing = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM hall_seat_type_price WHERE hall_id = ?",
                Integer.class,
                hall.getHallId());
        if (existing != null && existing > 0) {
            return;
        }
        int base = ticketPriceFor(hall);
        jdbcTemplate.update("INSERT INTO hall_seat_type_price (hall_id, seat_type, price, active) VALUES (?, ?, ?, 1)",
                hall.getHallId(), "STANDARD", base);
        jdbcTemplate.update("INSERT INTO hall_seat_type_price (hall_id, seat_type, price, active) VALUES (?, ?, ?, 1)",
                hall.getHallId(), "VIP", base + 20_000);
        jdbcTemplate.update("INSERT INTO hall_seat_type_price (hall_id, seat_type, price, active) VALUES (?, ?, ?, 1)",
                hall.getHallId(), "COUPLE", base * 2);
        jdbcTemplate.update("INSERT INTO hall_seat_type_price (hall_id, seat_type, price, active) VALUES (?, ?, ?, 1)",
                hall.getHallId(), "DISABLED", 0);
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
                    s.setType((row.equals("C") || row.equals("D")) ? "VIP" : "STANDARD");
                    allSeats.add(seatRepository.save(s));
                }
            }
        }
        return allSeats;
    }

    private void ensureSeats(List<Hall> halls) {
        List<Hall> hallsWithoutSeats = halls.stream()
                .filter(hall -> seatRepository
                        .findByHall_HallIdOrderBySeatRowAscSeatNumberAsc(hall.getHallId())
                        .isEmpty())
                .toList();
        if (!hallsWithoutSeats.isEmpty()) {
            seedSeats(hallsWithoutSeats);
        }
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
                    showtime.setPrice(ticketPriceFor(hall));
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

    private void ensureFoodItems() {
        if (foodItemRepository.count() == 0) {
            seedFoodItems();
        }
    }

    // ───────────────────── BOOKINGS + PAYMENTS ─────────────────────
    private void ensureBookingsAndPayments() {
        long existingBookings = bookingRepository.count();
        long existingTickets = ticketRepository.count();

        if (existingBookings > 0 && existingTickets == 0) {
            System.out.println("=== DataInitializer: Database out of sync (Bookings: " + existingBookings + ", Tickets: " + existingTickets + "). Clearing and re-seeding... ===");
            try {
                jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
                jdbcTemplate.execute("TRUNCATE TABLE food_order_item");
                jdbcTemplate.execute("TRUNCATE TABLE food_order");
                jdbcTemplate.execute("TRUNCATE TABLE payment");
                jdbcTemplate.execute("TRUNCATE TABLE ticket");
                jdbcTemplate.execute("TRUNCATE TABLE booking");
                jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");
                existingBookings = 0;
            } catch (Exception e) {
                System.out.println("=== DataInitializer: Failed to truncate, deleting in batch instead ===");
                try {
                    foodOrderItemRepository.deleteAllInBatch();
                    foodOrderRepository.deleteAllInBatch();
                    paymentRepository.deleteAllInBatch();
                    ticketRepository.deleteAllInBatch();
                    bookingRepository.deleteAllInBatch();
                    existingBookings = 0;
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }

        List<User> customers = userRepository.findAll().stream()
                .filter(user -> user.getRole() != null && "Customer".equalsIgnoreCase(user.getRole().getName()))
                .toList();
        List<Showtime> showtimes = showtimeRepository.findAll();
        List<FoodItem> foodItems = foodItemRepository.findAll();

        if (customers.isEmpty() || showtimes.isEmpty()) {
            return;
        }

        java.util.Map<Integer, List<Seat>> seatsCache = new java.util.HashMap<>();
        List<Showtime> bookableShowtimes = showtimes.stream()
                .filter(showtime -> showtime.getHall() != null)
                .filter(showtime -> !seatsCache.computeIfAbsent(showtime.getHall().getHallId(), hallId ->
                        seatRepository.findByHall_HallIdOrderBySeatRowAscSeatNumberAsc(hallId)
                ).isEmpty())
                .toList();
        if (bookableShowtimes.isEmpty()) {
            System.out.println("=== DataInitializer: Khong co suat chieu nao co ghe, bo qua seed booking ===");
            return;
        }

        String[] bookingStatuses = {
                "Confirmed", "Completed", "Pending", "Pending", "Cancelled", "Confirmed",
                "Pending", "Completed", "Confirmed", "Pending", "Cancelled", "Completed"
        };
        String[] paymentStatuses = {
                "Completed", "Completed", "Pending", "Pending", "Failed", "Completed",
                "Pending", "Completed", "Completed", "Pending", "Failed", "Completed"
        };
        String[] paymentMethods = {"VNPay", "VietQR", "VNPay", "VietQR", "VNPay", "Cash"};

        int created = 0;
        int targetToCreate = Math.max(0, (int) (REPORT_BOOKING_SEED_TARGET - existingBookings));
        while (created < targetToCreate) {
            Showtime showtime = bookableShowtimes.get(created % bookableShowtimes.size());
            List<Seat> seats = seatsCache.get(showtime.getHall().getHallId());

            String bookingStatus = bookingStatuses[created % bookingStatuses.length];
            String paymentStatus = paymentStatuses[created % paymentStatuses.length];
            User customer = customers.get(created % customers.size());
            int seatOffset = (created * 3) % Math.max(seats.size(), 1);
            int ticketCount = 1 + random.nextInt(3);
            List<Seat> bookingSeats = new ArrayList<>();
            for (int t = 0; t < ticketCount && t < seats.size(); t++) {
                bookingSeats.add(seats.get((seatOffset + t) % seats.size()));
            }

            int foodTotal = seedFoodTotal(foodItems, created);
            LocalDateTime createdAt = seedReportTimestamp(existingBookings + created);
            Booking booking = createSeedBooking(customer, showtime, bookingSeats, foodTotal, bookingStatus, createdAt);
            createSeedFoodOrder(booking, foodItems, created, foodTotal, createdAt);
            createSeedPayment(booking, paymentMethods[created % paymentMethods.length], paymentStatus, created, createdAt);
            created++;
        }

        ensureShowtimeCoverageBookings(customers, bookableShowtimes, foodItems);
        ensureFoodSalesData(foodItems);
    }

    private void ensureFoodSalesData(List<FoodItem> foodItems) {
        if (foodItems.isEmpty()) {
            return;
        }

        LocalDateTime rangeEnd = LocalDateTime.now();
        LocalDateTime rangeStart = rangeEnd.toLocalDate()
                .minusDays(REPORT_FOOD_ORDER_SEED_DAYS)
                .atStartOfDay();
        List<FoodOrder> existingOrders = foodOrderRepository.findAll();
        long existingConfirmedOrders = existingOrders.stream()
                .filter(order -> "CONFIRMED".equalsIgnoreCase(order.getStatus()))
                .filter(order -> order.getCreatedAt() != null)
                .filter(order -> !order.getCreatedAt().isBefore(rangeStart))
                .filter(order -> !order.getCreatedAt().isAfter(rangeEnd))
                .count();
        int missingOrders = Math.max(0,
                REPORT_FOOD_ORDER_SEED_TARGET - (int) existingConfirmedOrders);
        if (missingOrders == 0) {
            return;
        }

        Set<Integer> bookingsWithFoodOrders = new HashSet<>();
        existingOrders.stream()
                .filter(order -> order.getBooking() != null)
                .map(order -> order.getBooking().getBookingId())
                .forEach(bookingsWithFoodOrders::add);

        List<Booking> candidates = bookingRepository.findAll().stream()
                .filter(booking -> booking.getBookingId() != null)
                .filter(booking -> !bookingsWithFoodOrders.contains(booking.getBookingId()))
                .filter(booking -> booking.getCreatedAt() != null)
                .filter(booking -> !booking.getCreatedAt().isBefore(rangeStart))
                .filter(booking -> !booking.getCreatedAt().isAfter(rangeEnd))
                .filter(booking -> "Confirmed".equalsIgnoreCase(booking.getStatus())
                        || "Completed".equalsIgnoreCase(booking.getStatus()))
                .sorted((left, right) -> right.getCreatedAt().compareTo(left.getCreatedAt()))
                .toList();

        int ordersToCreate = Math.min(missingOrders, candidates.size());
        for (int i = 0; i < ordersToCreate; i++) {
            int seedIndex = (int) existingConfirmedOrders + i;
            FoodItem item = foodItems.get(seedIndex % foodItems.size());
            int quantity = 1 + (seedIndex % 3);
            createSeedFoodOrder(
                    candidates.get(i),
                    foodItems,
                    seedIndex,
                    item.getPrice() * quantity,
                    candidates.get(i).getCreatedAt());
        }
    }

    private void ensureShowtimeCoverageBookings(List<User> customers,
                                                List<Showtime> showtimes,
                                                List<FoodItem> foodItems) {
        if (customers.isEmpty() || showtimes.isEmpty()) {
            return;
        }

        LocalDate today = LocalDate.now();
        LocalDateTime reportStart = today.atStartOfDay();
        LocalDateTime reportEnd = today.plusDays(REPORT_COVERAGE_SEED_DAYS).plusDays(1).atStartOfDay();
        List<Showtime> reportShowtimes = showtimes.stream()
                .filter(showtime -> showtime.getStartTime() != null)
                .filter(showtime -> !showtime.getStartTime().isBefore(reportStart))
                .filter(showtime -> showtime.getStartTime().isBefore(reportEnd))
                .sorted((left, right) -> left.getStartTime().compareTo(right.getStartTime()))
                .toList();

        int seedIndex = 0;
        java.util.Map<String, Integer> lowCoverageSlots = new java.util.HashMap<>();
        java.util.Map<Integer, List<Seat>> seatsCache = new java.util.HashMap<>();
        for (Showtime showtime : reportShowtimes) {
            List<Seat> seats = seatsCache.computeIfAbsent(showtime.getHall().getHallId(), hallId ->
                    seatRepository.findByHall_HallIdOrderBySeatRowAscSeatNumberAsc(hallId)
            );
            if (seats.isEmpty()) {
                continue;
            }

            int capacity = showtime.getHall() != null && showtime.getHall().getCapacity() != null
                    ? showtime.getHall().getCapacity()
                    : seats.size();
            String lowCoverageKey = showtime.getHall().getBranch().getBranchId() + "|"
                    + showtime.getStartTime().toLocalDate();
            int lowCoverageCount = lowCoverageSlots.getOrDefault(lowCoverageKey, 0);
            boolean lowCoverageSample = lowCoverageCount < 2;
            if (lowCoverageSample) {
                lowCoverageSlots.put(lowCoverageKey, lowCoverageCount + 1);
            }

            int targetPaidTickets = targetCoverageTicketCount(showtime, capacity, lowCoverageSample);
            long currentPaidTickets = countCompletedTicketsForShowtime(showtime.getShowtimeId());
            int missingTickets = targetPaidTickets - (int) currentPaidTickets;
            if (missingTickets <= 0) {
                seedIndex++;
                continue;
            }

            int seatOffset = Math.floorMod(showtime.getShowtimeId() * 7, seats.size());
            while (missingTickets > 0) {
                int ticketCount = Math.min(REPORT_COVERAGE_MAX_BOOKING_SEATS, missingTickets);
                List<Seat> bookingSeats = new ArrayList<>();
                for (int t = 0; t < ticketCount; t++) {
                    bookingSeats.add(seats.get((seatOffset + targetPaidTickets - missingTickets + t) % seats.size()));
                }

                User customer = customers.get(seedIndex % customers.size());
                int foodTotal = seedFoodTotal(foodItems, seedIndex);
                LocalDateTime createdAt = showtime.getStartTime().minusDays(1).withHour(18).withMinute(seedIndex % 45);
                Booking booking = createSeedBooking(customer, showtime, bookingSeats, foodTotal, "Completed", createdAt);
                createSeedFoodOrder(booking, foodItems, seedIndex, foodTotal, createdAt);
                createSeedPayment(booking, seedIndex % 2 == 0 ? "VNPay" : "VietQR", "Completed", seedIndex, createdAt);

                missingTickets -= ticketCount;
                seedIndex++;
            }
        }
    }

    private int targetCoverageTicketCount(Showtime showtime, int capacity, boolean lowCoverageSample) {
        if (capacity <= 0 || showtime == null || showtime.getHall() == null) {
            return 0;
        }
        double coverage = lowCoverageSample
                ? (Math.floorMod(showtime.getShowtimeId(), 2) == 0 ? 0.22 : 0.38)
                : 0.80 + (Math.floorMod(showtime.getShowtimeId(), 6) * 0.02);
        return Math.min(capacity, Math.max(1, (int) Math.round(capacity * coverage)));
    }

    private long countCompletedTicketsForShowtime(Integer showtimeId) {
        if (showtimeId == null) {
            return 0L;
        }
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(t.ticket_id)
                FROM ticket t
                JOIN booking b ON b.booking_id = t.booking_id
                JOIN payment p ON p.booking_id = b.booking_id
                WHERE b.showtime_id = ?
                  AND b.status <> 'Cancelled'
                  AND p.status = 'Completed'
                """, Integer.class, showtimeId);
        return count != null ? count : 0L;
    }

    private Booking createSeedBooking(User customer,
                                      Showtime showtime,
                                      List<Seat> seats,
                                      int foodTotal,
                                      String status,
                                      LocalDateTime createdAt) {
        long seatTotal = seats.stream()
                .mapToLong(seat -> priceForSeedSeat(showtime, seat))
                .sum();
        long finalTotal = seatTotal + foodTotal;

        Booking booking = new Booking();
        booking.setUser(customer);
        booking.setShowtime(showtime);
        booking.setSeatSubtotal(seatTotal);
        booking.setFoodSubtotal((long) foodTotal);
        booking.setOrderSubtotal(finalTotal);
        booking.setTotalPrice(finalTotal);
        booking.setStatus(status);
        booking = bookingRepository.save(booking);
        booking.setCreatedAt(createdAt);

        for (int i = 0; i < seats.size(); i++) {
            Ticket ticket = new Ticket();
            ticket.setBooking(booking);
            ticket.setSeat(seats.get(i));
            ticket.setQrCode("SEED-" + booking.getBookingId() + "-" + (i + 1));
            ticket.setStatus("Cancelled".equals(status) ? "Cancelled" : "Active");
            ticketRepository.save(ticket);
        }

        return booking;
    }

    private long priceForSeedSeat(Showtime showtime, Seat seat) {
        if (showtime == null || seat == null) {
            return 0L;
        }
        int base = showtime.getPrice() != null && showtime.getPrice() > 0
                ? showtime.getPrice()
                : ticketPriceFor(showtime.getHall());
        return switch (seat.getType() != null ? seat.getType().toUpperCase() : "STANDARD") {
            case "VIP" -> base + 20_000L;
            case "COUPLE" -> base * 2L;
            case "DISABLED" -> 0L;
            default -> base;
        };
    }

    private static int defaultTicketPriceForHall(String hallName) {
        if (hallName != null && hallName.contains("IMAX")) {
            return 120000;
        }
        if (hallName != null && hallName.contains("3D")) {
            return 100000;
        }
        return 80000;
    }

    private static int ticketPriceFor(Hall hall) {
        return hall != null && hall.getTicketPrice() != null && hall.getTicketPrice() > 0
                ? hall.getTicketPrice()
                : 80000;
    }

    private void createSeedPayment(Booking booking, String method, String status, int index, LocalDateTime createdAt) {
        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setAmount(booking.getTotalPrice());
        payment.setMethod(method);
        payment.setStatus(status);

        if ("Pending".equals(status)) {
            boolean expiredSample = index % 4 == 3;
            payment.setExpiresAt(LocalDateTime.now().plusMinutes(expiredSample ? -5 : 15));
        }

        payment = paymentRepository.save(payment);
        payment.setCreatedAt(createdAt);
    }

    private void createSeedFoodOrder(Booking booking,
                                     List<FoodItem> foodItems,
                                     int index,
                                     int foodTotal,
                                     LocalDateTime createdAt) {
        if (foodItems.isEmpty() || foodTotal <= 0) {
            return;
        }

        FoodItem item = foodItems.get(index % foodItems.size());
        FoodOrder order = new FoodOrder();
        order.setBooking(booking);
        order.setStatus(switch (booking.getStatus()) {
            case "Confirmed", "Completed" -> "CONFIRMED";
            case "Cancelled" -> "CANCELLED";
            default -> "PENDING";
        });
        order.setTotalAmount(foodTotal);
        order.setCreatedAt(createdAt);
        order = foodOrderRepository.save(order);

        FoodOrderItem orderItem = new FoodOrderItem();
        orderItem.setOrderId(order.getOrderId());
        orderItem.setItemId(item.getItemId());
        orderItem.setFoodOrder(order);
        orderItem.setFoodItem(item);
        orderItem.setQuantity(Math.max(1, foodTotal / item.getPrice()));
        orderItem.setUnitPrice(item.getPrice());
        foodOrderItemRepository.save(orderItem);
    }

    private int seedFoodTotal(List<FoodItem> foodItems, int index) {
        if (foodItems.isEmpty() || index % 4 == 1) {
            return 0;
        }
        FoodItem item = foodItems.get(index % foodItems.size());
        int quantity = 1 + (index % 3);
        return item.getPrice() * quantity;
    }

    private LocalDateTime seedReportTimestamp(long index) {
        LocalDate date = LocalDate.now().minusDays(index % 180);
        int hour = switch ((int) (index % 4)) {
            case 0 -> 10;
            case 1 -> 14;
            case 2 -> 18;
            default -> 21;
        };
        int minute = (int) ((index * 7) % 45);
        return LocalDateTime.of(date, LocalTime.of(hour, minute));
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

    private void seedInitialNotifications(User admin) {
        List<Notification> existing = notificationRepository.findAll();
        for (Notification notification : existing) {
            if ("System Maintenance Scheduled".equalsIgnoreCase(notification.getTitle())) {
                notification.setTitle("Lich bao tri he thong");
                notification.setMessage("He thong se bao tri tu 02:00 den 04:00. Dich vu co the tam thoi gian doan.");
                notificationRepository.save(notification);
            } else if ("New Feature Available".equalsIgnoreCase(notification.getTitle())) {
                notification.setTitle("Tinh nang moi kha dung");
                notification.setMessage("Tinh nang thong ke moi da san sang de su dung.");
                notificationRepository.save(notification);
            } else if ("Security Alert".equalsIgnoreCase(notification.getTitle())) {
                notification.setTitle("Canh bao bao mat");
                notification.setMessage("Phat hien dang nhap tu thiet bi moi. Vui long kiem tra lai tai khoan.");
                notificationRepository.save(notification);
            } else if ("Account Verification Required".equalsIgnoreCase(notification.getTitle())) {
                notification.setTitle("Yeu cau xac minh tai khoan");
                notification.setMessage("Vui long xac minh email de tiep tuc su dung day du tinh nang.");
                notificationRepository.save(notification);
            }
        }

        if (notificationRepository.count() > 0) return;

        Notification maintenance = new Notification();
        maintenance.setUser(admin);
        maintenance.setTitle("Lich bao tri he thong");
        maintenance.setMessage("He thong se bao tri tu 02:00 den 04:00. Dich vu co the tam thoi gian doan.");
        maintenance.setType("System");
        maintenance.setRead(true);
        maintenance.setCreatedAt(LocalDateTime.now().minusHours(9));
        notificationRepository.save(maintenance);

        Notification feature = new Notification();
        feature.setUser(admin);
        feature.setTitle("Tinh nang moi kha dung");
        feature.setMessage("Tinh nang thong ke moi da san sang de su dung.");
        feature.setType("Promotion");
        feature.setRead(false);
        feature.setCreatedAt(LocalDateTime.now().minusDays(1));
        notificationRepository.save(feature);

        Notification security = new Notification();
        security.setUser(admin);
        security.setTitle("Canh bao bao mat");
        security.setMessage("Phat hien dang nhap tu thiet bi moi. Vui long kiem tra lai tai khoan.");
        security.setType("Alert");
        security.setRead(true);
        security.setCreatedAt(LocalDateTime.now().minusDays(1));
        notificationRepository.save(security);

        Notification verification = new Notification();
        verification.setUser(admin);
        verification.setTitle("Yeu cau xac minh tai khoan");
        verification.setMessage("Vui long xac minh email de tiep tuc su dung day du tinh nang.");
        verification.setType("System");
        verification.setRead(true);
        verification.setCreatedAt(LocalDateTime.now().minusDays(5));
        notificationRepository.save(verification);
    }

    private void seedReviews(List<Movie> movies, List<User> users) {
        if (reviewRepository.count() > 0) return;

        List<Review> reviews = new ArrayList<>();
        List<User> customers = users.stream()
                .filter(u -> u.getRole() != null && "Customer".equalsIgnoreCase(u.getRole().getName()))
                .toList();

        if (customers.isEmpty() || movies.isEmpty()) return;

        for (Movie movie : movies) {
            String title = movie.getTitle();
            if ("Avengers: Doomsday".equalsIgnoreCase(title)) {
                reviews.add(createReview(customers.get(0 % customers.size()), movie, 5, "Phim rat dang xem, ky xao an tuong.", 1));
                reviews.add(createReview(customers.get(1 % customers.size()), movie, 4, "Cot truyen loi cuon va ket thuc bat ngo.", 2));
            } else if ("Lat Mat 8".equalsIgnoreCase(title) || "Lật Mặt 8".equalsIgnoreCase(title)) {
                reviews.add(createReview(customers.get(2 % customers.size()), movie, 5, "Phim gia dinh cam dong va giai tri.", 1));
                reviews.add(createReview(customers.get(3 % customers.size()), movie, 4, "Dien xuat tu nhien, rat phu hop xem cung gia dinh.", 4));
            } else if ("Godzilla x Kong".equalsIgnoreCase(title)
                    || "Godzilla x Kong: The New Empire".equalsIgnoreCase(title)) {
                reviews.add(createReview(customers.get(4 % customers.size()), movie, 4, "Hanh dong da mat, am thanh tot.", 2));
                reviews.add(createReview(customers.get(5 % customers.size()), movie, 5, "Trai nghiem rap rat cuon.", 6));
            } else if ("Dune: Part Two".equalsIgnoreCase(title)) {
                reviews.add(createReview(customers.get(6 % customers.size()), movie, 5, "Hinh anh dep va am nhac xuat sac.", 3));
                reviews.add(createReview(customers.get(7 % customers.size()), movie, 4, "Phim dai nhung van giu duoc mach cam xuc.", 7));
            }
        }

        if (!reviews.isEmpty()) {
            reviewRepository.saveAll(reviews);
            System.out.println("=== DataInitializer: Seeded " + reviews.size() + " reviews ===");
        }
    }

    private Review createReview(User user, Movie movie, Integer rating, String comment, int daysAgo) {
        Review review = new Review();
        review.setUser(user);
        review.setMovie(movie);
        review.setRating(rating);
        review.setComment(comment);
        review.setCreatedAt(LocalDateTime.now().minusDays(daysAgo).minusHours(random.nextInt(12)));
        return review;
    }
}
