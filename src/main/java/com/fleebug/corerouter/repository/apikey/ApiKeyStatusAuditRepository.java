package com.fleebug.corerouter.repository.apikey;

import com.fleebug.corerouter.entity.apikey.ApiKeyStatusAudit;
import com.fleebug.corerouter.enums.apikey.ApiKeyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ApiKeyStatusAuditRepository extends JpaRepository<ApiKeyStatusAudit, Long> {

    long countByNewStatusAndChangedAtBetween(ApiKeyStatus newStatus, LocalDateTime from, LocalDateTime to);

    @Query("SELECT FUNCTION('DATE', a.changedAt), COUNT(a) " +
            "FROM ApiKeyStatusAudit a " +
            "WHERE a.newStatus = :newStatus AND a.changedAt BETWEEN :from AND :to " +
            "GROUP BY FUNCTION('DATE', a.changedAt) " +
            "ORDER BY FUNCTION('DATE', a.changedAt)")
    List<Object[]> countByNewStatusPerDayBetween(
            @Param("newStatus") ApiKeyStatus newStatus,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);
}
