package com.cinema.hyperCinema.repository;

import com.cinema.hyperCinema.model.Feedback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Integer>, JpaSpecificationExecutor<Feedback> {
    Page<Feedback> findAllByOrderByCreatedAtDesc(Pageable pageable);
    List<Feedback> findByUser_UserIdOrderByCreatedAtDesc(Integer userId);
}
