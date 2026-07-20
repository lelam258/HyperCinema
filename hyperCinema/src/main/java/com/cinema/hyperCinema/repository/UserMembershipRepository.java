package com.cinema.hyperCinema.repository;

import com.cinema.hyperCinema.model.UserMembership;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    @Query("""
            SELECT um FROM UserMembership um
            JOIN FETCH um.plan
            WHERE um.user.userId = :userId
              AND UPPER(um.status) = UPPER(:status)
            ORDER BY um.userMembershipId DESC
            """)
    List<UserMembership> findActiveByUserIdWithPlan(@Param("userId") Integer userId,
                                                    @Param("status") String status,
                                                    @Param("today") LocalDate today);

    @Query("""
            SELECT um FROM UserMembership um
            JOIN FETCH um.user
            JOIN FETCH um.plan
            WHERE UPPER(um.status) = UPPER(:status)
            """)
    List<UserMembership> findActiveMemberships(@Param("status") String status, @Param("today") LocalDate today);

    @Query("""
            SELECT um FROM UserMembership um
            JOIN um.user u
            JOIN u.role
            JOIN um.plan p
            WHERE (:keyword IS NULL
                   OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(u.phone) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:planId IS NULL OR p.planId = :planId)
              AND (:status IS NULL OR UPPER(um.status) = UPPER(:status))
            """)
    Page<UserMembership> searchMemberships(@Param("keyword") String keyword,
                                           @Param("planId") Integer planId,
                                           @Param("status") String status,
                                           Pageable pageable);

    @Query("""
            SELECT um FROM UserMembership um
            JOIN FETCH um.user u
            JOIN FETCH u.role
            JOIN FETCH um.plan p
            WHERE um.userMembershipId = :membershipId
            """)
    Optional<UserMembership> findByIdWithUserAndPlan(@Param("membershipId") Integer membershipId);

    @Query("""
            SELECT COUNT(um) > 0 FROM UserMembership um
            WHERE um.user.userId = :userId
              AND UPPER(um.status) = 'ACTIVE'
              AND (:excludeMembershipId IS NULL OR um.userMembershipId <> :excludeMembershipId)
            """)
    boolean existsOtherActiveForUser(@Param("userId") Integer userId,
                                     @Param("excludeMembershipId") Integer excludeMembershipId);

    boolean existsByPlan_PlanId(Integer planId);

    long countByPlan_PlanIdAndStatusIgnoreCase(Integer planId, String status);
}
