package com.cinema.hyperCinema.config;

import com.cinema.hyperCinema.model.*;
import com.cinema.hyperCinema.repository.*;
import org.springframework.boot.CommandLineRunner;
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
    private final LanguageRepository languageRepository;
    private final GenreRepository genreRepository;
    private final BranchRepository branchRepository;
    private final HallRepository hallRepository;
    private final SeatRepository seatRepository;
    private final MovieRepository movieRepository;
    private final MovieGenreRepository movieGenreRepository;
    private final BranchMovieRepository branchMovieRepository;
    private final ShowtimeRepository showtimeRepository;
    private final BookingRepository bookingRepository;
    private final TicketRepository ticketRepository;
    private final PaymentRepository paymentRepository;
    private final FoodItemRepository foodItemRepository;
    private final AuditLogRepository auditLogRepository;
    private final PasswordEncoder passwordEncoder;

    private final Random random = new Random(42);

    public DataInitializer(
            RoleRepository roleRepository,
            UserRepository userRepository,
            LanguageRepository languageRepository,
            GenreRepository genreRepository,
            BranchRepository branchRepository,
            HallRepository hallRepository,
            SeatRepository seatRepository,
            MovieRepository movieRepository,
            MovieGenreRepository movieGenreRepository,
            BranchMovieRepository branchMovieRepository,
            ShowtimeRepository showtimeRepository,
            BookingRepository bookingRepository,
            TicketRepository ticketRepository,
            PaymentRepository paymentRepository,
            FoodItemRepository foodItemRepository,
            AuditLogRepository auditLogRepository,
            PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.languageRepository = languageRepository;
        this.genreRepository = genreRepository;
        this.branchRepository = branchRepository;
        this.hallRepository = hallRepository;
        this.seatRepository = seatRepository;
        this.movieRepository = movieRepository;
        this.movieGenreRepository = movieGenreRepository;
        this.branchMovieRepository = branchMovieRepository;
        this.showtimeRepository = showtimeRepository;
        this.bookingRepository = bookingRepository;
        this.ticketRepository = ticketRepository;
        this.paymentRepository = paymentRepository;
        this.foodItemRepository = foodItemRepository;
        this.auditLogRepository = auditLogRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (roleRepository.count() > 0) {
            return; // Đã có data, bỏ qua
        }

        System.out.println("=== DataInitializer: Bắt đầu seed dữ liệu mẫu ===");

        List<Role> roles = seedRoles();
        List<Language> languages = seedLanguages();
        List<Genre> genres = seedGenres();
        List<User> users = seedUsers(roles);
        List<Branch> branches = seedBranches();
        assignBranchManagers(users, branches);
        List<Hall> halls = seedHalls(branches);
        List<Seat> allSeats = seedSeats(halls);
        List<Movie> movies = seedMovies(languages);
        seedMovieGenres(movies, genres);
        seedBranchMovies(movies, branches);
        List<Showtime> showtimes = seedShowtimes(movies, halls);
        seedFoodItems();
        seedBookingsAndPayments(users, showtimes, allSeats);
        seedAuditLogs(users);

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

    // ───────────────────── LANGUAGES ─────────────────────
    private List<Language> seedLanguages() {
        List<Language> langs = new ArrayList<>();
        for (String name : new String[]{"Tiếng Việt", "English", "Korean"}) {
            Language l = new Language();
            l.setName(name);
            langs.add(languageRepository.save(l));
        }
        return langs;
    }

    // ───────────────────── GENRES ─────────────────────
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
                h.setHallType(hallTypeFor(name));
                h.setCapacity(50);
                h.setStatus("Active");
                halls.add(hallRepository.save(h));
            }
        }
        return halls;
    }

    private String hallTypeFor(String hallName) {
        if (hallName != null && hallName.toUpperCase().contains("IMAX")) {
            return "IMAX";
        }
        if (hallName != null && hallName.toUpperCase().contains("3D")) {
            return "3D";
        }
        return "2D";
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

    // ───────────────────── MOVIES ─────────────────────
    private List<Movie> seedMovies(List<Language> languages) {
        List<Movie> movies = new ArrayList<>();
        Language vietnamese = languages.get(0);
        Language english = languages.get(1);
        Language korean = languages.get(2);

        movies.add(createMovie("Lật Mặt 8: Vòng Tay Nắng", 135, "Phim Việt Nam đạo diễn Lý Hải", LocalDate.now().minusDays(10), "NowShowing", vietnamese));
        movies.add(createMovie("Avengers: Doomsday", 150, "Siêu anh hùng Marvel đối đầu Doom", LocalDate.now().minusDays(5), "NowShowing", english));
        movies.add(createMovie("Godzilla x Kong: Đế Chế Mới", 115, "Quái vật đại chiến", LocalDate.now().minusDays(3), "NowShowing", english));
        movies.add(createMovie("Mai", 128, "Phim tâm lý xã hội Trấn Thành", LocalDate.now().minusDays(20), "NowShowing", vietnamese));
        movies.add(createMovie("Parasite 2", 130, "Phần tiếp theo siêu phẩm Hàn Quốc", LocalDate.now().minusDays(1), "NowShowing", korean));
        movies.add(createMovie("Inside Out 3", 100, "Phim hoạt hình Pixar về cảm xúc", LocalDate.now().plusDays(7), "ComingSoon", english));
        movies.add(createMovie("Đào, Phở và Piano 2", 120, "Phim lịch sử Việt Nam", LocalDate.now().plusDays(14), "ComingSoon", vietnamese));
        movies.add(createMovie("The Batman 2", 155, "Người Dơi trở lại Gotham", LocalDate.now().plusDays(21), "ComingSoon", english));
        movies.add(createMovie("Nhà Bà Nữ 2", 118, "Phim hài Trấn Thành", LocalDate.now().minusDays(15), "NowShowing", vietnamese));
        movies.add(createMovie("Dune: Part Three", 165, "Sử thi khoa học viễn tưởng", LocalDate.now().plusDays(30), "ComingSoon", english));

        return movies;
    }

    private Movie createMovie(String title, int duration, String desc, LocalDate releaseDate, String status, Language lang) {
        Movie m = new Movie();
        m.setTitle(title);
        m.setDuration(duration);
        m.setDescription(desc);
        m.setReleaseDate(releaseDate);
        m.setStatus(status);
        m.setLanguageId(lang.getLanguageId());
        return movieRepository.save(m);
    }

    // ───────────────────── MOVIE-GENRE ─────────────────────
    private void seedMovieGenres(List<Movie> movies, List<Genre> genres) {
        int[][] assignments = {
                {2, 6},         // Lật Mặt 8: Hài, Tâm lý
                {0, 4, 7},      // Avengers: Hành động, Sci-fi, Phiêu lưu
                {0, 4},         // Godzilla: Hành động, Sci-fi
                {3, 6},         // Mai: Tình cảm, Tâm lý
                {1, 6},         // Parasite 2: Kinh dị, Tâm lý
                {5, 2},         // Inside Out 3: Hoạt hình, Hài
                {6, 7},         // Đào Phở Piano 2: Tâm lý, Phiêu lưu
                {0, 6},         // The Batman 2: Hành động, Tâm lý
                {2, 3},         // Nhà Bà Nữ 2: Hài, Tình cảm
                {4, 7},         // Dune 3: Sci-fi, Phiêu lưu
        };
        for (int i = 0; i < movies.size(); i++) {
            for (int genreIdx : assignments[i]) {
                MovieGenre mg = new MovieGenre();
                mg.setId(new MovieGenreId(movies.get(i).getMovieId(), genres.get(genreIdx).getGenreId()));
                mg.setMovie(movies.get(i));
                mg.setGenre(genres.get(genreIdx));
                movieGenreRepository.save(mg);
            }
        }
    }

    // ───────────────────── BRANCH-MOVIE ─────────────────────
    private void seedBranchMovies(List<Movie> movies, List<Branch> branches) {
        for (Movie movie : movies) {
            if ("NowShowing".equals(movie.getStatus())) {
                for (Branch branch : branches) {
                    BranchMovie bm = new BranchMovie();
                    bm.setId(new BranchMovieId(branch.getBranchId(), movie.getMovieId()));
                    bm.setBranch(branch);
                    bm.setMovie(movie);
                    branchMovieRepository.save(bm);
                }
            }
        }
    }

    // ───────────────────── SHOWTIMES ─────────────────────
    private List<Showtime> seedShowtimes(List<Movie> movies, List<Hall> halls) {
        List<Showtime> showtimes = new ArrayList<>();
        List<Movie> nowShowing = movies.stream()
                .filter(m -> "NowShowing".equals(m.getStatus()))
                .toList();

        for (int dayOffset = -3; dayOffset <= 3; dayOffset++) {
            LocalDate date = LocalDate.now().plusDays(dayOffset);
            for (Hall hall : halls) {
                int[] startHours = {10, 14, 19};
                for (int h = 0; h < startHours.length; h++) {
                    Movie movie = nowShowing.get(random.nextInt(nowShowing.size()));
                    LocalDateTime startTime = date.atTime(startHours[h], 0);
                    LocalDateTime endTime = startTime.plusMinutes(movie.getDuration() + 15);

                    Showtime st = new Showtime();
                    st.setMovie(movie);
                    st.setHall(hall);
                    st.setStartTime(startTime);
                    st.setEndTime(endTime);
                    st.setPrice(randomPrice());
                    showtimes.add(showtimeRepository.save(st));
                }
            }
        }
        return showtimes;
    }

    private int randomPrice() {
        int[] prices = {75000, 85000, 95000, 110000, 120000, 150000};
        return prices[random.nextInt(prices.length)];
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
            fi.setCategory((String) data[2]);
            fi.setStatus("Active");
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
            long totalPrice = (long) st.getPrice() * ticketCount;

            Booking booking = new Booking();
            booking.setUser(customer);
            booking.setShowtime(st);
            booking.setTotalPrice(totalPrice);
            booking.setStatus(bookingStatus);
            booking = bookingRepository.save(booking);

            for (int t = 0; t < ticketCount && seatIdx < allSeats.size(); t++) {
                Ticket ticket = new Ticket();
                ticket.setBooking(booking);
                ticket.setSeat(allSeats.get(seatIdx % allSeats.size()));
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
