package com.cinema.hyperCinema.repository;

import com.cinema.hyperCinema.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Integer> {

    boolean existsByEntityTypeAndEntityIdAndActionAndDetailsLike(
            String entityType, Integer entityId, String action, String detailsPattern);

    /**
     * N audit log gần nhất.
     * Returns: [createdAt, username, action, entityType, details]
     */
    @Query("SELECT a.createdAt, a.user.fullName, a.action, a.entityType, a.details "
            + "FROM AuditLog a ORDER BY a.createdAt DESC LIMIT :limit")
    List<Object[]> findRecentLogs(@Param("limit") int limit);
}
