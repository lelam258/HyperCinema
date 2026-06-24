package com.cinema.hyperCinema.service.booking;

import java.util.List;
import java.util.Optional;

import com.cinema.hyperCinema.model.Booking;
import com.cinema.hyperCinema.model.Showtime;
import com.cinema.hyperCinema.model.User;

public interface BookingService {

    List<Booking> findRecentBookingsByUser(Integer userId, int limit);

    Optional<Booking> findById(Integer bookingId);

    Booking save(Booking booking);

    Optional<Showtime> findShowtimeWithDetails(Integer showtimeId);

    List<Showtime> findUpcomingShowtimesForMovie(Integer movieId);

    Booking createPendingVietQrBooking(User user,
                                       Integer showtimeId,
                                       List<Integer> seatIds,
                                       List<Integer> foodItemIds,
                                       List<Integer> foodQuantities);

    Booking createPosBooking(User actor,
                             Integer showtimeId,
                             List<Integer> seatIds,
                             List<Integer> foodItemIds,
                             List<Integer> foodQuantities,
                             String paymentMethod,
                             String voucherCode,
                             String customerPhone);
}
