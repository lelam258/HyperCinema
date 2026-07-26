package com.cinema.hyperCinema.service.hall.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.cinema.hyperCinema.dto.admin.hall.request.HallSearchCriteria;
import com.cinema.hyperCinema.model.Branch;
import com.cinema.hyperCinema.model.Hall;
import com.cinema.hyperCinema.model.MaintenanceStatus;
import com.cinema.hyperCinema.model.Role;
import com.cinema.hyperCinema.model.Seat;
import com.cinema.hyperCinema.model.SeatType;
import com.cinema.hyperCinema.model.User;
import com.cinema.hyperCinema.model.WeekendTicketPricing;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cinema.hyperCinema.dto.admin.hall.request.HallCreateRequest;

import com.cinema.hyperCinema.dto.admin.hall.request.HallUpdateRequest;
import com.cinema.hyperCinema.dto.admin.hall.response.BranchOption;
import com.cinema.hyperCinema.dto.admin.hall.response.HallDetailView;
import com.cinema.hyperCinema.dto.admin.hall.response.HallListItem;
import com.cinema.hyperCinema.dto.admin.hall.response.HallManagementContext;
import com.cinema.hyperCinema.dto.admin.hall.response.SeatTypePriceView;
import com.cinema.hyperCinema.exception.hall.HallNotFoundException;
import com.cinema.hyperCinema.exception.hall.HallValidationException;
import com.cinema.hyperCinema.repository.BranchRepository;
import com.cinema.hyperCinema.repository.HallRepository;
import com.cinema.hyperCinema.repository.HallSpecifications;
import com.cinema.hyperCinema.repository.SeatRepository;
import com.cinema.hyperCinema.repository.ShowtimeRepository;
import com.cinema.hyperCinema.repository.UserRepository;
import com.cinema.hyperCinema.repository.WeekendTicketPricingRepository;
import com.cinema.hyperCinema.service.hall.HallService;
import com.cinema.hyperCinema.service.pricing.HallSeatTypePricingService;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class HallServiceImpl implements HallService {

    private static final String STATUS_ACTIVE = "Active";
    private static final String STATUS_INACTIVE = "Inactive";

    private final HallRepository hallRepository;
    private final BranchRepository branchRepository;
    private final SeatRepository seatRepository;
    private final ShowtimeRepository showtimeRepository;
    private final UserRepository userRepository;
    private final HallSeatTypePricingService hallSeatTypePricingService;
    private final WeekendTicketPricingRepository weekendTicketPricingRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<HallListItem> search(HallSearchCriteria criteria, Pageable pageable, User actor) {
        User current = loadActor(actor);
        Integer forcedBranchId = forcedBranchId(current);
        if (!isAdmin(current) && forcedBranchId == null) {
            return new PageImpl<>(List.of(), pageable, 0);
        }
        Specification<Hall> spec = HallSpecifications.matches(criteria, forcedBranchId);
        return hallRepository.findAll(spec, pageable).map(this::toListItem);
    }

    @Override
    @Transactional(readOnly = true)
    public HallDetailView findById(Integer hallId, User actor) {
        User current = loadActor(actor);
        Hall hall = hallRepository.findById(hallId)
                .orElseThrow(() -> new HallNotFoundException(hallId));
        assertCanManageBranch(current, hallBranchId(hall));
        return toDetailView(hall);
    }

    @Override
    public HallDetailView create(HallCreateRequest request, User actor) {
        User current = loadActor(actor);
        String name = normalizeName(request.getName());
        String hallType = normalizeRequiredText(request.getHallType(), "hall.type.required", 50, "hall.type.too_long");
        Integer ticketPrice = normalizeTicketPrice(request.getTicketPrice());
        int rowCount = normalizeLayoutSize(request.getRowCount(), "hall.rows.invalid", 26);
        int columnCount = normalizeLayoutSize(request.getColumnCount(), "hall.columns.invalid", 50);
        Integer capacity = rowCount * columnCount;
        String status = normalizeRequiredText(request.getStatus(), "hall.status.required", 50, "hall.status.too_long");
        Integer targetBranchId = resolveTargetBranchId(request.getBranchId(), current);
        Branch branch = branchRepository.findById(targetBranchId)
                .orElseThrow(() -> new HallValidationException("hall.branch.required"));
        ensureBranchActive(branch);

        if (hallRepository.existsByBranch_BranchIdAndNameIgnoreCase(targetBranchId, name)) {
            throw new HallValidationException("hall.name.duplicate");
        }

        Hall hall = new Hall();
        hall.setName(name);
        hall.setBranch(branch);
        hall.setHallType(hallType);
        hall.setTicketPrice(ticketPrice);
        hall.setCapacity(capacity);
        hall.setStatus(status);
        Hall saved = hallRepository.save(hall);
        saveSeatTypePrices(saved, request.getStandardPrice(), request.getVipPrice(),
                request.getCouplePrice(), request.getDisabledPrice(), ticketPrice);
        saveWeekendPricing(saved, request.getWeekendPricingActive(), request.getWeekendStandardPrice(),
                request.getWeekendVipPrice(), request.getWeekendCouplePrice(), request.getWeekendDisabledPrice(),
                request.getStandardPrice(), request.getVipPrice(), request.getCouplePrice(),
                request.getDisabledPrice(), ticketPrice);
        seatRepository.saveAll(buildInitialSeats(saved, rowCount, columnCount, status));
        return toDetailView(saved);
    }

    @Override
    public HallDetailView update(Integer hallId, HallUpdateRequest request, User actor) {
        User current = loadActor(actor);
        Hall hall = hallRepository.findById(hallId)
                .orElseThrow(() -> new HallNotFoundException(hallId));
        assertCanManageBranch(current, hallBranchId(hall));

        String name = normalizeName(request.getName());
        String hallType = normalizeRequiredText(request.getHallType(), "hall.type.required", 50, "hall.type.too_long");
        Integer ticketPrice = normalizeTicketPrice(request.getTicketPrice());
        Integer capacity = normalizeCapacity(request.getCapacity());
        String status = normalizeRequiredText(request.getStatus(), "hall.status.required", 50, "hall.status.too_long");
        Integer targetBranchId = hallBranchId(hall);

        if (isAdmin(current) && request.getBranchId() != null
                && !request.getBranchId().equals(targetBranchId)) {
            ensureCanMove(hallId);
            Branch targetBranch = branchRepository.findById(request.getBranchId())
                    .orElseThrow(() -> new HallValidationException("hall.branch.required"));
            ensureBranchActive(targetBranch);
            hall.setBranch(targetBranch);
            targetBranchId = targetBranch.getBranchId();
        } else if (!isAdmin(current) && request.getBranchId() != null
                && !request.getBranchId().equals(targetBranchId)) {
            throw new HallValidationException("hall.branch.scope_denied");
        }

        if (hallRepository.existsByBranch_BranchIdAndNameIgnoreCaseAndHallIdNot(
                targetBranchId, name, hallId)) {
            throw new HallValidationException("hall.name.duplicate");
        }

        hall.setName(name);
        hall.setHallType(hallType);
        hall.setTicketPrice(ticketPrice);
        hall.setCapacity(capacity);
        hall.setStatus(status);
        Hall saved = hallRepository.save(hall);
        saveSeatTypePrices(saved, request.getStandardPrice(), request.getVipPrice(),
                request.getCouplePrice(), request.getDisabledPrice(), ticketPrice);
        saveWeekendPricing(saved, request.getWeekendPricingActive(), request.getWeekendStandardPrice(),
                request.getWeekendVipPrice(), request.getWeekendCouplePrice(), request.getWeekendDisabledPrice(),
                request.getStandardPrice(), request.getVipPrice(), request.getCouplePrice(),
                request.getDisabledPrice(), ticketPrice);
        syncSeatMaintenanceStatus(saved.getHallId(), saved.getStatus());
        return toDetailView(saved);
    }

    @Override
    public void delete(Integer hallId, User actor) {
        User current = loadActor(actor);
        Hall hall = hallRepository.findById(hallId)
                .orElseThrow(() -> new HallNotFoundException(hallId));
        assertCanManageBranch(current, hallBranchId(hall));
        ensureCanDelete(hallId);
        hall.setStatus(STATUS_INACTIVE);
        hallRepository.save(hall);
        syncSeatMaintenanceStatus(hallId, hall.getStatus());
    }

    @Override
    @Transactional(readOnly = true)
    public HallManagementContext managementContext(User actor) {
        User current = loadActor(actor);
        boolean admin = isAdmin(current);
        BranchOption lockedBranch = null;
        if (!admin && current.getBranch() != null) {
            lockedBranch = toBranchOption(current.getBranch());
        }
        List<BranchOption> branches = admin
                ? branchRepository.findByStatusIgnoreCase("Active", Sort.by(Sort.Direction.ASC, "name")).stream()
                .map(this::toBranchOption)
                .toList()
                : List.of();
        return HallManagementContext.builder()
                .admin(admin)
                .sidebar(sidebarFor(current))
                .lockedBranch(lockedBranch)
                .branchOptions(branches)
                .build();
    }

    private User loadActor(User actor) {
        if (actor == null || actor.getUserId() == null) {
            throw new HallValidationException("hall.access.denied");
        }
        return userRepository.findById(actor.getUserId())
                .orElseThrow(() -> new HallValidationException("hall.access.denied"));
    }

    private Integer resolveTargetBranchId(Integer requestedBranchId, User actor) {
        if (isAdmin(actor)) {
            if (requestedBranchId == null) {
                throw new HallValidationException("hall.branch.required");
            }
            return requestedBranchId;
        }
        Integer branchId = forcedBranchId(actor);
        if (branchId == null) {
            throw new HallValidationException("hall.branch.scope_required");
        }
        if (requestedBranchId != null && !requestedBranchId.equals(branchId)) {
            throw new HallValidationException("hall.branch.scope_denied");
        }
        return branchId;
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

    private void ensureBranchActive(Branch branch) {
        if (branch == null || !STATUS_ACTIVE.equalsIgnoreCase(branch.getStatus())) {
            throw new HallValidationException("hall.branch.inactive");
        }
    }

    private void ensureCanMove(Integer hallId) {
        if (seatRepository.existsByHall_HallId(hallId) || showtimeRepository.existsByHall_HallId(hallId)) {
            throw new HallValidationException("hall.branch.cannot_move_with_dependencies");
        }
    }

    private void ensureCanDelete(Integer hallId) {
        if (showtimeRepository.existsByHall_HallIdAndStartTimeAfter(hallId, LocalDateTime.now())) {
            throw new HallValidationException("hall.cannot_delete_with_dependencies");
        }
    }

    private HallListItem toListItem(Hall hall) {
        Integer hallId = hall.getHallId();
        long seatCount = seatRepository.countByHall_HallId(hallId);
        long showtimeCount = showtimeRepository.countByHall_HallId(hallId);
        Branch branch = hall.getBranch();
        Optional<WeekendTicketPricing> weekendPricing = weekendPricing(hall);
        return HallListItem.builder()
                .hallId(hallId)
                .name(hall.getName())
                .branchId(branch == null ? null : branch.getBranchId())
                .branchName(branch == null ? "" : branch.getName())
                .city(branch == null ? "" : branch.getCity())
                .hallType(hall.getHallType())
                .ticketPrice(hall.getTicketPrice())
                .priceRange(priceRange(seatTypePrices(hall)))
                .seatTypePrices(seatTypePrices(hall))
                .weekendPricingActive(weekendPricing.map(pricing -> Boolean.TRUE.equals(pricing.getActive())).orElse(false))
                .weekendPricingLabel(weekendPricing.map(this::weekendPricingLabel).orElse(""))
                .capacity(hall.getCapacity())
                .status(hall.getStatus())
                .seatCount(seatCount)
                .showtimeCount(showtimeCount)
                .canDelete(!STATUS_INACTIVE.equalsIgnoreCase(hall.getStatus())
                        && !showtimeRepository.existsByHall_HallIdAndStartTimeAfter(hallId, LocalDateTime.now()))
                .build();
    }

    private HallDetailView toDetailView(Hall hall) {
        Integer hallId = hall.getHallId();
        long seatCount = seatRepository.countByHall_HallId(hallId);
        long showtimeCount = showtimeRepository.countByHall_HallId(hallId);
        Branch branch = hall.getBranch();
        Optional<WeekendTicketPricing> weekendPricing = weekendPricing(hall);
        return HallDetailView.builder()
                .hallId(hallId)
                .name(hall.getName())
                .branchId(branch == null ? null : branch.getBranchId())
                .branchName(branch == null ? "" : branch.getName())
                .city(branch == null ? "" : branch.getCity())
                .address(branch == null ? "" : branch.getAddress())
                .hallType(hall.getHallType())
                .ticketPrice(hall.getTicketPrice())
                .seatTypePrices(seatTypePrices(hall))
                .weekendPricingActive(weekendPricing.map(pricing -> Boolean.TRUE.equals(pricing.getActive())).orElse(false))
                .weekendStandardPrice(weekendPricing.map(WeekendTicketPricing::getStandardPrice).orElse(null))
                .weekendVipPrice(weekendPricing.map(WeekendTicketPricing::getVipPrice).orElse(null))
                .weekendCouplePrice(weekendPricing.map(WeekendTicketPricing::getCouplePrice).orElse(null))
                .weekendDisabledPrice(weekendPricing.map(WeekendTicketPricing::getDisabledPrice).orElse(0))
                .weekendPricingLabel(weekendPricing.map(this::weekendPricingLabel).orElse(""))
                .capacity(hall.getCapacity())
                .status(hall.getStatus())
                .seatCount(seatCount)
                .showtimeCount(showtimeCount)
                .canDelete(!STATUS_INACTIVE.equalsIgnoreCase(hall.getStatus())
                        && !showtimeRepository.existsByHall_HallIdAndStartTimeAfter(hallId, LocalDateTime.now()))
                .build();
    }

    private BranchOption toBranchOption(Branch branch) {
        return BranchOption.builder()
                .branchId(branch.getBranchId())
                .name(branch.getName())
                .city(branch.getCity())
                .build();
    }

    private static Integer hallBranchId(Hall hall) {
        Branch branch = hall.getBranch();
        return branch == null ? null : branch.getBranchId();
    }

    private static String normalizeName(String name) {
        return normalizeRequiredText(name, "hall.name.required", 50, "hall.name.too_long");
    }

    private static String normalizeRequiredText(String value, String requiredKey, int maxLength, String tooLongKey) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) {
            throw new HallValidationException(requiredKey);
        }
        if (normalized.length() > maxLength) {
            throw new HallValidationException(tooLongKey);
        }
        return normalized;
    }

    private static Integer normalizeCapacity(Integer capacity) {
        if (capacity == null || capacity < 1) {
            throw new HallValidationException("hall.capacity.invalid");
        }
        return capacity;
    }

    private static Integer normalizeTicketPrice(Integer ticketPrice) {
        if (ticketPrice == null || ticketPrice < 1) {
            throw new HallValidationException("hall.ticket_price.invalid");
        }
        return ticketPrice;
    }

    private void saveSeatTypePrices(Hall hall,
                                    Integer standardPrice,
                                    Integer vipPrice,
                                    Integer couplePrice,
                                    Integer disabledPrice,
                                    Integer fallbackTicketPrice) {
        try {
            hallSeatTypePricingService.savePriceTable(
                    hall,
                    standardPrice != null ? standardPrice : fallbackTicketPrice,
                    vipPrice != null ? vipPrice : fallbackTicketPrice,
                    couplePrice != null ? couplePrice : fallbackTicketPrice,
                    disabledPrice != null ? disabledPrice : 0);
        } catch (IllegalArgumentException ex) {
            throw new HallValidationException("hall.seat_type_price.invalid");
        }
    }

    private void saveWeekendPricing(Hall hall,
                                    Boolean active,
                                    Integer standardPrice,
                                    Integer vipPrice,
                                    Integer couplePrice,
                                    Integer disabledPrice,
                                    Integer baseStandardPrice,
                                    Integer baseVipPrice,
                                    Integer baseCouplePrice,
                                    Integer baseDisabledPrice,
                                    Integer fallbackTicketPrice) {
        Integer normalizedStandard = normalizeWeekendPrice(standardPrice, baseStandardPrice, fallbackTicketPrice, false);
        Integer normalizedVip = normalizeWeekendPrice(vipPrice, baseVipPrice, fallbackTicketPrice, false);
        Integer normalizedCouple = normalizeWeekendPrice(couplePrice, baseCouplePrice, fallbackTicketPrice, false);
        Integer normalizedDisabled = normalizeWeekendPrice(disabledPrice, baseDisabledPrice, 0, true);
        WeekendTicketPricing pricing = weekendTicketPricingRepository.findByHall_HallId(hall.getHallId())
                .orElseGet(() -> {
                    WeekendTicketPricing created = new WeekendTicketPricing();
                    created.setHall(hall);
                    return created;
                });
        pricing.setHall(hall);
        pricing.setName("Weekend pricing - " + hall.getName());
        pricing.setDaysOfWeek("SATURDAY,SUNDAY");
        pricing.setStandardPrice(normalizedStandard);
        pricing.setVipPrice(normalizedVip);
        pricing.setCouplePrice(normalizedCouple);
        pricing.setDisabledPrice(normalizedDisabled);
        pricing.setActive(Boolean.TRUE.equals(active));
        weekendTicketPricingRepository.save(pricing);
    }

    private Optional<WeekendTicketPricing> weekendPricing(Hall hall) {
        Integer hallId = hall == null ? null : hall.getHallId();
        return hallId == null ? Optional.empty() : weekendTicketPricingRepository.findByHall_HallId(hallId);
    }

    private static Integer normalizeWeekendPrice(Integer price, Integer basePrice, Integer fallback, boolean allowZero) {
        Integer normalized = price != null ? price : (basePrice != null ? basePrice : fallback);
        if (normalized == null || normalized < 0 || (!allowZero && normalized == 0)) {
            throw new HallValidationException("hall.weekend_pricing.value.invalid");
        }
        return normalized;
    }

    private String weekendPricingLabel(WeekendTicketPricing pricing) {
        if (pricing == null || !Boolean.TRUE.equals(pricing.getActive())) {
            return "";
        }
        return "Gia cuoi tuan";
    }

    private List<SeatTypePriceView> seatTypePrices(Hall hall) {
        return hallSeatTypePricingService.priceTable(hall.getHallId(), hall.getTicketPrice());
    }

    private static String priceRange(List<SeatTypePriceView> prices) {
        List<Integer> positivePrices = prices.stream()
                .map(SeatTypePriceView::getPrice)
                .filter(price -> price != null && price > 0)
                .sorted()
                .toList();
        if (positivePrices.isEmpty()) {
            return "0";
        }
        Integer min = positivePrices.get(0);
        Integer max = positivePrices.get(positivePrices.size() - 1);
        return min.equals(max) ? String.valueOf(min) : min + " - " + max;
    }

    private static int normalizeLayoutSize(Integer value, String invalidKey, int max) {
        if (value == null || value < 1 || value > max) {
            throw new HallValidationException(invalidKey);
        }
        return value;
    }

    private void syncSeatMaintenanceStatus(Integer hallId, String hallStatus) {
        seatRepository.updateMaintenanceStatusByHallId(hallId, maintenanceStatusForHallStatus(hallStatus));
    }

    private static String maintenanceStatusForHallStatus(String hallStatus) {
        if ("Active".equalsIgnoreCase(hallStatus)) {
            return MaintenanceStatus.AVAILABLE.name();
        }
        return MaintenanceStatus.UNDER_MAINTENANCE.name();
    }

    private static List<Seat> buildInitialSeats(Hall hall, int rowCount, int columnCount, String hallStatus) {
        List<Seat> seats = new ArrayList<>(rowCount * columnCount);
        String seatMaintenanceStatus = maintenanceStatusForHallStatus(hallStatus);
        for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
            String rowLabel = String.valueOf((char) ('A' + rowIndex));
            for (int seatNumber = 1; seatNumber <= columnCount; seatNumber++) {
                Seat seat = new Seat();
                seat.setHall(hall);
                seat.setSeatRow(rowLabel);
                seat.setSeatNumber(seatNumber);
                seat.setType(SeatType.STANDARD.name());
                seat.setMaintenanceStatus(seatMaintenanceStatus);
                seats.add(seat);
            }
        }
        return seats;
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

    private static String sidebarFor(User user) {
        if (isAdmin(user)) {
            return "admin";
        }
        if (isManager(user)) {
            return "manager";
        }
        return "branch";
    }
}
