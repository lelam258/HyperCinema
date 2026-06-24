package com.cinema.hyperCinema.service;

import com.cinema.hyperCinema.config.VNPayProperties;
import com.cinema.hyperCinema.model.Booking;
import com.cinema.hyperCinema.repository.BookingRepository;
import com.cinema.hyperCinema.repository.PaymentRepository;
import com.cinema.hyperCinema.repository.TicketRepository;
import com.cinema.hyperCinema.service.impl.PaymentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private VNPayProperties vnpayProperties;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        when(vnpayProperties.version()).thenReturn("2.1.0");
        when(vnpayProperties.command()).thenReturn("pay");
        when(vnpayProperties.tmnCode()).thenReturn("TMNCODE");
        when(vnpayProperties.hashSecret()).thenReturn("SECRETKEY");
        when(vnpayProperties.payUrl()).thenReturn("https://vnpay.vn/pay");
        when(vnpayProperties.returnUrl()).thenReturn("http://localhost:8080/return");
        when(vnpayProperties.orderType()).thenReturn("other");
    }

    @Test
    void testCreateVNPayUrl() {
        Booking booking = new Booking();
        booking.setBookingId(123);
        booking.setTotalPrice(100000L);

        String ipAddress = "127.0.0.1";
        String url = paymentService.createVNPayUrl(booking, ipAddress);

        assertNotNull(url);
        assertTrue(url.startsWith("https://vnpay.vn/pay"));
        assertTrue(url.contains("vnp_TxnRef=123"));
        assertTrue(url.contains("vnp_Amount=10000000")); // Amount multiplied by 100
        assertTrue(url.contains("vnp_SecureHash="));
    }
}
