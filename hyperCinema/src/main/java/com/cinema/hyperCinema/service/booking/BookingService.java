package com.cinema.hyperCinema.service.booking;

import com.cinema.hyperCinema.model.Booking;
import com.cinema.hyperCinema.model.Showtime;
import com.cinema.hyperCinema.model.User;

import java.util.List;
import java.util.Optional;

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
}
