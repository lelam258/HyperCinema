package com.cinema.hyperCinema.dto.ui.admin;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminDashboardView {

    private List<MetricCardView> metrics;

    private List<SeriesPointView> revenueSeries;

    private List<SeriesPointView> roleDistribution;

    private List<TopMovieView> topMovies;

    private List<ActivityLogView> recentLogs;

    private List<QuickActionView> quickActions;

    private String lastUpdated;
}
