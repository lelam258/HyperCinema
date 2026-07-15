package com.cinema.hyperCinema.dto.report.revenue;

public enum ShowtimeCoverageEvaluation {
    GOOD("Tot"),
    AVERAGE("Trung binh"),
    LOW("Thap"),
    NO_SALES("Chua co doanh thu"),
    NO_DATA("Khong co du lieu");

    private final String displayName;

    ShowtimeCoverageEvaluation(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
