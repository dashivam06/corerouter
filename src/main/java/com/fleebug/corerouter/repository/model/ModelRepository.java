package com.fleebug.corerouter.repository.model;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fleebug.corerouter.enums.model.ModelStatus;
import com.fleebug.corerouter.model.model.Model;

import java.util.List;

public interface ModelRepository extends JpaRepository<Model, Integer> {

    Optional<Model> findByFullname(String fullname);

    boolean existsByFullname(String fullname);

    List<Model> findByStatus(ModelStatus status);

    List<Model> findByType(String type);

    List<Model> findByStatusAndType(ModelStatus status, String type);

}
