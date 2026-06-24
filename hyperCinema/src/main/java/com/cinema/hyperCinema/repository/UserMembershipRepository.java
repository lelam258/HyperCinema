package com.cinema.hyperCinema.repository;

import com.cinema.hyperCinema.model.UserMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface UserMembershipRepository extends JpaRepository<UserMembership, Integer> {
    Optional<UserMembership> findFirstByUser_UserIdAndStatusAndEndDateGreaterThanEqualOrderByEndDateDesc(
            Integer userId, String status, LocalDate today);
}
