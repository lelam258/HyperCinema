package com.cinema.hyperCinema.service.ui;

import com.cinema.hyperCinema.dto.ui.workspace.CustomerDashboardView;
import com.cinema.hyperCinema.dto.ui.workspace.WorkspaceDashboardView;
import com.cinema.hyperCinema.model.User;


public interface WorkspaceUiDataService {

    CustomerDashboardView getCustomerDashboard(User actor);

    WorkspaceDashboardView getManagerDashboard();

    WorkspaceDashboardView getBranchDashboard(User actor);

    WorkspaceDashboardView getStaffDashboard(User actor);
}
