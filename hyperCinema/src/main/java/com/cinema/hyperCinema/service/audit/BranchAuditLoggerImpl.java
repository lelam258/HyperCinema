package com.cinema.hyperCinema.service.audit;

import com.cinema.hyperCinema.dto.admin.branch.response.FieldChange;
import com.cinema.hyperCinema.model.AuditLog;
import com.cinema.hyperCinema.model.Branch;
import com.cinema.hyperCinema.model.User;
import com.cinema.hyperCinema.repository.AuditLogRepository;
import com.cinema.hyperCinema.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BranchAuditLoggerImpl implements BranchAuditLogger {

    private static final String ENTITY_TYPE = "Branch";

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logCreate(Branch branch, User admin) {
        save(admin, branch.getBranchId(), "CREATE", toJson(snapshot(branch)));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logUpdate(Branch oldBranch, Branch newBranch, List<FieldChange> changes, User admin) {
        save(admin, newBranch.getBranchId(), "UPDATE", toJson(changes));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logStatusChange(Branch branch, String oldStatus, String newStatus, User admin) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("oldStatus", oldStatus);
        body.put("newStatus", newStatus);
        save(admin, branch.getBranchId(), "STATUS_CHANGE", toJson(body));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAssignManager(Integer branchId, Integer userId, User admin) {
        save(admin, branchId, "ASSIGN_MANAGER", toJson(Map.of("userId", userId)));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logUnassignManager(Integer branchId, Integer userId, User admin) {
        save(admin, branchId, "UNASSIGN_MANAGER", toJson(Map.of("userId", userId)));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAssignStaff(Integer branchId, Integer userId, Integer managerId, User admin) {
        Map<String, Integer> body = new LinkedHashMap<>();
        body.put("userId", userId);
        body.put("managerId", managerId);
        save(admin, branchId, "ASSIGN_STAFF", toJson(body));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logUnassignStaff(Integer branchId, Integer userId, User admin) {
        save(admin, branchId, "UNASSIGN_STAFF", toJson(Map.of("userId", userId)));
    }

    private void save(User admin, Integer entityId, String action, String details) {
        AuditLog log = new AuditLog();
        log.setUser(userRepository.getReferenceById(admin.getUserId()));
        log.setEntityType(ENTITY_TYPE);
        log.setEntityId(entityId);
        log.setAction(action);
        log.setDetails(details);
        auditLogRepository.save(log);
    }

    private Map<String, Object> snapshot(Branch b) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", b.getName());
        body.put("address", b.getAddress());
        body.put("city", b.getCity());
        body.put("phone", b.getPhone());
        body.put("status", b.getStatus());
        body.put("openingTime", b.getOpeningTime());
        body.put("closingTime", b.getClosingTime());
        body.put("createdAt", b.getCreatedAt());
        return body;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JacksonException ex) {
            throw new RuntimeException("Failed to serialize audit details", ex);
        }
    }
}
