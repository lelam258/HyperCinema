package com.cinema.hyperCinema.service.ui.impl;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cinema.hyperCinema.dto.admin.voucher.response.VoucherPreview;
import com.cinema.hyperCinema.dto.ui.booking.FoodAddonOptionView;
import com.cinema.hyperCinema.dto.ui.booking.PosSummaryView;
import com.cinema.hyperCinema.dto.ui.booking.SeatAvailabilityView;
import com.cinema.hyperCinema.dto.ui.booking.ShowtimeOptionView;
import com.cinema.hyperCinema.dto.ui.booking.VoucherOptionView;
import com.cinema.hyperCinema.dto.ui.booking.VoucherPreviewView;
import com.cinema.hyperCinema.model.Branch;
import com.cinema.hyperCinema.model.FoodItem;
import com.cinema.hyperCinema.model.Promotion;
import com.cinema.hyperCinema.model.Seat;
import com.cinema.hyperCinema.model.Showtime;
import com.cinema.hyperCinema.model.User;
import com.cinema.hyperCinema.repository.FoodItemRepository;
import com.cinema.hyperCinema.repository.PromotionRepository;
import com.cinema.hyperCinema.repository.SeatRepository;
import com.cinema.hyperCinema.repository.SeatReservationRepository;
import com.cinema.hyperCinema.repository.ShowtimeRepository;
import com.cinema.hyperCinema.repository.TicketRepository;
import com.cinema.hyperCinema.service.pricing.TicketPriceBreakdown;
import com.cinema.hyperCinema.service.pricing.TicketPricingService;
import com.cinema.hyperCinema.service.ui.BookingUiDataService;
import com.cinema.hyperCinema.service.voucher.VoucherApplicationService;
import com.cinema.hyperCinema.util.UiDisplayMapper;

@Service
public class BookingUiDataServiceImpl implements BookingUiDataService {

    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final String STATUS_CANCELLED_LEGACY = "Cancelled";
    private static final String STATUS_RESERVED = "ACTIVE";
    private static final String PROMOTION_ACTIVE = "ACTIVE";
    private static final String MAINTENANCE = "UNDER_MAINTENANCE";
    private static final String SHOWTIME_CANCELLED = "CANCELLED";

    private final ShowtimeRepository showtimeRepository;
    private final SeatRepository seatRepository;
    private final TicketRepository ticketRepository;
    private final SeatReservationRepository seatReservationRepository;
    private final FoodItemRepository foodItemRepository;
    private final PromotionRepository promotionRepository;
    private final VoucherApplicationService voucherApplicationService;
    private final UiDisplayMapper displayMapper;
    private final TicketPricingService ticketPricingService;

