package com.cinema.hyperCinema.service.report;

import com.cinema.hyperCinema.dto.report.revenue.RevenueReportFilter;
import com.cinema.hyperCinema.dto.report.revenue.RevenueReportView;
import com.cinema.hyperCinema.model.User;

public interface RevenueReportService {

    String REALIZED_PAYMENT_STATUS = "Completed";

    String EXCLUDED_BOOKING_STATUS = "Cancelled";

    RevenueReportView getAdminReport(RevenueReportFilter filter);

    RevenueReportView getManagerReport(User manager, RevenueReportFilter filter);
}
