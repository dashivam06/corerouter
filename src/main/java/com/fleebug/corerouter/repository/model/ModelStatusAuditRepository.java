package com.fleebug.corerouter.repository.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.fleebug.corerouter.entity.model.ModelStatusAudit;

import java.util.List;

@Repository
public interface ModelStatusAuditRepository extends JpaRepository<ModelStatusAudit, Long> {

    List<ModelStatusAudit> findByModelModelIdOrderByChangedAtDesc(Integer modelId);

    List<ModelStatusAudit> findByChangedByOrderByChangedAtDesc(String changedBy);
}
