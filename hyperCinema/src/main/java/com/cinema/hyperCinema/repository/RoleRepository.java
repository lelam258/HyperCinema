package com.cinema.hyperCinema.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.cinema.hyperCinema.model.Role;

@Repository
public interface RoleRepository extends JpaRepository<Role, Integer> {
}
