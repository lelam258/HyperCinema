package com.cinema.hyperCinema.repository;

import com.cinema.hyperCinema.model.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Integer> {
    /** Tìm voucher theo Code (không phân biệt hoa thường). */
    Optional<Promotion> findByCodeIgnoreCase(String code);
}
