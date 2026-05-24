package com.cinema.hyperCinema.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.cinema.hyperCinema.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
}
