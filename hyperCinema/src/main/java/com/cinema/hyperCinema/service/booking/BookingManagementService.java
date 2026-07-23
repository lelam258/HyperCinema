package com.cinema.hyperCinema.service.booking;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.cinema.hyperCinema.dto.staff.booking.BookingDetailView;
import com.cinema.hyperCinema.dto.staff.booking.BookingListItemView;
import com.cinema.hyperCinema.dto.staff.booking.BookingManagementFilter;
import com.cinema.hyperCinema.dto.staff.booking.BookingManagementSummary;
import com.cinema.hyperCinema.model.User;

public interface BookingManagementService {

    Page<BookingListItemView> findBookings(User actor, BookingManagementFilter filter, Pageable pageable);

    BookingManagementSummary summarize(User actor, BookingManagementFilter filter);

    BookingDetailView findDetail(User actor, Integer bookingId);

    BookingDetailView findCustomerDetail(User actor, Integer bookingId);

    void confirmPayment(User actor, Integer bookingId);

    void markServed(User actor, Integer bookingId);

    void cancel(User actor, Integer bookingId);
}
