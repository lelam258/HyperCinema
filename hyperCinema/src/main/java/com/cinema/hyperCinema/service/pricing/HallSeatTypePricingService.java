package com.cinema.hyperCinema.service.pricing;

import com.cinema.hyperCinema.dto.admin.hall.response.SeatTypePriceView;
import com.cinema.hyperCinema.model.Hall;
import com.cinema.hyperCinema.model.HallSeatTypePrice;
import com.cinema.hyperCinema.model.Seat;
import com.cinema.hyperCinema.repository.HallSeatTypePriceRepository;
import com.cinema.hyperCinema.util.SeatPricing;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HallSeatTypePricingService {

    private final HallSeatTypePriceRepository priceRepository;

    @Transactional(readOnly = true)
    public int priceForSeat(Hall hall, Seat seat) {
        if (hall == null) {
            throw new IllegalStateException("Phong chieu chua co cau hinh gia ve.");
        }
        String seatType = SeatPricing.normalizeType(seat == null ? null : seat.getType());
        return priceRepository.findByHall_HallIdAndSeatTypeAndActiveTrue(hall.getHallId(), seatType)
                .map(HallSeatTypePrice::getPrice)
                .filter(price -> price >= 0)
                .orElseGet(() -> fallbackPrice(hall, seatType));
    }

    @Transactional(readOnly = true)
    public List<SeatTypePriceView> priceTable(Integer hallId, Integer fallbackTicketPrice) {
        Map<SeatPricing.SupportedSeatType, Integer> prices = defaultPriceMap(fallbackTicketPrice);
        for (HallSeatTypePrice row : priceRepository.findByHall_HallIdOrderBySeatTypeAsc(hallId)) {
            SeatPricing.SupportedSeatType type = SeatPricing.supportedType(row.getSeatType());
            if (type != null && Boolean.TRUE.equals(row.getActive()) && row.getPrice() != null) {
                prices.put(type, row.getPrice());
            }
        }
        return SeatPricing.supportedTypes().stream()
                .map(type -> SeatTypePriceView.builder()
                        .seatType(type.name())
                        .label(SeatPricing.labelFor(type.name()))
                        .price(prices.get(type))
                        .build())
                .toList();
    }

    @Transactional
    public void savePriceTable(Hall hall,
                               Integer standardPrice,
                               Integer vipPrice,
                               Integer couplePrice,
                               Integer disabledPrice) {
        Map<SeatPricing.SupportedSeatType, Integer> requested = new EnumMap<>(SeatPricing.SupportedSeatType.class);
        requested.put(SeatPricing.SupportedSeatType.STANDARD, normalizePrice(standardPrice, false));
        requested.put(SeatPricing.SupportedSeatType.VIP, normalizePrice(vipPrice, false));
        requested.put(SeatPricing.SupportedSeatType.COUPLE, normalizePrice(couplePrice, false));
        requested.put(SeatPricing.SupportedSeatType.DISABLED, normalizePrice(disabledPrice, true));

        List<HallSeatTypePrice> existing = priceRepository.findByHall_HallIdOrderBySeatTypeAsc(hall.getHallId());
        for (Map.Entry<SeatPricing.SupportedSeatType, Integer> entry : requested.entrySet()) {
            HallSeatTypePrice row = existing.stream()
                    .filter(item -> entry.getKey().name().equals(SeatPricing.normalizeType(item.getSeatType())))
                    .findFirst()
                    .orElseGet(() -> {
                        HallSeatTypePrice created = new HallSeatTypePrice();
                        created.setHall(hall);
                        created.setSeatType(entry.getKey().name());
                        return created;
                    });
            row.setPrice(entry.getValue());
            row.setActive(true);
            priceRepository.save(row);
        }
    }

    private int fallbackPrice(Hall hall, String seatType) {
        Integer ticketPrice = hall.getTicketPrice();
        if (ticketPrice != null && ticketPrice > 0) {
            if (SeatPricing.SupportedSeatType.DISABLED.name().equals(seatType)) {
                return 0;
            }
            return ticketPrice;
        }
        return SeatPricing.defaultPriceFor(seatType);
    }

    private Map<SeatPricing.SupportedSeatType, Integer> defaultPriceMap(Integer fallbackTicketPrice) {
        Map<SeatPricing.SupportedSeatType, Integer> prices = new EnumMap<>(SeatPricing.SupportedSeatType.class);
        for (SeatPricing.SupportedSeatType type : SeatPricing.supportedTypes()) {
            int defaultPrice = fallbackTicketPrice != null && fallbackTicketPrice > 0
                    && type != SeatPricing.SupportedSeatType.DISABLED
                    ? fallbackTicketPrice
                    : SeatPricing.defaultPriceFor(type.name());
            prices.put(type, defaultPrice);
        }
        return prices;
    }

    private static Integer normalizePrice(Integer price, boolean allowZero) {
        if (price == null || price < 0 || (!allowZero && price == 0)) {
            throw new IllegalArgumentException("hall.seat_type_price.invalid");
        }
        return price;
    }
}
