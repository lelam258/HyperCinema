package com.cinema.hyperCinema.repository;

import com.cinema.hyperCinema.model.PromotionUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PromotionUsageRepository extends JpaRepository<PromotionUsage, Integer> {
}
