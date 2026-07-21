package com.cinema.hyperCinema.controller.payment;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.cinema.hyperCinema.config.SecurityConfig;
import com.cinema.hyperCinema.model.Booking;
import com.cinema.hyperCinema.model.Branch;
import com.cinema.hyperCinema.model.Hall;
import com.cinema.hyperCinema.model.Payment;
import com.cinema.hyperCinema.model.Role;
import com.cinema.hyperCinema.model.Showtime;
import com.cinema.hyperCinema.model.User;
import com.cinema.hyperCinema.security.CustomAuthenticationSuccessHandler;
import com.cinema.hyperCinema.security.CustomUserDetails;
import com.cinema.hyperCinema.service.booking.BookingService;
import com.cinema.hyperCinema.service.payment.BookingPaymentService;

@WebMvcTest(VietQrPaymentController.class)
@Import(SecurityConfig.class)
class VietQrPaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookingService bookingService;

    @MockitoBean
    private BookingPaymentService bookingPaymentService;

    @MockitoBean
    private CustomAuthenticationSuccessHandler successHandler;

    @Test
    void paymentPage_whenStaffAccessesOwnedBooking_rendersStaffPaymentView() throws Exception {
        Booking booking = booking(staffUser());
        when(bookingService.findById(38)).thenReturn(Optional.of(booking));
        when(bookingPaymentService.findPaymentByBookingId(38)).thenReturn(Optional.of(payment(booking)));

        mockMvc.perform(get("/payment/vietqr/38").with(user(details(staffUser()))))
                .andExpect(status().isOk())
                .andExpect(view().name("staff/vietqr-payment"));
    }

    @Test
    void paymentPage_whenCustomerAccessesOwnedBooking_redirectsToCustomerBookings() throws Exception {
        User customer = customerUser();
        Booking booking = booking(customer);
        when(bookingService.findById(38)).thenReturn(Optional.of(booking));
        when(bookingPaymentService.findPaymentByBookingId(38)).thenReturn(Optional.of(payment(booking)));

        mockMvc.perform(get("/payment/vietqr/38").with(user(details(customer))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/my/bookings"));
    }

    @Test
    void confirmPayment_whenCustomerPostsRedirectsToCustomerBookings() throws Exception {
        User customer = customerUser();
        Booking booking = booking(customer);
        when(bookingService.findById(38)).thenReturn(Optional.of(booking));

        mockMvc.perform(post("/payment/vietqr/38/confirm")
                        .with(csrf())
                        .with(user(details(customer))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/my/bookings"));

        verify(bookingPaymentService, never()).confirmPayment(eq(38));
    }

    @Test
    void confirmPayment_whenStaffConfirms_redirectsToStaffBookingDetail() throws Exception {
        Booking booking = booking(staffUser());
        when(bookingService.findById(38)).thenReturn(Optional.of(booking));

        mockMvc.perform(post("/payment/vietqr/38/confirm")
                        .with(csrf())
                        .with(user(details(staffUser()))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/staff/bookings/38"));

        verify(bookingPaymentService).confirmPayment(eq(38));
    }

    private Booking booking(User owner) {
        Booking booking = new Booking();
        booking.setBookingId(38);
        booking.setUser(owner);
        booking.setTotalPrice(90000L);
        booking.setShowtime(showtime(owner.getBranch()));
        return booking;
    }

    private Payment payment(Booking booking) {
        Payment payment = new Payment();
        payment.setPaymentId(5);
        payment.setBooking(booking);
        payment.setAmount(booking.getTotalPrice());
        payment.setMethod("VietQR");
        payment.setStatus("Pending");
        payment.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        return payment;
    }

    private Showtime showtime(Branch branch) {
        Hall hall = new Hall();
        hall.setHallId(3);
        hall.setBranch(branch);

        Showtime showtime = new Showtime();
        showtime.setShowtimeId(9);
        showtime.setHall(hall);
        return showtime;
    }

    private User staffUser() {
        Branch branch = new Branch();
        branch.setBranchId(2);
        branch.setName("Branch A");

        User user = appUser(7, "staff", "Staff");
        user.setFullName("Staff One");
        user.setBranch(branch);
        return user;
    }

    private User customerUser() {
        User user = appUser(8, "customer", "Customer");
        user.setFullName("Customer One");
        return user;
    }

    private User appUser(Integer userId, String username, String roleName) {
        Role role = new Role();
        role.setName(roleName);

        User user = new User();
        user.setUserId(userId);
        user.setUsername(username);
        user.setPasswordHash("x");
        user.setStatus("Active");
        user.setRole(role);
        return user;
    }

    private CustomUserDetails details(User user) {
        return new CustomUserDetails(user);
    }
}
