package com.cinema.hyperCinema.dto.ui.workspace;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerDashboardView {

    private Integer userId;

    private String customerName;

    private String email;

    private String phone;

    private String membershipTier;

    private long rewardPoints;

    private CustomerMembershipProgressView membershipProgress;

    private List<WorkspaceActionView> actions;

    private String lastUpdated;
}
