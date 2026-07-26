package com.cinema.hyperCinema.service.seat.impl;

import com.cinema.hyperCinema.dto.admin.seat.request.SeatGenerateRequest;
import com.cinema.hyperCinema.dto.admin.seat.request.SeatUpdateRequest;
import com.cinema.hyperCinema.dto.admin.seat.response.SeatDetailView;
import com.cinema.hyperCinema.dto.admin.seat.response.SeatListItem;
import com.cinema.hyperCinema.dto.admin.seat.response.SeatMapView;
import com.cinema.hyperCinema.dto.admin.seat.response.ShowtimeSeatView;
import com.cinema.hyperCinema.exception.hall.HallNotFoundException;
import com.cinema.hyperCinema.exception.hall.HallValidationException;
import com.cinema.hyperCinema.exception.seat.SeatNotFoundException;
import com.cinema.hyperCinema.exception.seat.SeatValidationException;
import com.cinema.hyperCinema.model.*;
import com.cinema.hyperCinema.repository.*;
import com.cinema.hyperCinema.service.pricing.HallSeatTypePricingService;
import com.cinema.hyperCinema.service.pricing.TicketPriceBreakdown;
import com.cinema.hyperCinema.service.pricing.TicketPricingService;
import com.cinema.hyperCinema.service.seat.SeatService;
import com.cinema.hyperCinema.util.SeatPricing;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    private final HallSeatTypePricingService hallSeatTypePricingService;
    private final TicketPricingService ticketPricingService;

    private static final String SHOWTIME_CANCELLED = "CANCELLED";

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
    public SeatMapView getSeatMap(Integer hallId, User actor) {
        User current = loadActor(actor);
        Hall hall = hallRepository.findById(hallId)
                .orElseThrow(() -> new HallNotFoundException(hallId));
        assertCanManageBranch(current, hallBranchId(hall));

        boolean hasActiveReference = showtimeRepository.existsByHall_HallId(hallId);
        List<SeatListItem> seats = seatRepository.findByHall_HallIdOrderBySeatRowAscSeatNumberAsc(hallId).stream()
                .map(seat -> toListItem(seat, hasActiveReference, hall))
                .toList();

        return SeatMapView.builder()
                .hallId(hall.getHallId())
                .hallName(hall.getName())
                .branchName(hall.getBranch() != null ? hall.getBranch().getName() : "")
                .seatTypePrices(hallSeatTypePricingService.priceTable(hallId, hall.getTicketPrice()))
                .seats(seats)
                .totalSeats(seats.size())
                .empty(seats.isEmpty())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShowtimeSeatView> getSeatsForShowtime(Integer showtimeId) {
        Showtime showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy suất chiếu ID: " + showtimeId));
        if (SHOWTIME_CANCELLED.equals(showtime.getStatus())) {
            throw new IllegalArgumentException("Không tìm thấy suất chiếu ID: " + showtimeId);
        }

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

        return seats.stream().map(seat -> {
            String status = "AVAILABLE";
            if (bookedSeatIds.contains(seat.getSeatId())) {
                status = "BOOKED";
            } else if (reservedSeatIds.contains(seat.getSeatId())) {
                status = "RESERVED";
            }

            TicketPriceBreakdown price = ticketPricingService.priceForSeat(showtime, seat);
            return ShowtimeSeatView.builder()
                    .seatId(seat.getSeatId())
                    .seatRow(seat.getSeatRow())
                    .seatNumber(seat.getSeatNumber())
                    .type(SeatPricing.normalizeType(seat.getType()))
                    .status(status)
                    .finalPrice(price.effectivePrice())
                    .weekendPricing(price.weekendAdjusted())
                    .pricingLabel(price.adjustmentLabel())
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

        // Reuse existing seats where possible; seats outside the new layout are moved to maintenance.
        char startRow = request.getRowStart().charAt(0);
        char endRow = request.getRowEnd().charAt(0);

        if (startRow > endRow) {
            throw new SeatValidationException("seat.generate.invalid_row_range");
        }

        Set<String> vipRows = new HashSet<>(request.getVipRows() != null ? request.getVipRows() : List.of());
        Set<String> doubleRows = new HashSet<>(request.getDoubleRows() != null ? request.getDoubleRows() : List.of());
        List<Seat> existingSeats = seatRepository.findByHall_HallIdOrderBySeatRowAscSeatNumberAsc(hallId);
        Map<String, Seat> existingByPosition = new HashMap<>();
        for (Seat existingSeat : existingSeats) {
            existingByPosition.put(seatKey(existingSeat.getSeatRow(), existingSeat.getSeatNumber()), existingSeat);
        }
        Set<String> activeLayoutKeys = new HashSet<>();

        int totalCapacity = 0;
        for (char r = startRow; r <= endRow; r++) {
            String rowStr = String.valueOf(r);
            if (doubleRows.contains(rowStr)) {
                int totalCustomers = request.getSeatsPerRow();
                int doubleSeatsCount = totalCustomers / 2;
                int col = 1;

                for (int i = 0; i < doubleSeatsCount; i++) {
                    upsertSeat(hall, existingByPosition, activeLayoutKeys, rowStr, col++, SeatPricing.SupportedSeatType.COUPLE.name());
                    totalCapacity += 2;
                }
            } else {
                String seatType = vipRows.contains(rowStr)
                        ? SeatPricing.SupportedSeatType.VIP.name()
                        : SeatPricing.SupportedSeatType.STANDARD.name();
                for (int col = 1; col <= request.getSeatsPerRow(); col++) {
                    upsertSeat(hall, existingByPosition, activeLayoutKeys, rowStr, col, seatType);
                    totalCapacity += 1;
                }
            }
        }

        for (Seat existingSeat : existingSeats) {
            if (!activeLayoutKeys.contains(seatKey(existingSeat.getSeatRow(), existingSeat.getSeatNumber()))) {
                existingSeat.setMaintenanceStatus(MaintenanceStatus.UNDER_MAINTENANCE.name());
                seatRepository.save(existingSeat);
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

        String row = request.getSeatRow() != null ? request.getSeatRow().trim().toUpperCase() : seat.getSeatRow();
        Integer number = request.getSeatNumber() != null ? request.getSeatNumber() : seat.getSeatNumber();
        if (!row.equalsIgnoreCase(seat.getSeatRow()) || !number.equals(seat.getSeatNumber())) {
            boolean duplicate = seatRepository.existsByHall_HallIdAndSeatRowAndSeatNumberAndSeatIdNot(
                    seat.getHall().getHallId(), row, number, seatId);
            if (duplicate) {
                throw new SeatValidationException("seat.duplicate");
            }
        }

        seat.setSeatRow(row);
        seat.setSeatNumber(number);
        seat.setType(SeatPricing.normalizeType(request.getType()));
        if (request.getMaintenanceStatus() != null && !request.getMaintenanceStatus().isBlank()) {
            seat.setMaintenanceStatus(normalizeMaintenanceStatus(request.getMaintenanceStatus()));
        }
        seatRepository.save(seat);
    }

    @Override
    public void updateSeatMaintenance(Integer seatId, String maintenanceStatus, User actor) {
        User current = loadActor(actor);
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new SeatNotFoundException(seatId));
        assertCanManageBranch(current, hallBranchId(seat.getHall()));

        seat.setMaintenanceStatus(normalizeMaintenanceStatus(maintenanceStatus));
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
        int capacityChange = SeatPricing.SupportedSeatType.COUPLE.name().equals(SeatPricing.normalizeType(seat.getType())) ? 2 : 1;
        seat.setMaintenanceStatus(MaintenanceStatus.UNDER_MAINTENANCE.name());
        seatRepository.save(seat);

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

        Optional<Seat> existingSeat = seatRepository.findByHall_HallIdOrderBySeatRowAscSeatNumberAsc(hallId).stream()
                .filter(existing -> rowStr.equalsIgnoreCase(existing.getSeatRow())
                        && num.equals(existing.getSeatNumber()))
                .findFirst();
        if (existingSeat.isPresent()
                && !MaintenanceStatus.UNDER_MAINTENANCE.name().equals(existingSeat.get().getMaintenanceStatus())) {
            throw new SeatValidationException("seat.duplicate");
        }

        Seat seat = existingSeat.orElseGet(() -> {
            Seat newSeat = new Seat();
            newSeat.setHall(hall);
            newSeat.setSeatRow(rowStr);
            newSeat.setSeatNumber(num);
            return newSeat;
        });
        seat.setType(SeatPricing.normalizeType(request.getType()));
        seat.setMaintenanceStatus(MaintenanceStatus.AVAILABLE.name());
        seatRepository.save(seat);

        // Tăng sức chứa thực tế
        int capacityChange = SeatPricing.SupportedSeatType.COUPLE.name().equals(SeatPricing.normalizeType(request.getType())) ? 2 : 1;
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

        seatRepository.updateMaintenanceStatusByHallId(hallId, MaintenanceStatus.UNDER_MAINTENANCE.name());
        hall.setCapacity(0);
        hallRepository.save(hall);
    }

    private void upsertSeat(Hall hall,
                            Map<String, Seat> existingByPosition,
                            Set<String> activeLayoutKeys,
                            String row,
                            Integer number,
                            String type) {
        String key = seatKey(row, number);
        activeLayoutKeys.add(key);
        Seat seat = existingByPosition.get(key);
        if (seat == null) {
            seat = new Seat();
            seat.setHall(hall);
            seat.setSeatRow(row);
            seat.setSeatNumber(number);
        }
        seat.setType(SeatPricing.normalizeType(type));
        seat.setMaintenanceStatus(MaintenanceStatus.AVAILABLE.name());
        seatRepository.save(seat);
    }

    private static String seatKey(String row, Integer number) {
        return row + "-" + number;
    }

    private static String normalizeMaintenanceStatus(String status) {
        if (MaintenanceStatus.UNDER_MAINTENANCE.name().equalsIgnoreCase(status)) {
            return MaintenanceStatus.UNDER_MAINTENANCE.name();
        }
        if (MaintenanceStatus.AVAILABLE.name().equalsIgnoreCase(status)) {
            return MaintenanceStatus.AVAILABLE.name();
        }
        throw new SeatValidationException("seat.maintenance_status.invalid");
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

    private SeatListItem toListItem(Seat seat, boolean hasActiveReference, Hall hall) {
        return SeatListItem.builder()
                .seatId(seat.getSeatId())
                .seatRow(seat.getSeatRow())
                .seatNumber(seat.getSeatNumber())
                .type(SeatPricing.normalizeType(seat.getType()))
                .price(hallSeatTypePricingService.priceForSeat(hall, seat))
                .maintenanceStatus(seat.getMaintenanceStatus())
                .hasActiveReference(hasActiveReference)
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
