package com.cinema.hyperCinema.service.branch;

import com.cinema.hyperCinema.dto.admin.branch.request.BranchCreateRequest;
import com.cinema.hyperCinema.dto.admin.branch.request.BranchSearchCriteria;
import com.cinema.hyperCinema.dto.admin.branch.request.BranchUpdateRequest;
import com.cinema.hyperCinema.dto.admin.branch.response.BranchDetailView;
import com.cinema.hyperCinema.dto.admin.branch.response.BranchListItem;
import com.cinema.hyperCinema.dto.admin.branch.response.UpdateResult;
import com.cinema.hyperCinema.dto.admin.branch.response.UserSummary;
import com.cinema.hyperCinema.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface BranchService {

    BranchDetailView create(BranchCreateRequest request, User admin);

    UpdateResult update(Integer branchId, BranchUpdateRequest request, User admin);

    BranchDetailView findById(Integer branchId);

    Page<BranchListItem> search(BranchSearchCriteria criteria, Pageable pageable);

    void changeStatus(Integer branchId, String newStatus, User admin);

    void deleteHard(Integer branchId, User admin);

    void assignManager(Integer branchId, Integer userId, User admin);

    void unassignManager(Integer branchId, Integer userId, User admin);

    void assignStaff(Integer branchId, Integer userId, Integer managerId, User admin);

    void unassignStaff(Integer branchId, Integer userId, User admin);

    List<UserSummary> listUnassignedManagers();

    List<UserSummary> listUnassignedStaff();
}
