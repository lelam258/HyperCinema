package com.cinema.hyperCinema.service;

import java.time.LocalDate;
import java.util.Map;

public interface AnalyticsService {
    Map<String, Object> getTicketSalesReport(LocalDate from, LocalDate to, Integer branchId);
    Map<String, Object> getFoodSalesReport(LocalDate from, LocalDate to, Integer branchId);
    Map<String, Object> getHallOccupancyReport(LocalDate from, LocalDate to, Integer branchId);
    byte[] exportReport(String reportType, LocalDate from, LocalDate to, Integer branchId, String format);
}
