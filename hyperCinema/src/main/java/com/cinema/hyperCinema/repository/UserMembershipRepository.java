package com.cinema.hyperCinema.repository;

import com.cinema.hyperCinema.model.UserMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserMembershipRepository extends JpaRepository<UserMembership, Integer> {
    Optional<UserMembership> findFirstByUser_UserIdAndStatusAndEndDateGreaterThanEqualOrderByEndDateDesc(
            Integer userId, String status, LocalDate today);

    @Query("SELECT um FROM UserMembership um JOIN FETCH um.user WHERE um.status = :status AND um.endDate >= :today")
    List<UserMembership> findActiveMemberships(@Param("status") String status, @Param("today") LocalDate today);
}
