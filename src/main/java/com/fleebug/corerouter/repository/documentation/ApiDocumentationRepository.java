package com.fleebug.corerouter.repository.documentation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.fleebug.corerouter.entity.documentation.*;

import java.util.List;

@Repository
public interface ApiDocumentationRepository extends JpaRepository<ApiDocumentation, Integer> {

    List<ApiDocumentation> findByModel_ModelId(Integer modelId);

    List<ApiDocumentation> findByTitleContainingIgnoreCase(String keyword);
}
