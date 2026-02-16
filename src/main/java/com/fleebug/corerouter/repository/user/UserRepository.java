package com.fleebug.corerouter.repository.user;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.fleebug.corerouter.enums.user.UserStatus;
import com.fleebug.corerouter.model.user.User;

public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);



    List<User> findByStatus(UserStatus status);
    
}
