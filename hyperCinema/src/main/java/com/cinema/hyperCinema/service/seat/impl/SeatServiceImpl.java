package com.cinema.hyperCinema.service.seat.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cinema.hyperCinema.dto.admin.seat.request.SeatBulkCreateRequest;
import com.cinema.hyperCinema.dto.admin.seat.request.SeatCreateRequest;
import com.cinema.hyperCinema.dto.admin.seat.request.SeatUpdateRequest;
import com.cinema.hyperCinema.dto.admin.seat.response.BulkCreateResult;
import com.cinema.hyperCinema.dto.admin.seat.response.SeatListItem;
import com.cinema.hyperCinema.dto.admin.seat.response.SeatManagementContext;
import com.cinema.hyperCinema.dto.admin.seat.response.SeatMapView;
import com.cinema.hyperCinema.exception.seat.SeatNotFoundException;
import com.cinema.hyperCinema.exception.seat.SeatValidationException;
import com.cinema.hyperCinema.model.Branch;
import com.cinema.hyperCinema.model.Hall;
import com.cinema.hyperCinema.model.MaintenanceStatus;
import com.cinema.hyperCinema.model.Role;
import com.cinema.hyperCinema.model.Seat;
import com.cinema.hyperCinema.model.SeatType;
import com.cinema.hyperCinema.model.User;
import com.cinema.hyperCinema.repository.HallRepository;
import com.cinema.hyperCinema.repository.SeatRepository;
import com.cinema.hyperCinema.repository.SeatReservationRepository;
import com.cinema.hyperCinema.repository.TicketRepository;
import com.cinema.hyperCinema.repository.UserRepository;
import com.cinema.hyperCinema.service.seat.SeatService;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class SeatServiceImpl implements SeatService {

    private final SeatRepository seatRepository;
    private final HallRepository hallRepository;
    private final TicketRepository ticketRepository;
    private final SeatReservationRepository seatReservationRepository;
    private final UserRepository userRepository;

    // ── Public API ──────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public SeatMapView getSeatMap(Integer hallId, User actor) {
        User current = loadActor(actor);
        Hall hall = resolveHall(hallId);
        assertCanManageHall(current, hall);

        List<Seat> seats = seatRepository.findByHall_HallIdOrderBySeatRowAscSeatNumberAsc(hallId);
        List<SeatListItem> items = seats.stream()
                .map(this::toSeatListItem)
                .toList();

        Branch branch = hall.getBranch();
        return SeatMapView.builder()
                .hallId(hall.getHallId())
                .hallName(hall.getName())
                .branchName(branch == null ? "" : branch.getName())
                .seats(items)
                .totalSeats(items.size())
                .empty(items.isEmpty())
                .build();
    }

    @Override
    public SeatListItem create(Integer hallId, SeatCreateRequest request, User actor) {
        User current = loadActor(actor);
        Hall hall = resolveHall(hallId);
        assertCanManageHall(current, hall);

        // Validate seatRow
        String seatRow = request.getSeatRow();
        if (seatRow == null || seatRow.isBlank()) {
            throw new SeatValidationException("seat.row.required");
        }
        if (seatRow.length() > 5) {
            throw new SeatValidationException("seat.row.too_long");
        }

        // Validate seatNumber
        Integer seatNumber = request.getSeatNumber();
        if (seatNumber == null || seatNumber < 1) {
            throw new SeatValidationException("seat.number.invalid");
        }

        // Validate type
        String type = request.getType();
        if (type == null || type.isBlank() || !isValidSeatType(type)) {
            throw new SeatValidationException("seat.type.invalid");
        }

        // Validate maintenanceStatus (optional)
        String maintenanceStatus = request.getMaintenanceStatus();
        if (maintenanceStatus != null && !maintenanceStatus.isBlank()) {
            if (!isValidMaintenanceStatus(maintenanceStatus)) {
                throw new SeatValidationException("seat.maintenance_status.invalid");
            }
        } else {
            maintenanceStatus = MaintenanceStatus.AVAILABLE.name();
        }

        // Check duplicate
        if (seatRepository.existsByHall_HallIdAndSeatRowAndSeatNumber(hallId, seatRow, seatNumber)) {
            throw new SeatValidationException("seat.duplicate");
        }

        // Create and save seat
        Seat seat = new Seat();
        seat.setHall(hall);
        seat.setSeatRow(seatRow);
        seat.setSeatNumber(seatNumber);
        seat.setType(type.toUpperCase());
        seat.setMaintenanceStatus(maintenanceStatus.toUpperCase());

        Seat saved = seatRepository.save(seat);
        return toSeatListItem(saved);
    }

    @Override
    public BulkCreateResult bulkCreate(Integer hallId, SeatBulkCreateRequest request, User actor) {
        User current = loadActor(actor);
        Hall hall = resolveHall(hallId);
        assertCanManageHall(current, hall);

        List<SeatBulkCreateRequest.RowSpec> rows = request.getRows();

        // Validate each RowSpec
        for (SeatBulkCreateRequest.RowSpec row : rows) {
            if (row.getRowLabel() == null || row.getRowLabel().isBlank()) {
                throw new SeatValidationException("seat.row.required");
            }
            if (row.getRowLabel().length() > 5) {
                throw new SeatValidationException("seat.row.too_long");
            }
            if (row.getSeatCount() == null || row.getSeatCount() < 1) {
                throw new SeatValidationException("seat.number.invalid");
            }
        }

        // Determine type: use provided value if valid, else default to STANDARD
        String type;
        if (request.getType() != null && !request.getType().isBlank()) {
            if (!isValidSeatType(request.getType())) {
                throw new SeatValidationException("seat.type.invalid");
            }
            type = request.getType().toUpperCase();
        } else {
            type = SeatType.STANDARD.name();
        }

        // Determine maintenanceStatus: use provided value if valid, else default to AVAILABLE
        String maintenanceStatus;
        if (request.getMaintenanceStatus() != null && !request.getMaintenanceStatus().isBlank()) {
            if (!isValidMaintenanceStatus(request.getMaintenanceStatus())) {
                throw new SeatValidationException("seat.maintenance_status.invalid");
            }
            maintenanceStatus = request.getMaintenanceStatus().toUpperCase();
        } else {
            maintenanceStatus = MaintenanceStatus.AVAILABLE.name();
        }

        List<Integer> columnAislesAfter = parsePositiveIntegerList(request.getColumnAislesAfter());

        // Generate all (rowLabel, seatNumber) pairs and check internal duplicates
        List<String> keys = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (SeatBulkCreateRequest.RowSpec row : rows) {
            String rowLabel = row.getRowLabel().trim();
            for (int num = 1; num <= row.getSeatCount(); num++) {
                int seatNumber = displaySeatNumber(num, columnAislesAfter);
                String key = rowLabel + "-" + seatNumber;
                // Check internal duplicates
                if (!seen.add(key)) {
                    throw new SeatValidationException("seat.bulk.internal_duplicate");
                }
                keys.add(key);
            }
        }

        // Check DB duplicates via batch query
        if (!keys.isEmpty()) {
            List<Seat> existing = seatRepository.findByHallIdAndRowNumberKeys(hallId, keys);
            if (!existing.isEmpty()) {
                throw new SeatValidationException("seat.bulk.conflict");
            }
        }

        // Create Seat entities for all pairs
        List<Seat> seats = new ArrayList<>();
        for (SeatBulkCreateRequest.RowSpec row : rows) {
            String rowLabel = row.getRowLabel().trim();
            for (int num = 1; num <= row.getSeatCount(); num++) {
                int seatNumber = displaySeatNumber(num, columnAislesAfter);
                Seat seat = new Seat();
                seat.setHall(hall);
                seat.setSeatRow(rowLabel);
                seat.setSeatNumber(seatNumber);
                seat.setType(type);
                seat.setMaintenanceStatus(maintenanceStatus);
                seats.add(seat);
            }
        }

        // Save all
        List<Seat> saved = seatRepository.saveAll(seats);

        return BulkCreateResult.builder()
                .createdCount(saved.size())
                .hallId(hallId)
                .build();
    }

    @Override
    public BulkCreateResult addRow(Integer hallId, String type, User actor) {
        User current = loadActor(actor);
        Hall hall = resolveHall(hallId);
        assertCanManageHall(current, hall);

        String seatType = seatTypeOrDefault(type);
        List<Seat> existingSeats = seatRepository.findByHall_HallIdOrderBySeatRowAscSeatNumberAsc(hallId);
        if (existingSeats.isEmpty()) {
            throw new SeatValidationException("seat.layout.empty");
        }

        String nextRow = nextRowLabel(existingSeats);
        List<Integer> seatNumbers = existingSeats.stream()
                .map(Seat::getSeatNumber)
                .filter(number -> number != null && number > 0)
                .distinct()
                .sorted()
                .toList();
        if (seatNumbers.isEmpty()) {
            throw new SeatValidationException("seat.layout.empty");
        }

        List<Seat> newSeats = new ArrayList<>();
        for (Integer seatNumber : seatNumbers) {
            Seat seat = new Seat();
            seat.setHall(hall);
            seat.setSeatRow(nextRow);
            seat.setSeatNumber(seatNumber);
            seat.setType(seatType);
            seat.setMaintenanceStatus(MaintenanceStatus.AVAILABLE.name());
            newSeats.add(seat);
        }

        List<Seat> saved = seatRepository.saveAll(newSeats);
        return BulkCreateResult.builder()
                .createdCount(saved.size())
                .hallId(hallId)
                .build();
    }

    @Override
    public BulkCreateResult addColumn(Integer hallId, String type, User actor) {
        User current = loadActor(actor);
        Hall hall = resolveHall(hallId);
        assertCanManageHall(current, hall);

        String seatType = seatTypeOrDefault(type);
        List<Seat> existingSeats = seatRepository.findByHall_HallIdOrderBySeatRowAscSeatNumberAsc(hallId);
        if (existingSeats.isEmpty()) {
            throw new SeatValidationException("seat.layout.empty");
        }

        Map<String, Integer> maxNumberByRow = new LinkedHashMap<>();
        for (Seat seat : existingSeats) {
            maxNumberByRow.merge(seat.getSeatRow(), seat.getSeatNumber(), Math::max);
        }

        List<Seat> newSeats = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : maxNumberByRow.entrySet()) {
            Seat seat = new Seat();
            seat.setHall(hall);
            seat.setSeatRow(entry.getKey());
            seat.setSeatNumber(entry.getValue() + 1);
            seat.setType(seatType);
            seat.setMaintenanceStatus(MaintenanceStatus.AVAILABLE.name());
            newSeats.add(seat);
        }

        List<Seat> saved = seatRepository.saveAll(newSeats);
        return BulkCreateResult.builder()
                .createdCount(saved.size())
                .hallId(hallId)
                .build();
    }

    @Override
    public void insertColumnAisle(Integer hallId, Integer afterColumn, User actor) {
        User current = loadActor(actor);
        Hall hall = resolveHall(hallId);
        assertCanManageHall(current, hall);

        if (afterColumn == null || afterColumn < 1) {
            throw new SeatValidationException("seat.aisle.invalid");
        }

        List<Seat> seats = seatRepository.findByHall_HallIdOrderBySeatRowAscSeatNumberAsc(hallId).stream()
                .filter(seat -> seat.getSeatNumber() != null && seat.getSeatNumber() > afterColumn)
                .sorted(Comparator.comparing(Seat::getSeatNumber).reversed())
                .toList();

        for (Seat seat : seats) {
            seat.setSeatNumber(seat.getSeatNumber() + 1);
            seatRepository.saveAndFlush(seat);
        }
    }

    @Override
    public void insertRowAisle(Integer hallId, String afterRow, User actor) {
        User current = loadActor(actor);
        Hall hall = resolveHall(hallId);
        assertCanManageHall(current, hall);

        if (afterRow == null || afterRow.trim().length() != 1) {
            throw new SeatValidationException("seat.aisle.invalid");
        }

        char after = Character.toUpperCase(afterRow.trim().charAt(0));
        if (after < 'A' || after >= 'Z') {
            throw new SeatValidationException("seat.aisle.invalid");
        }

        List<Seat> existingSeats = seatRepository.findByHall_HallIdOrderBySeatRowAscSeatNumberAsc(hallId);
        Map<Character, List<Seat>> seatsByRow = new LinkedHashMap<>();
        for (Seat seat : existingSeats) {
            String row = seat.getSeatRow();
            if (row == null || row.trim().length() != 1) {
                throw new SeatValidationException("seat.aisle.invalid");
            }
            char label = Character.toUpperCase(row.trim().charAt(0));
            if (label > after) {
                seatsByRow.computeIfAbsent(label, ignored -> new ArrayList<>()).add(seat);
            }
        }

        List<Character> rowLabels = new ArrayList<>(seatsByRow.keySet());
        rowLabels.sort(Comparator.reverseOrder());
        for (Character label : rowLabels) {
            char next = (char) (label + 1);
            if (next > 'Z') {
                throw new SeatValidationException("seat.aisle.invalid");
            }
            for (Seat seat : seatsByRow.get(label)) {
                seat.setSeatRow(String.valueOf(next));
            }
            seatRepository.saveAllAndFlush(seatsByRow.get(label));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public SeatListItem findById(Integer seatId, User actor) {
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new SeatNotFoundException(seatId));

        User current = loadActor(actor);
        assertCanManageHall(current, seat.getHall());

        return toSeatListItem(seat);
    }

    @Override
    public SeatListItem update(Integer seatId, SeatUpdateRequest request, User actor) {
        // 1. Load seat
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new SeatNotFoundException(seatId));

        // 2. Auth check
        User current = loadActor(actor);
        assertCanManageHall(current, seat.getHall());

        // 3. Validate fields
        String seatRow = request.getSeatRow();
        if (seatRow == null || seatRow.isBlank()) {
            throw new SeatValidationException("seat.row.required");
        }
        if (seatRow.length() > 5) {
            throw new SeatValidationException("seat.row.too_long");
        }

        Integer seatNumber = request.getSeatNumber();
        if (seatNumber == null || seatNumber < 1) {
            throw new SeatValidationException("seat.number.invalid");
        }

        String type = request.getType();
        if (type == null || type.isBlank() || !isValidSeatType(type)) {
            throw new SeatValidationException("seat.type.invalid");
        }

        String maintenanceStatus = request.getMaintenanceStatus();
        if (maintenanceStatus == null || maintenanceStatus.isBlank() || !isValidMaintenanceStatus(maintenanceStatus)) {
            throw new SeatValidationException("seat.maintenance_status.invalid");
        }

        // 4. Check duplicate excluding self
        Hall hall = seat.getHall();
        if (seatRepository.existsByHall_HallIdAndSeatRowAndSeatNumberAndSeatIdNot(
                hall.getHallId(), seatRow, seatNumber, seatId)) {
            throw new SeatValidationException("seat.duplicate");
        }

        // 5. Update fields
        seat.setSeatRow(seatRow);
        seat.setSeatNumber(seatNumber);
        seat.setType(type.toUpperCase());
        seat.setMaintenanceStatus(maintenanceStatus.toUpperCase());

        // 6. Persist
        Seat saved = seatRepository.save(seat);

        // 7. Return DTO
        return toSeatListItem(saved);
    }

    @Override
    public void delete(Integer seatId, User actor) {
        // 1. Load seat
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new SeatNotFoundException(seatId));

        // 2. Auth check
        User current = loadActor(actor);
        assertCanManageHall(current, seat.getHall());

        // 3. Check active references
        if (hasActiveReference(seatId)) {
            throw new SeatValidationException("seat.has_active_reference");
        }

        // 4. Delete
        seatRepository.delete(seat);
    }

    @Override
    public SeatListItem toggleMaintenance(Integer seatId, String newStatus, User actor) {
        // 1. Load seat
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new SeatNotFoundException(seatId));

        // 2. Auth check
        User current = loadActor(actor);
        assertCanManageHall(current, seat.getHall());

        // 3. Validate newStatus
        if (newStatus == null || newStatus.isBlank() || !isValidMaintenanceStatus(newStatus)) {
            throw new SeatValidationException("seat.maintenance_status.invalid");
        }

        // 4. Update maintenance status
        seat.setMaintenanceStatus(newStatus.toUpperCase());

        // 5. Persist
        Seat saved = seatRepository.save(seat);

        // 6. Return DTO
        return toSeatListItem(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public SeatManagementContext managementContext(Integer hallId, User actor) {
        User current = loadActor(actor);
        Hall hall = resolveHall(hallId);
        assertCanManageHall(current, hall);

        Branch branch = hall.getBranch();
        return SeatManagementContext.builder()
                .admin(isAdmin(current))
                .sidebar(sidebarFor(current))
                .hallId(hall.getHallId())
                .hallName(hall.getName())
                .branchName(branch == null ? "" : branch.getName())
                .build();
    }

    // ── Internal helpers ────────────────────────────────────────────────────────

    private User loadActor(User actor) {
        if (actor == null || actor.getUserId() == null) {
            throw new SeatValidationException("seat.access.denied");
        }
        return userRepository.findById(actor.getUserId())
                .orElseThrow(() -> new SeatValidationException("seat.access.denied"));
    }

    private void assertCanManageHall(User actor, Hall hall) {
        if (isAdmin(actor)) {
            return;
        }
        Integer hallBranchId = hallBranchId(hall);
        Integer actorBranchId = actorBranchId(actor);
        if (actorBranchId == null || !actorBranchId.equals(hallBranchId)) {
            throw new SeatValidationException("seat.access.denied");
        }
    }

    private Hall resolveHall(Integer hallId) {
        return hallRepository.findById(hallId)
                .orElseThrow(() -> new SeatNotFoundException(hallId));
    }

    private SeatListItem toSeatListItem(Seat seat) {
        boolean hasActive = hasActiveReference(seat.getSeatId());
        return SeatListItem.builder()
                .seatId(seat.getSeatId())
                .seatRow(seat.getSeatRow())
                .seatNumber(seat.getSeatNumber())
                .type(seat.getType())
                .maintenanceStatus(seat.getMaintenanceStatus())
                .hasActiveReference(hasActive)
                .build();
    }

    private boolean hasActiveReference(Integer seatId) {
        boolean hasTicket = ticketRepository.existsBySeat_SeatIdAndStatusNot(seatId, "CANCELLED");
        if (hasTicket) {
            return true;
        }
        return seatReservationRepository.existsBySeat_SeatIdAndStatusAndExpiredAtAfter(
                seatId, "ACTIVE", LocalDateTime.now());
    }

    private static Integer hallBranchId(Hall hall) {
        Branch branch = hall.getBranch();
        return branch == null ? null : branch.getBranchId();
    }

    private static Integer actorBranchId(User actor) {
        Branch branch = actor.getBranch();
        return branch == null ? null : branch.getBranchId();
    }

    private static boolean isAdmin(User user) {
        return isRole(user, "Admin") || isRole(user, "Administrator");
    }

    private static boolean isManager(User user) {
        return isRole(user, "Manager");
    }

    private static boolean isBranchManager(User user) {
        return isRole(user, "BranchManager") || isRole(user, "Branch Manager") || isRole(user, "Branch_Manager");
    }

    private static boolean isRole(User user, String expected) {
        Role role = user.getRole();
        return role != null && normalizeRoleName(expected).equals(normalizeRoleName(role.getName()));
    }

    private static String normalizeRoleName(String roleName) {
        if (roleName == null) {
            return "";
        }
        String normalized = roleName.trim();
        if (normalized.regionMatches(true, 0, "ROLE_", 0, 5)) {
            normalized = normalized.substring(5);
        }
        return normalized.replaceAll("[\\s_]+", "").toUpperCase();
    }

    private static String sidebarFor(User user) {
        if (isAdmin(user)) {
            return "admin";
        }
        if (isManager(user) || isBranchManager(user)) {
            return "manager";
        }
        return "branch";
    }

    private static boolean isValidSeatType(String type) {
        try {
            SeatType.valueOf(type.toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static boolean isValidMaintenanceStatus(String status) {
        try {
            MaintenanceStatus.valueOf(status.toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static String seatTypeOrDefault(String type) {
        if (type == null || type.isBlank()) {
            return SeatType.STANDARD.name();
        }
        if (!isValidSeatType(type)) {
            throw new SeatValidationException("seat.type.invalid");
        }
        return type.toUpperCase();
    }

    private static String nextRowLabel(List<Seat> existingSeats) {
        char maxRow = 0;
        for (Seat seat : existingSeats) {
            String row = seat.getSeatRow();
            if (row == null || row.trim().length() != 1) {
                throw new SeatValidationException("seat.row.invalid");
            }
            char label = Character.toUpperCase(row.trim().charAt(0));
            if (label < 'A' || label > 'Z') {
                throw new SeatValidationException("seat.row.invalid");
            }
            if (label > maxRow) {
                maxRow = label;
            }
        }
        if (maxRow == 0) {
            return "A";
        }
        if (maxRow >= 'Z') {
            throw new SeatValidationException("seat.row.too_long");
        }
        return String.valueOf((char) (maxRow + 1));
    }

    private static List<Integer> parsePositiveIntegerList(String value) {
        List<Integer> result = new ArrayList<>();
        if (value == null || value.isBlank()) {
            return result;
        }
        String[] parts = value.split(",");
        Set<Integer> seen = new HashSet<>();
        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            try {
                int number = Integer.parseInt(trimmed);
                if (number < 1 || !seen.add(number)) {
                    throw new SeatValidationException("seat.aisle.invalid");
                }
                result.add(number);
            } catch (NumberFormatException ex) {
                throw new SeatValidationException("seat.aisle.invalid");
            }
        }
        result.sort(Integer::compareTo);
        return result;
    }

    private static int displaySeatNumber(int logicalSeatNumber, List<Integer> columnAislesAfter) {
        int offset = 0;
        for (Integer aisleAfter : columnAislesAfter) {
            if (logicalSeatNumber > aisleAfter) {
                offset++;
            }
        }
        return logicalSeatNumber + offset;
    }
}
