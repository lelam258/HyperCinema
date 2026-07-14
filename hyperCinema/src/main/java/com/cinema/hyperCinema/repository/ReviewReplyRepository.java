package com.cinema.hyperCinema.repository;

import com.cinema.hyperCinema.model.ReviewReply;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewReplyRepository extends JpaRepository<ReviewReply, Integer> {
}
