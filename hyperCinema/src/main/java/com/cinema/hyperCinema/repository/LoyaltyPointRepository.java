package com.cinema.hyperCinema.repository;

import com.cinema.hyperCinema.model.LoyaltyPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface LoyaltyPointRepository extends JpaRepository<LoyaltyPoint, Integer> {
    // COALESCE nếu như SUM(lp.points) có giá trị thì sẽ lấy giá trị đó còn ngược lại là lấy 0
    @Query("SELECT COALESCE(SUM(lp.points), 0) FROM LoyaltyPoint lp WHERE lp.user.userId = :userId")
    Long sumPointsByUserId(@Param("userId") Integer userId);

    boolean existsByUser_UserIdAndType(Integer userId, String type);
}
