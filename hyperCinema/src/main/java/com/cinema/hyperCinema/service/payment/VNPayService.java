package com.cinema.hyperCinema.service.payment;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Service;

import com.cinema.hyperCinema.config.VNPayProperties;
import com.cinema.hyperCinema.model.Booking;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class VNPayService {

    private static final DateTimeFormatter VNPAY_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final VNPayProperties properties;

    public VNPayService(VNPayProperties properties) {
        this.properties = properties;
    }

    public String createPaymentUrl(Booking booking, HttpServletRequest request) {
        if (!properties.isConfigured()) {
            throw new IllegalStateException("VNPay is not configured");
        }

        LocalDateTime now = LocalDateTime.now();
        Map<String, String> params = new TreeMap<>();
        params.put("vnp_Version", properties.version());
        params.put("vnp_Command", properties.command());
        params.put("vnp_TmnCode", properties.tmnCode());
        params.put("vnp_Amount", String.valueOf(booking.getTotalPrice() * 100));
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", String.valueOf(booking.getBookingId()));
        params.put("vnp_OrderInfo", "Thanh toan ve HyperCinema #" + booking.getBookingId());
        params.put("vnp_OrderType", properties.orderType());
        params.put("vnp_Locale", "vn");
        params.put("vnp_ReturnUrl", properties.returnUrl());
        params.put("vnp_IpAddr", clientIp(request));
        params.put("vnp_CreateDate", now.format(VNPAY_DATE_FORMAT));
        params.put("vnp_ExpireDate", now.plusMinutes(15).format(VNPAY_DATE_FORMAT));

        String hashData = buildQuery(params, true);
        String query = buildQuery(params, true);
        return properties.payUrl() + "?" + query + "&vnp_SecureHash=" + hmacSha512(properties.hashSecret(), hashData);
    }

    public boolean isValidReturn(Map<String, String> requestParams) {
        String secureHash = requestParams.get("vnp_SecureHash");
        if (secureHash == null || secureHash.isBlank()) {
            return false;
        }

        Map<String, String> signedParams = requestParams.entrySet().stream()
                .filter(entry -> entry.getValue() != null && !entry.getValue().isBlank())
                .filter(entry -> !"vnp_SecureHash".equals(entry.getKey()))
                .filter(entry -> !"vnp_SecureHashType".equals(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (left, right) -> left, TreeMap::new));
        String hashData = buildQuery(signedParams, true);
        return secureHash.equalsIgnoreCase(hmacSha512(properties.hashSecret(), hashData));
    }

    public boolean isSuccessful(Map<String, String> requestParams) {
        return "00".equals(requestParams.get("vnp_ResponseCode"))
                && "00".equals(requestParams.get("vnp_TransactionStatus"));
    }

    private String buildQuery(Map<String, String> params, boolean encodeValues) {
        return params.entrySet().stream()
                .filter(entry -> entry.getValue() != null && !entry.getValue().isBlank())
                .map(entry -> entry.getKey() + "=" + (encodeValues ? encode(entry.getValue()) : entry.getValue()))
                .collect(Collectors.joining("&"));
    }

    private String encode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.US_ASCII.toString());
        } catch (UnsupportedEncodingException ex) {
            throw new IllegalStateException("Unable to encode VNPay parameter", ex);
        }
    }

    private String hmacSha512(String key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            byte[] bytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hash = new StringBuilder(bytes.length * 2);
            for (byte value : bytes) {
                hash.append(String.format("%02x", value));
            }
            return hash.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to sign VNPay request", ex);
        }
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "127.0.0.1";
    }
}
