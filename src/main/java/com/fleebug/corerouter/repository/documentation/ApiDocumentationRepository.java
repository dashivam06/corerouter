package com.fleebug.corerouter.repository.documentation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.fleebug.corerouter.entity.documentation.*;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApiDocumentationRepository extends JpaRepository<ApiDocumentation, Integer> {

    List<ApiDocumentation> findByModel_ModelId(Integer modelId);

    List<ApiDocumentation> findByModel_ModelIdAndActiveTrue(Integer modelId);

    List<ApiDocumentation> findByTitleContainingIgnoreCase(String keyword);

    List<ApiDocumentation> findByTitleContainingIgnoreCaseAndActiveTrue(String keyword);

    Optional<ApiDocumentation> findByDocIdAndActiveTrue(Integer docId);
}
