package com.fleebug.corerouter.repository.model;

import com.fleebug.corerouter.model.model.ModelStatusAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ModelStatusAuditRepository extends JpaRepository<ModelStatusAudit, Long> {

    List<ModelStatusAudit> findByModelModelIdOrderByChangedAtDesc(Integer modelId);

    List<ModelStatusAudit> findByChangedByOrderByChangedAtDesc(String changedBy);
}
