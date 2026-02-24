package com.fleebug.corerouter.repository.request;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.fleebug.corerouter.entity.apikey.ApiKey;
import com.fleebug.corerouter.entity.request.Request;

import java.util.List;

@Repository
public interface RequestRepository extends JpaRepository<Request, Integer> {

    List<Request> findByApiKey(ApiKey apiKey);

    List<Request> findByStatus(String status);

    List<Request> findByTotalTokensUsedGreaterThan(Integer totalTokens);

    List<Request> findByModelId(Integer modelId);
}
