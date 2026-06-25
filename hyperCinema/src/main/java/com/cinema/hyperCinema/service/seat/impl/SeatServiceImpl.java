package com.cinema.hyperCinema.service.seat.impl;

import com.cinema.hyperCinema.dto.admin.seat.request.SeatGenerateRequest;
import com.cinema.hyperCinema.dto.admin.seat.request.SeatUpdateRequest;
import com.cinema.hyperCinema.dto.admin.seat.response.SeatDetailView;
import com.cinema.hyperCinema.dto.admin.seat.response.ShowtimeSeatView;
import com.cinema.hyperCinema.exception.hall.HallNotFoundException;
import com.cinema.hyperCinema.exception.hall.HallValidationException;
import com.cinema.hyperCinema.exception.seat.SeatNotFoundException;
import com.cinema.hyperCinema.exception.seat.SeatValidationException;
import com.cinema.hyperCinema.model.*;
import com.cinema.hyperCinema.repository.*;
import com.cinema.hyperCinema.service.seat.SeatService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class SeatServiceImpl implements SeatService {

    private final SeatRepository seatRepository;
    private final HallRepository hallRepository;
    private final ShowtimeRepository showtimeRepository;
    private final TicketRepository ticketRepository;
    private final SeatReservationRepository seatReservationRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<SeatDetailView> getSeatsByHall(Integer hallId, User actor) {
        User current = loadActor(actor);
        Hall hall = hallRepository.findById(hallId)
                .orElseThrow(() -> new HallNotFoundException(hallId));
        assertCanManageBranch(current, hallBranchId(hall));

        return seatRepository.findByHall_HallIdOrderBySeatRowAscSeatNumberAsc(hallId).stream()
                .map(this::toDetailView)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShowtimeSeatView> getSeatsForShowtime(Integer showtimeId) {
        Showtime showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy suất chiếu ID: " + showtimeId));

        Hall hall = showtime.getHall();
        List<Seat> seats = seatRepository.findByHall_HallIdOrderBySeatRowAscSeatNumberAsc(hall.getHallId());

        // Tìm các ghế đã đặt
        List<Ticket> activeTickets = ticketRepository.findByBooking_Showtime_ShowtimeIdAndBooking_StatusNot(showtimeId, "Cancelled");
        Set<Integer> bookedSeatIds = activeTickets.stream()
                .map(ticket -> ticket.getSeat().getSeatId())
                .collect(Collectors.toSet());

        // Tìm các ghế đang được giữ tạm thời
        List<SeatReservation> activeReservations = seatReservationRepository.findByShowtime_ShowtimeIdAndExpiredAtAfter(showtimeId, LocalDateTime.now());
        Set<Integer> reservedSeatIds = activeReservations.stream()
                .map(res -> res.getSeat().getSeatId())
                .collect(Collectors.toSet());

        Integer basePrice = showtime.getPrice();

        return seats.stream().map(seat -> {
            String status = "AVAILABLE";
            if (bookedSeatIds.contains(seat.getSeatId())) {
                status = "BOOKED";
            } else if (reservedSeatIds.contains(seat.getSeatId())) {
                status = "RESERVED";
            }

            Integer finalPrice = basePrice;
            if ("VIP".equalsIgnoreCase(seat.getType())) {
                finalPrice += 20000; // Phụ thu ghế VIP
            } else if ("Double".equalsIgnoreCase(seat.getType())) {
                finalPrice = basePrice * 2; // Ghế đôi tính giá gấp đôi
            }

            return ShowtimeSeatView.builder()
                    .seatId(seat.getSeatId())
                    .seatRow(seat.getSeatRow())
                    .seatNumber(seat.getSeatNumber())
                    .type(seat.getType())
                    .status(status)
                    .finalPrice(finalPrice)
                    .build();
        }).toList();
    }

    @Override
    public void generateSeats(Integer hallId, SeatGenerateRequest request, User actor) {
        User current = loadActor(actor);
        Hall hall = hallRepository.findById(hallId)
                .orElseThrow(() -> new HallNotFoundException(hallId));
        assertCanManageBranch(current, hallBranchId(hall));

        // Kiểm tra xem đã có suất chiếu nào liên quan chưa (nếu có, không cho đổi sơ đồ ghế)
        if (showtimeRepository.existsByHall_HallId(hallId)) {
            throw new SeatValidationException("seat.cannot_generate_with_showtimes");
        }

        // Xóa các ghế cũ
        seatRepository.deleteByHall_HallId(hallId);
        seatRepository.flush(); // Force immediate execution of deletion in DB

        char startRow = request.getRowStart().charAt(0);
        char endRow = request.getRowEnd().charAt(0);

        if (startRow > endRow) {
            throw new SeatValidationException("seat.generate.invalid_row_range");
        }

        Set<String> vipRows = new HashSet<>(request.getVipRows() != null ? request.getVipRows() : List.of());
        Set<String> doubleRows = new HashSet<>(request.getDoubleRows() != null ? request.getDoubleRows() : List.of());

        int totalCapacity = 0;
        for (char r = startRow; r <= endRow; r++) {
            String rowStr = String.valueOf(r);
            if (doubleRows.contains(rowStr)) {
                int totalCustomers = request.getSeatsPerRow();
                int doubleSeatsCount = totalCustomers / 2;
                int col = 1;

                for (int i = 0; i < doubleSeatsCount; i++) {
                    Seat seat = new Seat();
                    seat.setHall(hall);
                    seat.setSeatRow(rowStr);
                    seat.setSeatNumber(col++);
                    seat.setType("Double");
                    seatRepository.save(seat);
                    totalCapacity += 2;
                }
            } else {
                String seatType = vipRows.contains(rowStr) ? "VIP" : "Standard";
                for (int col = 1; col <= request.getSeatsPerRow(); col++) {
                    Seat seat = new Seat();
                    seat.setHall(hall);
                    seat.setSeatRow(rowStr);
                    seat.setSeatNumber(col);
                    seat.setType(seatType);
                    seatRepository.save(seat);
                    totalCapacity += 1;
                }
            }
        }

        // Cập nhật sức chứa thực tế cho phòng chiếu
        hall.setCapacity(totalCapacity);
        hallRepository.save(hall);
    }

    @Override
    public void updateSeat(Integer seatId, SeatUpdateRequest request, User actor) {
        User current = loadActor(actor);
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new SeatNotFoundException(seatId));
        assertCanManageBranch(current, hallBranchId(seat.getHall()));

        if (showtimeRepository.existsByHall_HallId(seat.getHall().getHallId())) {
            throw new SeatValidationException("seat.cannot_modify_with_showtimes");
        }

        seat.setType(request.getType());
        seatRepository.save(seat);
    }

    @Override
    public void deleteSeat(Integer seatId, User actor) {
        User current = loadActor(actor);
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new SeatNotFoundException(seatId));
        assertCanManageBranch(current, hallBranchId(seat.getHall()));

        if (showtimeRepository.existsByHall_HallId(seat.getHall().getHallId())) {
            throw new SeatValidationException("seat.cannot_modify_with_showtimes");
        }

        Hall hall = seat.getHall();
        int capacityChange = "Double".equalsIgnoreCase(seat.getType()) ? 2 : 1;
        seatRepository.delete(seat);

        // Giảm sức chứa thực tế
        hall.setCapacity(Math.max(0, hall.getCapacity() - capacityChange));
        hallRepository.save(hall);
    }

    @Override
    public void addSingleSeat(Integer hallId, SeatUpdateRequest request, User actor) {
        User current = loadActor(actor);
        Hall hall = hallRepository.findById(hallId)
                .orElseThrow(() -> new HallNotFoundException(hallId));
        assertCanManageBranch(current, hallBranchId(hall));

        if (showtimeRepository.existsByHall_HallId(hallId)) {
            throw new SeatValidationException("seat.cannot_modify_with_showtimes");
        }

        String rowStr = request.getSeatRow().toUpperCase().trim();
        Integer num = request.getSeatNumber();

        if (seatRepository.existsByHall_HallIdAndSeatRowAndSeatNumber(hallId, rowStr, num)) {
            throw new SeatValidationException("seat.duplicate");
        }

        Seat seat = new Seat();
        seat.setHall(hall);
        seat.setSeatRow(rowStr);
        seat.setSeatNumber(num);
        seat.setType(request.getType());
        seatRepository.save(seat);

        // Tăng sức chứa thực tế
        int capacityChange = "Double".equalsIgnoreCase(request.getType()) ? 2 : 1;
        hall.setCapacity(hall.getCapacity() + capacityChange);
        hallRepository.save(hall);
    }

    @Override
    public void clearAllSeats(Integer hallId, User actor) {
        User current = loadActor(actor);
        Hall hall = hallRepository.findById(hallId)
                .orElseThrow(() -> new HallNotFoundException(hallId));
        assertCanManageBranch(current, hallBranchId(hall));

        if (showtimeRepository.existsByHall_HallId(hallId)) {
            throw new SeatValidationException("seat.cannot_modify_with_showtimes");
        }

        seatRepository.deleteByHall_HallId(hallId);
        hall.setCapacity(0);
        hallRepository.save(hall);
    }

    private User loadActor(User actor) {
        if (actor == null || actor.getUserId() == null) {
            throw new HallValidationException("hall.access.denied");
        }
        return userRepository.findById(actor.getUserId())
                .orElseThrow(() -> new HallValidationException("hall.access.denied"));
    }

    private void assertCanManageBranch(User actor, Integer branchId) {
        if (isAdmin(actor)) {
            return;
        }
        Integer scopedBranchId = forcedBranchId(actor);
        if (scopedBranchId == null || !scopedBranchId.equals(branchId)) {
            throw new HallValidationException("hall.branch.scope_denied");
        }
    }

    private Integer forcedBranchId(User actor) {
        if (isManager(actor) || isBranchManager(actor)) {
            Branch branch = actor.getBranch();
            return branch == null ? null : branch.getBranchId();
        }
        if (isAdmin(actor)) {
            return null;
        }
        throw new HallValidationException("hall.access.denied");
    }

    private static Integer hallBranchId(Hall hall) {
        Branch branch = hall.getBranch();
        return branch == null ? null : branch.getBranchId();
    }

    private SeatDetailView toDetailView(Seat seat) {
        return SeatDetailView.builder()
                .seatId(seat.getSeatId())
                .hallId(seat.getHall().getHallId())
                .seatRow(seat.getSeatRow())
                .seatNumber(seat.getSeatNumber())
                .type(seat.getType())
                .build();
    }

    private static boolean isAdmin(User user) {
        return isRole(user, "Admin") || isRole(user, "Administrator");
    }

    private static boolean isManager(User user) {
        return isRole(user, "Manager");
    }

    private static boolean isBranchManager(User user) {
        return isRole(user, "BranchManager") || isRole(user, "Branch Manager") || isRole(user, "Branch_Manager");
    }

    private static boolean isRole(User user, String expected) {
        Role role = user.getRole();
        return role != null && normalizeRoleName(expected).equals(normalizeRoleName(role.getName()));
    }

    private static String normalizeRoleName(String roleName) {
        if (roleName == null) {
            return "";
        }
        String normalized = roleName.trim();
        if (normalized.regionMatches(true, 0, "ROLE_", 0, 5)) {
            normalized = normalized.substring(5);
        }
        return normalized.replaceAll("[\\s_]+", "").toUpperCase();
    }
}
