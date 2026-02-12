package com.fleebug.corerouter.repository.apikey;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fleebug.corerouter.enums.apikey.ApiKeyStatus;
import com.fleebug.corerouter.model.apikey.ApiKey;

public interface ApiKeyRepository extends JpaRepository<ApiKey, Integer> {

    Optional<ApiKey> findByKey(String key);

    boolean existsByKey(String key);

    List<ApiKey> findByUserUserId(Integer userId);

    List<ApiKey> findByStatus(ApiKeyStatus status);

    List<ApiKey> findByUserUserIdAndStatus(Integer userId, ApiKeyStatus status);

    List<ApiKey> findByLastUsedAtBefore(LocalDateTime time);

}
