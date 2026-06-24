package com.cinema.hyperCinema.util;

import org.springframework.stereotype.Component;

import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Component
public class UiDisplayMapper {

    private static final Locale VIETNAM = new Locale("vi", "VN");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
    private static final DateTimeFormatter SHORT_DATE = DateTimeFormatter.ofPattern("dd/MM");

    public String currency(Number amount) {
        long value = amount == null ? 0L : amount.longValue();
        return NumberFormat.getNumberInstance(VIETNAM).format(value) + " VND";
    }

    public String integer(Number value) {
        long safeValue = value == null ? 0L : value.longValue();
        return NumberFormat.getIntegerInstance(VIETNAM).format(safeValue);
    }

    public String dateTime(LocalDateTime value) {
        return value == null ? "" : value.format(DATE_TIME);
    }

    public String shortDate(LocalDateTime value) {
        return value == null ? "" : value.format(SHORT_DATE);
    }

    public String statusLabel(String status) {
        if (status == null || status.isBlank()) {
            return "Unknown";
        }
        switch (status) {
            case "Active":
                return "Active";
            case "Inactive":
                return "Inactive";
            case "NowShowing":
                return "Now showing";
            case "ComingSoon":
                return "Coming soon";
            case "Ended":
                return "Ended";
            case "UNDER_MAINTENANCE":
                return "Maintenance";
            default:
                return status;
        }
    }
}
