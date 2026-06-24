package com.cinema.hyperCinema.dto.ui.workspace;

import com.cinema.hyperCinema.dto.ui.admin.MetricCardView;
import com.cinema.hyperCinema.dto.ui.admin.SeriesPointView;
import com.cinema.hyperCinema.dto.ui.admin.TopMovieView;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkspaceDashboardView {

    private String role;

    private String actorName;

    private Integer branchId;

    private String branchName;

    private List<MetricCardView> metrics;

    private List<SeriesPointView> revenueSeries;

    private List<LeaderboardRowView> leaderboard;

    private List<TopMovieView> topMovies;

    private List<WorkspaceActionView> actions;

    private String lastUpdated;
}
