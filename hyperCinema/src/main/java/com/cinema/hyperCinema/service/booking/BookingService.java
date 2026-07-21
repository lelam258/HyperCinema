package com.cinema.hyperCinema.service.booking;

import com.cinema.hyperCinema.model.Booking;
import com.cinema.hyperCinema.model.Showtime;
import com.cinema.hyperCinema.model.User;
import com.cinema.hyperCinema.dto.booking.CustomerBookingHistoryFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface BookingService {

    List<Booking> findRecentBookingsByUser(Integer userId, int limit);

    Page<Booking> findBookingsByUser(Integer userId, Pageable pageable);

    Page<Booking> findBookingsByUser(Integer userId, CustomerBookingHistoryFilter filter, Pageable pageable);

    Optional<Booking> findById(Integer bookingId);

    Booking save(Booking booking);

    Optional<Showtime> findShowtimeWithDetails(Integer showtimeId);

    List<Showtime> findUpcomingShowtimesForMovie(Integer movieId);

    Booking createPendingVietQrBooking(User user,
                                       Integer showtimeId,
                                       List<Integer> seatIds,
                                       List<Integer> foodItemIds,
                                       List<Integer> foodQuantities,
                                       String voucherCode);

    Booking createPendingVNPayBooking(User user,
                                      Integer showtimeId,
                                      List<Integer> seatIds,
                                      List<Integer> foodItemIds,
                                      List<Integer> foodQuantities,
                                      String voucherCode);
}
