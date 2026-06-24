package com.cinema.hyperCinema.dto.admin.branch.response;

import lombok.*;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchDetailView {

    private Integer branchId;

    private String name;

    private String address;

    private String city;

    private String phone;

    private String status;

    private LocalTime openingTime;

    private LocalTime closingTime;

    private LocalDateTime createdAt;

    private List<HallSummary> halls;

    private List<UserSummary> managers;

    private List<UserSummary> staffMembers;
}
