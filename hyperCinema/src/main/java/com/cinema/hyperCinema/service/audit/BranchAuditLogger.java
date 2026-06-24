package com.cinema.hyperCinema.service.audit;

import com.cinema.hyperCinema.dto.admin.branch.response.FieldChange;
import com.cinema.hyperCinema.model.Branch;
import com.cinema.hyperCinema.model.User;

import java.util.List;

public interface BranchAuditLogger {

    void logCreate(Branch branch, User admin);

    void logUpdate(Branch oldBranch, Branch newBranch, List<FieldChange> changes, User admin);

    void logStatusChange(Branch branch, String oldStatus, String newStatus, User admin);

    void logAssignManager(Integer branchId, Integer userId, User admin);

    void logUnassignManager(Integer branchId, Integer userId, User admin);

    void logAssignStaff(Integer branchId, Integer userId, Integer managerId, User admin);

    void logUnassignStaff(Integer branchId, Integer userId, User admin);
}
