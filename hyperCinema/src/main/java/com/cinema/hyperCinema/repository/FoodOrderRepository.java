package com.cinema.hyperCinema.repository;

import com.cinema.hyperCinema.model.FoodOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FoodOrderRepository extends JpaRepository<FoodOrder, Integer> {

    List<FoodOrder> findByBooking_BookingId(Integer bookingId);

    List<FoodOrder> findByBookingIsNullAndBranch_BranchIdOrderByCreatedAtDesc(Integer branchId);
}
