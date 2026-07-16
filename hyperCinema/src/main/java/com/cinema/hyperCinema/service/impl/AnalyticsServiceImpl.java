package com.cinema.hyperCinema.service.impl;

import com.cinema.hyperCinema.model.*;
import com.cinema.hyperCinema.service.AnalyticsService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AnalyticsServiceImpl implements AnalyticsService {

    @PersistenceContext
    private EntityManager em;

    @Override
    public Map<String, Object> getTicketSalesReport(LocalDate from, LocalDate to, Integer branchId) {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.atTime(LocalTime.MAX);

        // Current Period Data
        List<Payment> currentPayments = getPayments(start, end, branchId);
        List<Booking> currentBookings = getBookings(start, end, branchId);
        long totalTickets = getTicketsCount(currentBookings);
        long totalRevenue = currentPayments.stream().mapToLong(Payment::getAmount).sum();
        double avgPrice = totalTickets > 0 ? (double) totalRevenue / totalTickets : 0.0;
        long activeBranches = getActiveBranchesCount();

        // Previous Period Data (for growth indicators)
        long daysBetween = ChronoUnit.DAYS.between(from, to);
        LocalDateTime prevStart = from.minusDays(daysBetween + 1).atStartOfDay();
        LocalDateTime prevEnd = from.minusDays(1).atTime(LocalTime.MAX);

        List<Payment> prevPayments = getPayments(prevStart, prevEnd, branchId);
        List<Booking> prevBookings = getBookings(prevStart, prevEnd, branchId);
        long prevTotalTickets = getTicketsCount(prevBookings);
        long prevTotalRevenue = prevPayments.stream().mapToLong(Payment::getAmount).sum();
        double prevAvgPrice = prevTotalTickets > 0 ? (double) prevTotalRevenue / prevTotalTickets : 0.0;

        // Growth percentages
        double revenueGrowth = calculateGrowth(totalRevenue, prevTotalRevenue);
        double ticketGrowth = calculateGrowth(totalTickets, prevTotalTickets);
        double avgPriceGrowth = calculateGrowth(avgPrice, prevAvgPrice);

        // Timeline Data: day by day
        Map<String, Map<String, Long>> timeline = new LinkedHashMap<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM dd", Locale.ENGLISH);
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            String label = d.format(fmt);
            Map<String, Long> dayData = new HashMap<>();
            dayData.put("revenue", 0L);
            dayData.put("tickets", 0L);
            timeline.put(label, dayData);
        }

        for (Payment p : currentPayments) {
            String label = p.getCreatedAt().toLocalDate().format(fmt);
            if (timeline.containsKey(label)) {
                timeline.get(label).put("revenue", timeline.get(label).get("revenue") + p.getAmount());
            }
        }
        for (Booking b : currentBookings) {
            if ("Completed".equals(b.getStatus())) {
                String label = b.getCreatedAt().toLocalDate().format(fmt);
                if (timeline.containsKey(label)) {
                    long tickets = b.getTickets() != null ? b.getTickets().size() : 0;
                    timeline.get(label).put("tickets", timeline.get(label).get("tickets") + tickets);
                }
            }
        }

        // Branch Breakdown Table
        // Group by Branch and Date
        Map<String, Map<LocalDate, Map<String, Object>>> branchGroup = new TreeMap<>();
        for (Booking b : currentBookings) {
            if (!"Completed".equals(b.getStatus())) continue;
            String branchName = b.getShowtime().getHall().getBranch().getName();
            LocalDate date = b.getCreatedAt().toLocalDate();

            branchGroup.computeIfAbsent(branchName, k -> new TreeMap<>());
            Map<LocalDate, Map<String, Object>> dateMap = branchGroup.get(branchName);
            dateMap.computeIfAbsent(date, k -> {
                Map<String, Object> data = new HashMap<>();
                data.put("tickets", 0L);
                data.put("revenue", 0L);
                return data;
            });

            Map<String, Object> data = dateMap.get(date);
            long tickets = b.getTickets() != null ? b.getTickets().size() : 0;
            data.put("tickets", (long) data.get("tickets") + tickets);
            data.put("revenue", (long) data.get("revenue") + b.getTotalPrice());
        }

        List<Map<String, Object>> breakdownList = new ArrayList<>();
        DateTimeFormatter tableDateFmt = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.ENGLISH);
        for (Map.Entry<String, Map<LocalDate, Map<String, Object>>> bEntry : branchGroup.entrySet()) {
            String branchName = bEntry.getKey();
            for (Map.Entry<LocalDate, Map<String, Object>> dEntry : bEntry.getValue().entrySet()) {
                LocalDate date = dEntry.getKey();
                Map<String, Object> metrics = dEntry.getValue();
                long tCount = (long) metrics.get("tickets");
                long rev = (long) metrics.get("revenue");
                double avg = tCount > 0 ? (double) rev / tCount : 0.0;

                Map<String, Object> row = new HashMap<>();
                row.put("branch", branchName);
                row.put("rawDate", date);
                row.put("date", date.format(tableDateFmt));
                row.put("tickets", tCount);
                row.put("revenue", rev);
                row.put("avgPrice", avg);
                breakdownList.add(row);
            }
        }
        // Sort table by Date desc, tickets desc
        breakdownList.sort((r1, r2) -> {
            int c = ((LocalDate) r2.get("rawDate")).compareTo((LocalDate) r1.get("rawDate"));
            if (c != 0) return c;
            return Long.compare((long) r2.get("tickets"), (long) r1.get("tickets"));
        });

        Map<String, Object> report = new HashMap<>();
        report.put("totalTickets", totalTickets);
        report.put("totalRevenue", totalRevenue);
        report.put("avgPrice", avgPrice);
        report.put("activeBranches", activeBranches);
        report.put("revenueGrowth", revenueGrowth);
        report.put("ticketGrowth", ticketGrowth);
        report.put("avgPriceGrowth", avgPriceGrowth);
        report.put("timelineLabels", new ArrayList<>(timeline.keySet()));
        report.put("timelineRevenue", timeline.values().stream().map(m -> m.get("revenue")).collect(Collectors.toList()));
        report.put("timelineTickets", timeline.values().stream().map(m -> m.get("tickets")).collect(Collectors.toList()));
        report.put("breakdown", breakdownList);

        return report;
    }

    @Override
    public Map<String, Object> getFoodSalesReport(LocalDate from, LocalDate to, Integer branchId) {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.atTime(LocalTime.MAX);

        // Fetch completed food orders in range
        List<FoodOrder> currentOrders = getFoodOrders(start, end, branchId);
        long totalRevenue = currentOrders.stream().mapToLong(FoodOrder::getTotalAmount).sum();

        // Growth (compare against previous period)
        long daysBetween = ChronoUnit.DAYS.between(from, to);
        LocalDateTime prevStart = from.minusDays(daysBetween + 1).atStartOfDay();
        LocalDateTime prevEnd = from.minusDays(1).atTime(LocalTime.MAX);
        List<FoodOrder> prevOrders = getFoodOrders(prevStart, prevEnd, branchId);
        long prevRevenue = prevOrders.stream().mapToLong(FoodOrder::getTotalAmount).sum();
        double revenueGrowth = calculateGrowth(totalRevenue, prevRevenue);

        // Collect Food Order Items details
        long foodUnits = 0;
        long beverageUnits = 0;

        Map<String, Map<String, Object>> productAgg = new HashMap<>();

        for (FoodOrder order : currentOrders) {
            if (order.getItems() == null) continue;
            for (FoodOrderItem item : order.getItems()) {
                String pName = item.getFoodItem().getName();
                String category = item.getFoodItem().getCategoryName();
                int qty = item.getQuantity();
                long itemTotal = (long) item.getUnitPrice() * qty;

                if ("Food".equalsIgnoreCase(category)) {
                    foodUnits += qty;
                } else if ("Beverage".equalsIgnoreCase(category)) {
                    beverageUnits += qty;
                }

                productAgg.computeIfAbsent(pName, k -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("name", pName);
                    m.put("category", category);
                    m.put("qty", 0L);
                    m.put("price", (long) item.getUnitPrice());
                    m.put("revenue", 0L);
                    return m;
                });

                Map<String, Object> m = productAgg.get(pName);
                m.put("qty", (long) m.get("qty") + qty);
                m.put("revenue", (long) m.get("revenue") + itemTotal);
            }
        }

        List<Map<String, Object>> productDetails = new ArrayList<>(productAgg.values());
        // Sort by revenue desc
        productDetails.sort((p1, p2) -> Long.compare((long) p2.get("revenue"), (long) p1.get("revenue")));

        // Prepare charts
        List<String> productLabels = productDetails.stream().map(m -> (String) m.get("name")).collect(Collectors.toList());
        List<Long> productQuantities = productDetails.stream().map(m -> (Long) m.get("qty")).collect(Collectors.toList());
        List<String> productCategories = productDetails.stream().map(m -> (String) m.get("category")).collect(Collectors.toList());

        long foodRevenue = productDetails.stream().filter(m -> "Food".equalsIgnoreCase((String) m.get("category"))).mapToLong(m -> (long) m.get("revenue")).sum();
        long beverageRevenue = productDetails.stream().filter(m -> "Beverage".equalsIgnoreCase((String) m.get("category"))).mapToLong(m -> (long) m.get("revenue")).sum();
        long comboRevenue = productDetails.stream().filter(m -> "Combo".equalsIgnoreCase((String) m.get("category"))).mapToLong(m -> (long) m.get("revenue")).sum();

        Map<String, Object> report = new HashMap<>();
        report.put("totalRevenue", totalRevenue);
        report.put("revenueGrowth", revenueGrowth);
        report.put("foodUnits", foodUnits);
        report.put("beverageUnits", beverageUnits);
        report.put("productLabels", productLabels);
        report.put("productQuantities", productQuantities);
        report.put("productCategories", productCategories);
        report.put("foodRevenue", foodRevenue);
        report.put("beverageRevenue", beverageRevenue);
        report.put("comboRevenue", comboRevenue);
        report.put("products", productDetails);

        return report;
    }

    @Override
    public Map<String, Object> getHallOccupancyReport(LocalDate from, LocalDate to, Integer branchId) {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.atTime(LocalTime.MAX);

        // Fetch showtimes in range
        List<Showtime> showtimes = getShowtimes(start, end, branchId);
        List<Booking> bookings = getBookings(start, end, branchId);

        // Calculate occupancy metrics
        long totalCapacity = showtimes.stream().mapToLong(st -> st.getHall().getCapacity()).sum();
        long totalTicketsBooked = bookings.stream()
                .filter(b -> "Completed".equals(b.getStatus()))
                .mapToLong(b -> b.getTickets() != null ? b.getTickets().size() : 0)
                .sum();
        double avgOccupancy = totalCapacity > 0 ? ((double) totalTicketsBooked / totalCapacity) * 100.0 : 0.0;

        // Peak Booking Hour
        Map<Integer, Long> hourlyBookings = new HashMap<>();
        for (int h = 9; h <= 22; h++) {
            hourlyBookings.put(h, 0L);
        }
        for (Booking b : bookings) {
            if ("Completed".equals(b.getStatus())) {
                int hour = b.getCreatedAt().getHour();
                if (hour >= 9 && hour <= 22) {
                    long count = b.getTickets() != null ? b.getTickets().size() : 0;
                    hourlyBookings.put(hour, hourlyBookings.get(hour) + count);
                }
            }
        }

        int peakHour = 18; // Default
        long maxTickets = -1;
        for (Map.Entry<Integer, Long> entry : hourlyBookings.entrySet()) {
            if (entry.getValue() > maxTickets) {
                maxTickets = entry.getValue();
                peakHour = entry.getKey();
            }
        }
        String peakHourStr = (peakHour > 12 ? (peakHour - 12) + " PM" : peakHour + " AM");

        // Hourly traffic and capacity levels
        // High (>= 85%), Medium (65-84%), Low (< 65%)
        List<String> hourlyLabels = new ArrayList<>();
        List<Long> hourlyTickets = new ArrayList<>();
        List<String> hourlyLevels = new ArrayList<>(); // color categories

        // To assign colors based on occupancy per hour slot:
        // We aggregate scheduled capacity per hour:
        Map<Integer, Long> hourlyCapacity = new HashMap<>();
        for (int h = 9; h <= 22; h++) {
            hourlyCapacity.put(h, 0L);
        }
        for (Showtime st : showtimes) {
            int hour = st.getStartTime().getHour();
            if (hour >= 9 && hour <= 22) {
                hourlyCapacity.put(hour, hourlyCapacity.get(hour) + st.getHall().getCapacity());
            }
        }

        for (int h = 9; h <= 22; h++) {
            String label = (h > 12 ? (h - 12) + "PM" : h + "AM");
            long tickets = hourlyBookings.get(h);
            long cap = hourlyCapacity.get(h);
            double occ = cap > 0 ? ((double) tickets / cap) * 100.0 : 0.0;

            String level = "low";
            if (occ >= 85) level = "high";
            else if (occ >= 65) level = "medium";

            hourlyLabels.add(label);
            hourlyTickets.add(tickets);
            hourlyLevels.add(level);
        }

        // Hall utilization breakdown
        Map<Integer, Map<String, Object>> hallAgg = new HashMap<>();
        for (Showtime st : showtimes) {
            int hallId = st.getHall().getHallId();
            hallAgg.computeIfAbsent(hallId, k -> {
                Map<String, Object> m = new HashMap<>();
                m.put("hallName", st.getHall().getName());
                m.put("branchName", st.getHall().getBranch().getName());
                m.put("capacity", (long) st.getHall().getCapacity());
                m.put("scheduledCapacity", 0L);
                m.put("tickets", 0L);
                m.put("hourlyTickets", new HashMap<Integer, Long>());
                return m;
            });

            Map<String, Object> m = hallAgg.get(hallId);
            m.put("scheduledCapacity", (long) m.get("scheduledCapacity") + st.getHall().getCapacity());
        }

        for (Booking b : bookings) {
            if (!"Completed".equals(b.getStatus())) continue;
            Showtime st = b.getShowtime();
            int hallId = st.getHall().getHallId();
            Map<String, Object> m = hallAgg.get(hallId);
            if (m != null) {
                long tickets = b.getTickets() != null ? b.getTickets().size() : 0;
                m.put("tickets", (long) m.get("tickets") + tickets);

                int hour = b.getCreatedAt().getHour();
                Map<Integer, Long> hrMap = (Map<Integer, Long>) m.get("hourlyTickets");
                hrMap.put(hour, hrMap.getOrDefault(hour, 0L) + tickets);
            }
        }

        List<Map<String, Object>> hallDetails = new ArrayList<>();
        String mostUsedHallName = "N/A";
        double maxHallOcc = -1.0;

        for (Map<String, Object> hData : hallAgg.values()) {
            long cap = (long) hData.get("scheduledCapacity");
            long tix = (long) hData.get("tickets");
            double occ = cap > 0 ? ((double) tix / cap) * 100.0 : 0.0;
            hData.put("occupancyRate", occ);

            // Find peak hour for this hall
            Map<Integer, Long> hrMap = (Map<Integer, Long>) hData.get("hourlyTickets");
            int hPeak = 18;
            long hPeakTix = -1;
            for (Map.Entry<Integer, Long> entry : hrMap.entrySet()) {
                if (entry.getValue() > hPeakTix) {
                    hPeakTix = entry.getValue();
                    hPeak = entry.getKey();
                }
            }
            String hPeakStr = (hPeak > 12 ? (hPeak - 12) + " PM" : hPeak + " AM");
            hData.put("peakHour", hPeakStr);

            hallDetails.add(hData);

            if (occ > maxHallOcc) {
                maxHallOcc = occ;
                mostUsedHallName = hData.get("hallName") + " (" + hData.get("branchName") + ")";
            }
        }

        // Sort halls by occupancy rate desc
        hallDetails.sort((h1, h2) -> Double.compare((double) h2.get("occupancyRate"), (double) h1.get("occupancyRate")));

        Map<String, Object> report = new HashMap<>();
        report.put("avgOccupancy", avgOccupancy);
        report.put("peakHour", peakHourStr);
        report.put("mostUsedHall", mostUsedHallName + (maxHallOcc >= 0 ? " - " + String.format("%.1f", maxHallOcc) + "% occupancy" : ""));
        report.put("hourlyLabels", hourlyLabels);
        report.put("hourlyTickets", hourlyTickets);
        report.put("hourlyLevels", hourlyLevels);
        report.put("halls", hallDetails);

        return report;
    }

    @Override
    public byte[] exportReport(String reportType, LocalDate from, LocalDate to, Integer branchId, String format) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintWriter pw = new PrintWriter(out);

        if ("csv".equalsIgnoreCase(format) || "excel".equalsIgnoreCase(format)) {
            // Excel exports are generated as CSV for native download compatibility
            if ("ticket".equalsIgnoreCase(reportType)) {
                Map<String, Object> data = getTicketSalesReport(from, to, branchId);
                pw.println("Branch,Date,Tickets Sold,Revenue,Avg Price");
                List<Map<String, Object>> list = (List<Map<String, Object>>) data.get("breakdown");
                for (Map<String, Object> row : list) {
                    pw.println(String.format("\"%s\",\"%s\",%s,%s,%.2f",
                            row.get("branch"), row.get("date"), row.get("tickets"), row.get("revenue"), row.get("avgPrice")));
                }
            } else if ("fb".equalsIgnoreCase(reportType)) {
                Map<String, Object> data = getFoodSalesReport(from, to, branchId);
                pw.println("Product Name,Category,Quantity Sold,Unit Price,Total Revenue");
                List<Map<String, Object>> list = (List<Map<String, Object>>) data.get("products");
                for (Map<String, Object> row : list) {
                    pw.println(String.format("\"%s\",\"%s\",%s,%s,%s",
                            row.get("name"), row.get("category"), row.get("qty"), row.get("price"), row.get("revenue")));
                }
            } else if ("occupancy".equalsIgnoreCase(reportType)) {
                Map<String, Object> data = getHallOccupancyReport(from, to, branchId);
                pw.println("Hall,Branch,Capacity,Seats Used,Occupancy Rate,Peak Hour");
                List<Map<String, Object>> list = (List<Map<String, Object>>) data.get("halls");
                for (Map<String, Object> row : list) {
                    pw.println(String.format("\"%s\",\"%s\",%s,%s,%.2f%%,\"%s\"",
                            row.get("hallName"), row.get("branchName"), row.get("capacity"), row.get("tickets"), row.get("occupancyRate"), row.get("peakHour")));
                }
            }
        } else if ("pdf".equalsIgnoreCase(format)) {
            // PDF export formatted as simple plain text dashboard ready for print
            pw.println("==========================================================");
            pw.println("                 HYPERCINEMA OPERATIONAL REPORT           ");
            pw.println("==========================================================");
            pw.println("Report Type: " + reportType.toUpperCase() + " SALES");
            pw.println("Date Range:  " + from + " to " + to);
            pw.println("Generated At: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            pw.println("----------------------------------------------------------");

            if ("ticket".equalsIgnoreCase(reportType)) {
                Map<String, Object> data = getTicketSalesReport(from, to, branchId);
                pw.println("TOTAL REVENUE:       " + data.get("totalRevenue") + " VND");
                pw.println("TOTAL TICKETS SOLD:  " + data.get("totalTickets"));
                pw.println("AVERAGE PRICE:       " + String.format("%.2f", data.get("avgPrice")) + " VND");
                pw.println("----------------------------------------------------------");
                pw.println(String.format("%-25s %-15s %-10s %-15s", "Branch", "Date", "Tickets", "Revenue"));
                List<Map<String, Object>> list = (List<Map<String, Object>>) data.get("breakdown");
                for (Map<String, Object> row : list) {
                    pw.println(String.format("%-25s %-15s %-10s %-15s",
                            row.get("branch"), row.get("date"), row.get("tickets"), row.get("revenue") + " VND"));
                }
            } else if ("fb".equalsIgnoreCase(reportType)) {
                Map<String, Object> data = getFoodSalesReport(from, to, branchId);
                pw.println("TOTAL F&B REVENUE:   " + data.get("totalRevenue") + " VND");
                pw.println("FOOD UNITS:          " + data.get("foodUnits"));
                pw.println("BEVERAGE UNITS:      " + data.get("beverageUnits"));
                pw.println("----------------------------------------------------------");
                pw.println(String.format("%-25s %-15s %-10s %-15s", "Product Name", "Category", "Qty", "Revenue"));
                List<Map<String, Object>> list = (List<Map<String, Object>>) data.get("products");
                for (Map<String, Object> row : list) {
                    pw.println(String.format("%-25s %-15s %-10s %-15s",
                            row.get("name"), row.get("category"), row.get("qty"), row.get("revenue") + " VND"));
                }
            } else if ("occupancy".equalsIgnoreCase(reportType)) {
                Map<String, Object> data = getHallOccupancyReport(from, to, branchId);
                pw.println("AVG OCCUPANCY:       " + String.format("%.2f%%", data.get("avgOccupancy")));
                pw.println("PEAK BOOKING HOUR:   " + data.get("peakHour"));
                pw.println("MOST USED HALL:      " + data.get("mostUsedHall"));
                pw.println("----------------------------------------------------------");
                pw.println(String.format("%-20s %-20s %-10s %-10s %-10s", "Hall", "Branch", "Capacity", "Used", "Occupancy"));
                List<Map<String, Object>> list = (List<Map<String, Object>>) data.get("halls");
                for (Map<String, Object> row : list) {
                    pw.println(String.format("%-20s %-20s %-10s %-10s %-10.1f%%",
                            row.get("hallName"), row.get("branchName"), row.get("capacity"), row.get("tickets"), row.get("occupancyRate")));
                }
            }
            pw.println("==========================================================");
        }

        pw.flush();
        return out.toByteArray();
    }

    // ───────────────────── PRIVATE QUERY HELPERS ─────────────────────

    private List<Payment> getPayments(LocalDateTime start, LocalDateTime end, Integer branchId) {
        String jpql = "SELECT p FROM Payment p JOIN p.booking bk JOIN bk.showtime s JOIN s.hall h " +
                "WHERE p.status = 'Completed' AND p.createdAt BETWEEN :start AND :end ";
        if (branchId != null) {
            jpql += "AND h.branch.branchId = :branchId ";
        }
        TypedQuery<Payment> q = em.createQuery(jpql, Payment.class);
        q.setParameter("start", start);
        q.setParameter("end", end);
        if (branchId != null) {
            q.setParameter("branchId", branchId);
        }
        return q.getResultList();
    }

    private List<Booking> getBookings(LocalDateTime start, LocalDateTime end, Integer branchId) {
        String jpql = "SELECT DISTINCT bk FROM Booking bk LEFT JOIN FETCH bk.tickets JOIN FETCH bk.showtime s JOIN FETCH s.hall h JOIN FETCH s.movie JOIN FETCH h.branch LEFT JOIN FETCH bk.payment " +
                "WHERE bk.createdAt BETWEEN :start AND :end ";
        if (branchId != null) {
            jpql += "AND h.branch.branchId = :branchId ";
        }
        TypedQuery<Booking> q = em.createQuery(jpql, Booking.class);
        q.setParameter("start", start);
        q.setParameter("end", end);
        if (branchId != null) {
            q.setParameter("branchId", branchId);
        }
        return q.getResultList();
    }

    private List<FoodOrder> getFoodOrders(LocalDateTime start, LocalDateTime end, Integer branchId) {
        String jpql = "SELECT DISTINCT fo FROM FoodOrder fo JOIN FETCH fo.items i JOIN FETCH i.foodItem JOIN fo.booking bk JOIN bk.showtime s JOIN s.hall h " +
                "WHERE fo.status = 'Completed' AND fo.createdAt BETWEEN :start AND :end ";
        if (branchId != null) {
            jpql += "AND h.branch.branchId = :branchId ";
        }
        TypedQuery<FoodOrder> q = em.createQuery(jpql, FoodOrder.class);
        q.setParameter("start", start);
        q.setParameter("end", end);
        if (branchId != null) {
            q.setParameter("branchId", branchId);
        }
        return q.getResultList();
    }

    private List<Showtime> getShowtimes(LocalDateTime start, LocalDateTime end, Integer branchId) {
        String jpql = "SELECT DISTINCT st FROM Showtime st JOIN FETCH st.hall h JOIN FETCH h.branch JOIN FETCH st.movie " +
                "WHERE st.startTime BETWEEN :start AND :end ";
        if (branchId != null) {
            jpql += "AND h.branch.branchId = :branchId ";
        }
        TypedQuery<Showtime> q = em.createQuery(jpql, Showtime.class);
        q.setParameter("start", start);
        q.setParameter("end", end);
        if (branchId != null) {
            q.setParameter("branchId", branchId);
        }
        return q.getResultList();
    }

    private long getTicketsCount(List<Booking> bookings) {
        return bookings.stream()
                .filter(b -> "Completed".equals(b.getStatus()))
                .mapToLong(b -> b.getTickets() != null ? b.getTickets().size() : 0)
                .sum();
    }

    private long getActiveBranchesCount() {
        return em.createQuery("SELECT COUNT(b) FROM Branch b WHERE b.status = 'Active'", Long.class).getSingleResult();
    }

    private double calculateGrowth(double current, double previous) {
        if (previous == 0.0) {
            return current > 0.0 ? 100.0 : 0.0;
        }
        return ((current - previous) / previous) * 100.0;
    }
}
