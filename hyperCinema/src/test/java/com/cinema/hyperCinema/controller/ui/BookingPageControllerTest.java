package com.cinema.hyperCinema.controller.ui;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.cinema.hyperCinema.config.SecurityConfig;
import com.cinema.hyperCinema.model.Booking;
import com.cinema.hyperCinema.model.Role;
import com.cinema.hyperCinema.model.User;
import com.cinema.hyperCinema.repository.ReviewInteractionRepository;
import com.cinema.hyperCinema.repository.ReviewRepository;
import com.cinema.hyperCinema.security.CustomAuthenticationSuccessHandler;
import com.cinema.hyperCinema.security.CustomUserDetails;
import com.cinema.hyperCinema.service.NotificationService;
import com.cinema.hyperCinema.service.booking.BookingService;
import com.cinema.hyperCinema.service.movie.MovieService;
import com.cinema.hyperCinema.service.payment.VNPayService;
import com.cinema.hyperCinema.service.ui.BookingUiDataService;
import com.cinema.hyperCinema.service.ui.WorkspaceUiDataService;

@WebMvcTest(BookingPageController.class)
@Import(SecurityConfig.class)
class BookingPageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookingUiDataService bookingUiDataService;

    @MockitoBean
    private WorkspaceUiDataService workspaceUiDataService;

    @MockitoBean
    private MovieService movieService;

    @MockitoBean
    private BookingService bookingService;

    @MockitoBean
    private VNPayService vnPayService;

    @MockitoBean
    private ReviewRepository reviewRepository;

    @MockitoBean
    private ReviewInteractionRepository reviewInteractionRepository;

    @MockitoBean
    private CustomAuthenticationSuccessHandler successHandler;

    @MockitoBean
    private NotificationService notificationService;

    @Test
    void createBooking_whenCustomerSelectsVNPay_redirectsToGatewayPaymentUrl() throws Exception {
        Booking booking = new Booking();
        booking.setBookingId(41);
        when(bookingService.createPendingVNPayBooking(any(User.class), eq(9), eq(List.of(1, 2)), any(), any(), any()))
                .thenReturn(booking);
        when(vnPayService.createPaymentUrl(eq(booking), any()))
                .thenReturn("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?vnp_TxnRef=41&vnp_SecureHash=signed");

        mockMvc.perform(post("/booking")
                        .param("showtimeId", "9")
                        .param("seatIds", "1", "2")
                        .param("paymentMethod", "VNPay")
                        .with(csrf())
                        .with(user(details("Customer"))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?vnp_TxnRef=41&vnp_SecureHash=signed"));

        verify(vnPayService).validatePaymentConfiguration();
        verify(bookingService).createPendingVNPayBooking(
                any(User.class), eq(9), eq(List.of(1, 2)), any(), any(), any());
        verify(vnPayService).createPaymentUrl(eq(booking), any());
    }

    @Test
    void createBooking_whenCustomerSelectsVNPayAndConfigurationIsIncomplete_redirectsBackToCheckout() throws Exception {
        org.mockito.Mockito.doThrow(new IllegalStateException("Cau hinh VNPay chua hoan tat."))
                .when(vnPayService).validatePaymentConfiguration();

        mockMvc.perform(post("/booking")
                        .param("showtimeId", "9")
                        .param("seatIds", "1")
                        .param("paymentMethod", "VNPay")
                        .with(csrf())
                        .with(user(details("Customer"))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/booking?showtimeId=9"));

        verify(vnPayService).validatePaymentConfiguration();
        verify(bookingService, never()).createPendingVNPayBooking(any(), any(), any(), any(), any(), any());
        verify(vnPayService, never()).createPaymentUrl(any(), any());
    }

    @Test
    void createBooking_whenCustomerSubmitsRemovedMethod_redirectsBackToCheckout() throws Exception {
        mockMvc.perform(post("/booking")
                        .param("showtimeId", "9")
                        .param("seatIds", "1")
                        .param("paymentMethod", "VietQR")
                        .with(csrf())
                        .with(user(details("Customer"))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/booking?showtimeId=9"));

        verify(bookingService, never()).createPendingVNPayBooking(any(), any(), any(), any(), any(), any());
        verify(vnPayService, never()).validatePaymentConfiguration();
        verify(vnPayService, never()).createPaymentUrl(any(), any());
    }

    @Test
    void createBooking_whenStaffSubmitsVNPay_redirectsToGatewayPaymentUrl() throws Exception {
        Booking booking = new Booking();
        booking.setBookingId(42);
        when(bookingService.createPendingVNPayBooking(any(User.class), eq(9), eq(List.of(1)), any(), any(), any()))
                .thenReturn(booking);
        when(vnPayService.createPaymentUrl(eq(booking), any()))
                .thenReturn("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?vnp_TxnRef=42&vnp_SecureHash=signed");

        mockMvc.perform(post("/booking")
                        .param("showtimeId", "9")
                        .param("seatIds", "1")
                        .param("paymentMethod", "VNPay")
                        .with(csrf())
                        .with(user(details("Staff"))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?vnp_TxnRef=42&vnp_SecureHash=signed"));

        verify(vnPayService).validatePaymentConfiguration();
        verify(bookingService).createPendingVNPayBooking(
                any(User.class), eq(9), eq(List.of(1)), any(), any(), any());
        verify(vnPayService).createPaymentUrl(eq(booking), any());
    }

    @Test
    void createBooking_whenStaffUsesRemovedMethod_redirectsBackToStaffPos() throws Exception {
        mockMvc.perform(post("/booking")
                        .param("showtimeId", "9")
                        .param("seatIds", "1")
                        .param("paymentMethod", "Cash")
                        .with(csrf())
                        .with(user(details("Staff"))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/staff/booking"));

        verify(bookingService, never()).createPendingVNPayBooking(any(), any(), any(), any(), any(), any());
        verify(vnPayService, never()).validatePaymentConfiguration();
        verify(vnPayService, never()).createPaymentUrl(any(), any());
    }

    @Test
    void createBooking_whenStaffUsesCard_redirectsBackToStaffPos() throws Exception {
        mockMvc.perform(post("/booking")
                        .param("showtimeId", "9")
                        .param("seatIds", "1")
                        .param("paymentMethod", "Card")
                        .with(csrf())
                        .with(user(details("Staff"))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/staff/booking"));

        verify(bookingService, never()).createPendingVNPayBooking(any(), any(), any(), any(), any(), any());
        verify(vnPayService, never()).validatePaymentConfiguration();
        verify(vnPayService, never()).createPaymentUrl(any(), any());
    }

    @Test
    void createBooking_passesVoucherCodeToBookingService() throws Exception {
        Booking booking = new Booking();
        booking.setBookingId(39);
        when(bookingService.createPendingVNPayBooking(any(User.class), eq(9), eq(List.of(1)), any(), any(), eq("SAVE10")))
                .thenReturn(booking);
        when(vnPayService.createPaymentUrl(eq(booking), any()))
                .thenReturn("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?vnp_TxnRef=39&vnp_SecureHash=signed");

        mockMvc.perform(post("/booking")
                        .param("showtimeId", "9")
                        .param("seatIds", "1")
                        .param("paymentMethod", "VNPay")
                        .param("voucherCode", "SAVE10")
                        .with(csrf())
                        .with(user(details("Customer"))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?vnp_TxnRef=39&vnp_SecureHash=signed"));

        verify(bookingService).createPendingVNPayBooking(
                any(User.class), eq(9), eq(List.of(1)), any(), any(), eq("SAVE10"));
    }

    @Test
    void createBooking_whenStaffSelectedVoucher_passesVoucherCodeToBookingService() throws Exception {
        Booking booking = new Booking();
        booking.setBookingId(40);
        when(bookingService.createPendingVNPayBooking(any(User.class), eq(9), eq(List.of(1)), any(), any(), eq("STAFF50")))
                .thenReturn(booking);
        when(vnPayService.createPaymentUrl(eq(booking), any()))
                .thenReturn("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?vnp_TxnRef=40&vnp_SecureHash=signed");

        mockMvc.perform(post("/booking")
                        .param("showtimeId", "9")
                        .param("seatIds", "1")
                        .param("paymentMethod", "VNPay")
                        .param("voucherCode", "STAFF50")
                        .with(csrf())
                        .with(user(details("Staff"))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?vnp_TxnRef=40&vnp_SecureHash=signed"));

        verify(bookingService).createPendingVNPayBooking(
                any(User.class), eq(9), eq(List.of(1)), any(), any(), eq("STAFF50"));
    }

    private CustomUserDetails details(String roleName) {
        Role role = new Role();
        role.setName(roleName);

        User user = new User();
        user.setUserId(7);
        user.setUsername(roleName.toLowerCase());
        user.setPasswordHash("x");
        user.setStatus("Active");
        user.setRole(role);

        return new CustomUserDetails(user);
    }
}
