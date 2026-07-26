package com.cinema.hyperCinema.service.pricing;

import com.cinema.hyperCinema.model.Hall;
import com.cinema.hyperCinema.model.Seat;
import com.cinema.hyperCinema.model.Showtime;
import com.cinema.hyperCinema.model.WeekendTicketPricing;
import com.cinema.hyperCinema.repository.WeekendTicketPricingRepository;
import com.cinema.hyperCinema.util.SeatPricing;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TicketPricingService {

    private final HallSeatTypePricingService hallSeatTypePricingService;
    private final WeekendTicketPricingRepository weekendTicketPricingRepository;

    @Transactional(readOnly = true)
    public TicketPriceBreakdown priceForSeat(Showtime showtime, Seat seat) {
        int basePrice = hallSeatTypePricingService.priceForSeat(showtime != null ? showtime.getHall() : null, seat);
        WeekendTicketPricing pricing = matchingWeekendPricing(showtime);
        if (pricing == null) {
            return new TicketPriceBreakdown(basePrice, basePrice, false, null, null, 0, null);
        }

        int effectivePrice = configuredPriceForSeat(pricing, seat, basePrice);
        if (effectivePrice < 0) {
            throw new IllegalStateException("Cau hinh gia cuoi tuan khong hop le.");
        }
        return new TicketPriceBreakdown(
                basePrice,
                effectivePrice,
                effectivePrice != basePrice,
                "OVERRIDE",
                effectivePrice,
                effectivePrice - basePrice,
                "Gia cuoi tuan");
    }

    @Transactional(readOnly = true)
    public Integer minPriceForShowtime(Showtime showtime) {
        if (showtime == null || showtime.getHall() == null) {
            return 0;
        }
        return hallSeatTypePricingService.priceTable(showtime.getHall().getHallId(), showtime.getHall().getTicketPrice())
                .stream()
                .map(price -> previewPrice(showtime, price.getSeatType(), price.getPrice()))
                .filter(price -> price != null && price > 0)
                .min(Integer::compareTo)
                .orElse(0);
    }

    @Transactional(readOnly = true)
    public boolean hasWeekendPricing(Showtime showtime) {
        return matchingWeekendPricing(showtime) != null;
    }

    @Transactional(readOnly = true)
    public Integer adjustedPriceForBase(Showtime showtime, Integer basePrice) {
        return previewPrice(showtime, "STANDARD", basePrice);
    }

    @Transactional(readOnly = true)
    public Integer adjustedPriceForSeatType(Showtime showtime, String seatType, Integer basePrice) {
        return previewPrice(showtime, seatType, basePrice);
    }

    private Integer previewPrice(Showtime showtime, String seatType, Integer basePrice) {
        if (basePrice == null) {
            return null;
        }
        WeekendTicketPricing pricing = matchingWeekendPricing(showtime);
        if (pricing == null) {
            return basePrice;
        }
        return Math.max(0, configuredPriceForType(pricing, seatType, basePrice));
    }

    private WeekendTicketPricing matchingWeekendPricing(Showtime showtime) {
        Integer hallId = hallId(showtime);
        if (hallId == null || showtime == null || showtime.getStartTime() == null) {
            return null;
        }
        return weekendTicketPricingRepository.findByHall_HallIdAndActiveTrue(hallId)
                .filter(pricing -> configuredDays(pricing).contains(showtime.getStartTime().getDayOfWeek()))
                .orElse(null);
    }

    private int configuredPriceForSeat(WeekendTicketPricing pricing, Seat seat, int fallback) {
        String seatType = SeatPricing.normalizeType(seat == null ? null : seat.getType());
        return configuredPriceForType(pricing, seatType, fallback);
    }

    private int configuredPriceForType(WeekendTicketPricing pricing, String seatType, int fallback) {
        Integer price = switch (SeatPricing.normalizeType(seatType)) {
            case "VIP" -> pricing.getVipPrice();
            case "COUPLE" -> pricing.getCouplePrice();
            case "DISABLED" -> pricing.getDisabledPrice();
            default -> pricing.getStandardPrice();
        };
        return price != null ? price : fallback;
    }

    private Set<DayOfWeek> configuredDays(WeekendTicketPricing pricing) {
        String rawDays = pricing != null ? pricing.getDaysOfWeek() : null;
        if (rawDays == null || rawDays.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(rawDays.split(","))
                .map(value -> value.trim().toUpperCase(Locale.ROOT))
                .filter(value -> !value.isBlank())
                .map(DayOfWeek::valueOf)
                .collect(Collectors.toSet());
    }

    private Integer hallId(Showtime showtime) {
        Hall hall = showtime != null ? showtime.getHall() : null;
        return hall != null ? hall.getHallId() : null;
    }
}
