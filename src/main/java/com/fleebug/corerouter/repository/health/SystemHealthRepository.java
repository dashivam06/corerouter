package com.fleebug.corerouter.repository.health;


import com.fleebug.corerouter.model.health.*;
import com.fleebug.corerouter.model.model.Model;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SystemHealthRepository extends JpaRepository<SystemHealth, Integer> {

    List<SystemHealth> findByModel(Model model);

    SystemHealth findTopByModelOrderByCheckedAtDesc(Model model);

    List<SystemHealth> findByStatus(String status);
}