    public BookingUiDataServiceImpl(ShowtimeRepository showtimeRepository,
                                    SeatRepository seatRepository,
                                    TicketRepository ticketRepository,
                                    SeatReservationRepository seatReservationRepository,
                                    FoodItemRepository foodItemRepository,
                                    PromotionRepository promotionRepository,
                                    VoucherApplicationService voucherApplicationService,
                                    UiDisplayMapper displayMapper,
                                    TicketPricingService ticketPricingService) {
        this.showtimeRepository = showtimeRepository;
        this.seatRepository = seatRepository;
        this.ticketRepository = ticketRepository;
        this.seatReservationRepository = seatReservationRepository;
        this.foodItemRepository = foodItemRepository;
        this.promotionRepository = promotionRepository;
        this.voucherApplicationService = voucherApplicationService;
        this.displayMapper = displayMapper;
        this.ticketPricingService = ticketPricingService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShowtimeOptionView> upcomingShowtimes(User actor, int limit) {
        // đặt thời gian tại thời điểm hiện tại
        LocalDateTime now = LocalDateTime.now();

        Branch branch = actor != null ? actor.getBranch() : null;
        List<Showtime> showtimes = branch != null
                ? showtimeRepository.findByHall_Branch_BranchIdAndStartTimeAfterOrderByStartTimeAsc(
                branch.getBranchId(), now, PageRequest.of(0, limit))
                : showtimeRepository.findByStartTimeAfterOrderByStartTimeAsc(now, PageRequest.of(0, limit));
        return showtimes.stream()
                .map(this::toShowtimeOption)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SeatAvailabilityView> seatAvailability(Integer showtimeId, User actor) {
        Showtime showtime = showtimeRepository.findById(showtimeId).orElse(null);
        if (showtime == null || SHOWTIME_CANCELLED.equals(showtime.getStatus()) || !canAccessShowtime(showtime, actor)) {
            return Collections.emptyList();
        }
        List<Seat> seats = seatRepository.findByHall_HallIdOrderBySeatRowAscSeatNumberAsc(
                showtime.getHall().getHallId());
        Set<Integer> bookedSeatIds = new HashSet<>(
                ticketRepository.findUnavailableSeatIdsByShowtimeId(
                        showtimeId, Arrays.asList(STATUS_CANCELLED, STATUS_CANCELLED_LEGACY)));
        Set<Integer> reservedSeatIds = new HashSet<>(
                seatReservationRepository.findActiveReservedSeatIds(showtimeId, STATUS_RESERVED, LocalDateTime.now()));
        return seats.stream()
                .map(seat -> toSeatAvailability(seat, showtime, bookedSeatIds, reservedSeatIds))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<FoodAddonOptionView> availableFoodItems(User actor) {
        return foodItemRepository.findByIsAvailableTrueAndStockGreaterThanOrderByCategoryNameAscNameAsc(0)
                .stream()
                .map(this::toFoodOption)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<VoucherOptionView> availableVouchers(User actor, long orderValue, Integer requestedBranchId) {
        if (orderValue <= 0) {
            return Collections.emptyList();
        }
        Integer branchId = actor != null && actor.getBranch() != null
                ? actor.getBranch().getBranchId()
                : requestedBranchId;
        return promotionRepository.findAvailableVoucherCandidates(
                        PROMOTION_ACTIVE, LocalDateTime.now(), orderValue, branchId)
                .stream()
                .map(voucher -> toVoucherOption(voucher, branchId))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public VoucherPreviewView previewVoucher(String code, long orderValue, Integer branchId) {
        VoucherPreview preview = voucherApplicationService.validateAndPreview(
                code, orderValue, branchId, LocalDateTime.now());
        return VoucherPreviewView.builder()
                .code(preview.getCode())
                .valid(preview.isValid())
                .discountAmount(preview.getDiscountAmount())
                .displayDiscount(displayMapper.currency(preview.getDiscountAmount()))
                .message(preview.isValid() ? "voucher.apply.valid" : preview.getErrorKey())
                .build();
    }

    @Override
    public PosSummaryView emptyPosSummary(User actor) {
        return PosSummaryView.builder()
                .selectedSeats(Collections.emptyList())
                .selectedFoodItems(Collections.emptyList())
                .subtotal(0L)
                .discount(0L)
                .total(0L)
                .displaySubtotal(displayMapper.currency(0))
                .displayDiscount(displayMapper.currency(0))
                .displayTotal(displayMapper.currency(0))
                .validationMessages(Collections.emptyList())
                .build();
    }

    private ShowtimeOptionView toShowtimeOption(Showtime showtime) {
        return ShowtimeOptionView.builder()
                .showtimeId(showtime.getShowtimeId())
                .movieTitle(showtime.getMovie() != null ? showtime.getMovie().getTitle() : "Movie")
                .branchName(showtime.getHall() != null && showtime.getHall().getBranch() != null
                        ? showtime.getHall().getBranch().getName() : "")
                .hallName(showtime.getHall() != null ? showtime.getHall().getName() : "")
                .formatLabel(showtime.getHall() != null && showtime.getHall().getHallType() != null
                        ? showtime.getHall().getHallType()
                        : "Standard")
                .startTime(showtime.getStartTime())
                .endTime(showtime.getEndTime())
                .basePrice(minTicketPriceFor(showtime))
                .displayPrice(displayMapper.currency(minTicketPriceFor(showtime)))
                .weekendPricing(ticketPricingService.hasWeekendPricing(showtime))
                .pricingLabel(ticketPricingService.hasWeekendPricing(showtime) ? "Gia cuoi tuan" : null)
                .available(showtime.getStartTime() != null && showtime.getStartTime().isAfter(LocalDateTime.now()))
                .build();
    }

    private SeatAvailabilityView toSeatAvailability(Seat seat,
                                                    Showtime showtime,
                                                    Set<Integer> bookedSeatIds,
                                                    Set<Integer> reservedSeatIds) {
        String state = "available";
        boolean selectable = true;
        if (MAINTENANCE.equals(seat.getMaintenanceStatus())) {
            state = "maintenance";
            selectable = false;
        } else if (bookedSeatIds.contains(seat.getSeatId())) {
            state = "booked";
            selectable = false;
        } else if (reservedSeatIds.contains(seat.getSeatId())) {
            state = "reserved";
            selectable = false;
        }
        TicketPriceBreakdown price = ticketPricingService.priceForSeat(showtime, seat);
        return SeatAvailabilityView.builder()
                .seatId(seat.getSeatId())
                .row(seat.getSeatRow())
                .number(seat.getSeatNumber())
                .label(seat.getSeatRow() + seat.getSeatNumber())
                .type(com.cinema.hyperCinema.util.SeatPricing.normalizeType(seat.getType()))
                .price(price.effectivePrice())
                .displayPrice(displayMapper.currency(price.effectivePrice()))
                .weekendPricing(price.weekendAdjusted())
                .pricingLabel(price.adjustmentLabel())
                .state(state)
                .selectable(selectable)
                .build();
    }

    private FoodAddonOptionView toFoodOption(FoodItem item) {
        boolean available = Boolean.TRUE.equals(item.getIsAvailable()) && item.getStock() != null && item.getStock() > 0;
        return FoodAddonOptionView.builder()
                .itemId(item.getItemId())
                .name(item.getName())
                .categoryName(item.getCategoryName())
                .price(item.getPrice())
                .displayPrice(displayMapper.currency(item.getPrice()))
                .stock(item.getStock())
                .available(available)
                .unavailableReason(available ? null : "food.item.unavailable")
                .build();
    }

    private VoucherOptionView toVoucherOption(Promotion voucher, Integer branchId) {
        return VoucherOptionView.builder()
                .promotionId(voucher.getPromotionId())
                .code(voucher.getCode())
                .title(voucher.getTitle())
                .discountLabel(discountLabel(voucher))
                .minOrderValue(voucher.getMinOrderValue())
                .displayMinOrderValue(displayMapper.currency(voucher.getMinOrderValue()))
                .validUntil(displayMapper.dateTime(voucher.getEndDate()))
                .branchScope(branchScope(voucher, branchId))
                .eligible(true)
                .disabledReason(null)
                .build();
    }

    private String discountLabel(Promotion voucher) {
        if ("PERCENTAGE".equalsIgnoreCase(voucher.getDiscountType())) {
            return voucher.getDiscountValue() + "%";
        }
        return displayMapper.currency(voucher.getDiscountValue());
    }

    private String branchScope(Promotion voucher, Integer branchId) {
        if (!Boolean.TRUE.equals(voucher.getBranchSpecific())) {
            return "Toan he thong";
        }
        if (voucher.getBranch() != null && voucher.getBranch().getName() != null) {
            return voucher.getBranch().getName();
        }
        return branchId != null ? "Chi nhanh #" + branchId : "Chi nhanh";
    }

    private boolean canAccessShowtime(Showtime showtime, User actor) {
        if (actor == null || actor.getBranch() == null) {
            return true;
        }
        Integer actorBranchId = actor.getBranch().getBranchId();
        return showtime.getHall() != null
                && showtime.getHall().getBranch() != null
                && actorBranchId.equals(showtime.getHall().getBranch().getBranchId());
    }

    private Integer minTicketPriceFor(Showtime showtime) {
        if (showtime == null || showtime.getHall() == null) {
            return 0;
        }
        return ticketPricingService.minPriceForShowtime(showtime);
    }
}
