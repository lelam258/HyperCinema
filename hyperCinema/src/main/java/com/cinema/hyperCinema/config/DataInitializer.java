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
    private final GenreRepository genreRepository;
    private final BranchRepository branchRepository;
    private final HallRepository hallRepository;
    private final SeatRepository seatRepository;
    private final MovieRepository movieRepository;
    private final ShowtimeRepository showtimeRepository;
    private final BookingRepository bookingRepository;
    private final TicketRepository ticketRepository;
    private final PaymentRepository paymentRepository;
    private final FoodItemRepository foodItemRepository;
    private final AuditLogRepository auditLogRepository;
    private final LanguageRepository languageRepository;
    private final FoodOrderRepository foodOrderRepository;
    private final FoodOrderItemRepository foodOrderItemRepository;
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
            ShowtimeRepository showtimeRepository,
            BookingRepository bookingRepository,
            TicketRepository ticketRepository,
            PaymentRepository paymentRepository,
            FoodItemRepository foodItemRepository,
            AuditLogRepository auditLogRepository,
            LanguageRepository languageRepository,
            FoodOrderRepository foodOrderRepository,
            FoodOrderItemRepository foodOrderItemRepository,
            PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.genreRepository = genreRepository;
        this.branchRepository = branchRepository;
        this.hallRepository = hallRepository;
        this.seatRepository = seatRepository;
        this.movieRepository = movieRepository;
        this.showtimeRepository = showtimeRepository;
        this.bookingRepository = bookingRepository;
        this.ticketRepository = ticketRepository;
        this.paymentRepository = paymentRepository;
        this.foodItemRepository = foodItemRepository;
        this.auditLogRepository = auditLogRepository;
        this.languageRepository = languageRepository;
        this.foodOrderRepository = foodOrderRepository;
        this.foodOrderItemRepository = foodOrderItemRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.findByUsername("admin").isPresent()) {
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
            return;
        }

        System.out.println("=== DataInitializer: Bắt đầu seed dữ liệu mẫu ===");

        List<Role> roles = seedRoles();
        List<Genre> genres = seedGenres();
        List<User> users = seedUsers(roles);
        List<Branch> branches = seedBranches();
        assignBranchManagers(users, branches);
        List<Hall> halls = seedHalls(branches);
        List<Seat> allSeats = seedSeats(halls);
        seedFoodItems();
        seedAuditLogs(users);

        List<Language> languages = seedLanguages();
        List<Movie> movies = seedMovies(genres, languages);
        List<Showtime> showtimes = seedShowtimes(movies, halls);
        seedBookingsPaymentsAndFood(users, showtimes, allSeats);

        System.out.println("=== DataInitializer: Hoàn tất seed dữ liệu mẫu ===");
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
                h.setCapacity(50);
                String hallType = "2D";
                if (name.contains("3D")) {
                    hallType = "3D";
                } else if (name.contains("IMAX")) {
                    hallType = "IMAX";
                }
                h.setHallType(hallType);
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
            fi.setCategory((String) data[2]);
            fi.setStatus("Active");
            foodItemRepository.save(fi);
        }
    }

    // ───────────────────── LANGUAGES & MOVIES ─────────────────────
    private List<Language> seedLanguages() {
        List<Language> languages;
        if (languageRepository.count() == 0) {
            languages = new ArrayList<>();
            for (String name : new String[]{"English", "Vietnamese"}) {
                Language l = new Language();
                l.setName(name);
                languages.add(languageRepository.save(l));
            }
        } else {
            languages = languageRepository.findAll();
        }
        return languages;
    }

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
                            orderItem.setFood(item);
                            orderItem.setQuantity(qty);
                            orderItem.setPrice(item.getPrice());
                            orderItems.add(orderItem);

                            foodTotal += (long) item.getPrice() * qty;
                        }

                        foodOrder.setTotalPrice(foodTotal);
                        foodOrder.setItems(orderItems);
                        foodOrderRepository.save(foodOrder);

                        for (FoodOrderItem item : orderItems) {
                            foodOrderItemRepository.save(item);
                        }
                    }
                }

                bookingCount++;
            }
        }
        System.out.println("=== Seeded " + bookingCount + " bookings. ===");
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
