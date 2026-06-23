package com.cinema.hyperCinema.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.cinema.hyperCinema.dto.admin.seat.response.SeatListItem;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Helper bean for Thymeleaf templates to group seats by row.
 * Used in seat-map.html via {@code @seatGroupHelper.groupByRow(seats)}.
 */
@Component
public class SeatGroupHelper {

    /**
     * Groups a list of SeatListItem by seatRow, preserving insertion order.
     * Assumes the input list is already sorted by seatRow then seatNumber.
     */
    public Map<String, List<SeatListItem>> groupByRow(List<SeatListItem> seats) {
        if (seats == null || seats.isEmpty()) {
            return new LinkedHashMap<>();
        }
        return seats.stream()
                .collect(Collectors.groupingBy(
                        SeatListItem::getSeatRow,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
    }

    public List<RowGroup> groupRows(List<SeatListItem> seats) {
        Map<String, List<SeatListItem>> grouped = groupByRow(seats);
        Set<Integer> occupiedNumbers = seats == null
                ? Set.of()
                : seats.stream()
                        .map(SeatListItem::getSeatNumber)
                        .filter(number -> number != null && number > 0)
                        .collect(Collectors.toCollection(HashSet::new));
        int maxSeatNumber = occupiedNumbers.stream().mapToInt(Integer::intValue).max().orElse(0);
        Set<Integer> aisleNumbers = new HashSet<>();
        for (int number = 1; number <= maxSeatNumber; number++) {
            if (!occupiedNumbers.contains(number)) {
                aisleNumbers.add(number);
            }
        }

        List<RowGroup> rows = new ArrayList<>();
        String previous = null;
        for (Map.Entry<String, List<SeatListItem>> entry : grouped.entrySet()) {
            rows.add(new RowGroup(
                    entry.getKey(),
                    entry.getValue(),
                    buildCells(entry.getValue(), maxSeatNumber, aisleNumbers),
                    hasRowGap(previous, entry.getKey())
            ));
            previous = entry.getKey();
        }
        return rows;
    }

    private List<SeatCell> buildCells(List<SeatListItem> rowSeats, int maxSeatNumber, Set<Integer> aisleNumbers) {
        Map<Integer, SeatListItem> seatsByNumber = new HashMap<>();
        if (rowSeats != null) {
            for (SeatListItem seat : rowSeats) {
                if (seat.getSeatNumber() != null && seat.getSeatNumber() > 0) {
                    seatsByNumber.put(seat.getSeatNumber(), seat);
                }
            }
        }

        List<SeatCell> cells = new ArrayList<>();
        for (int number = 1; number <= maxSeatNumber; number++) {
            SeatListItem seat = seatsByNumber.get(number);
            if (seat != null) {
                cells.add(SeatCell.seat(number, seat));
            } else if (aisleNumbers.contains(number)) {
                cells.add(SeatCell.aisle(number));
            } else {
                cells.add(SeatCell.empty(number));
            }
        }
        return cells;
    }

    public boolean hasColumnGapAfter(List<SeatListItem> seats, int index) {
        if (seats == null || index < 0 || index >= seats.size() - 1) {
            return false;
        }
        Integer current = seats.get(index).getSeatNumber();
        Integer next = seats.get(index + 1).getSeatNumber();
        return current != null && next != null && next - current > 1;
    }

    private boolean hasRowGap(String previous, String current) {
        if (previous == null || current == null || previous.length() != 1 || current.length() != 1) {
            return false;
        }
        char prev = Character.toUpperCase(previous.charAt(0));
        char curr = Character.toUpperCase(current.charAt(0));
        return curr - prev > 1;
    }

    @Getter
    @AllArgsConstructor
    public static class RowGroup {
        private String label;
        private List<SeatListItem> seats;
        private List<SeatCell> cells;
        private boolean gapBefore;
    }

    @Getter
    @AllArgsConstructor
    public static class SeatCell {
        private Integer number;
        private SeatListItem seat;
        private boolean placeholder;
        private boolean aisle;

        static SeatCell seat(Integer number, SeatListItem seat) {
            return new SeatCell(number, seat, false, false);
        }

        static SeatCell empty(Integer number) {
            return new SeatCell(number, null, true, false);
        }

        static SeatCell aisle(Integer number) {
            return new SeatCell(number, null, false, true);
        }
    }
}
