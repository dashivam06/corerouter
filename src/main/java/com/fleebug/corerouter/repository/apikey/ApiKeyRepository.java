package com.fleebug.corerouter.repository.apikey;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.fleebug.corerouter.entity.apikey.ApiKey;
import com.fleebug.corerouter.enums.apikey.ApiKeyStatus;

public interface ApiKeyRepository extends JpaRepository<ApiKey, Integer> {

    Optional<ApiKey> findByKey(String key);

    boolean existsByKey(String key);

    List<ApiKey> findByUserUserId(Integer userId);

    List<ApiKey> findByUserUserIdAndStatusNot(Integer userId, ApiKeyStatus status);

    List<ApiKey> findByStatus(ApiKeyStatus status);

    long countByStatus(ApiKeyStatus status);

    long countByUserUserIdAndStatus(Integer userId, ApiKeyStatus status);

    List<ApiKey> findByUserUserIdAndStatus(Integer userId, ApiKeyStatus status);

    List<ApiKey> findByLastUsedAtBefore(LocalDateTime time);

        Page<ApiKey> findByStatus(ApiKeyStatus status, Pageable pageable);

        long countByCreatedAtBetween(LocalDateTime from, LocalDateTime to);

        @Query("SELECT FUNCTION('DATE', a.createdAt), COUNT(a) " +
            "FROM ApiKey a " +
            "WHERE a.createdAt BETWEEN :from AND :to " +
            "GROUP BY FUNCTION('DATE', a.createdAt) " +
            "ORDER BY FUNCTION('DATE', a.createdAt)")
        List<Object[]> countCreatedPerDayBetween(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

}
