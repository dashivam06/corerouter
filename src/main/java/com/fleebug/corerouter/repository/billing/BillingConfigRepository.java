package com.fleebug.corerouter.repository.billing;

import com.fleebug.corerouter.entity.billing.BillingConfig;
import com.fleebug.corerouter.entity.model.Model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BillingConfigRepository extends JpaRepository<BillingConfig, Integer> {

    Optional<BillingConfig> findByModel(Model model);

    Optional<BillingConfig> findByModelModelId(Integer modelId);

    boolean existsByModelModelId(Integer modelId);
}
