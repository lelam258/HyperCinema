package com.cinema.hyperCinema.repository;

import com.cinema.hyperCinema.model.UserMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserMembershipRepository extends JpaRepository<UserMembership, Integer> {
}
