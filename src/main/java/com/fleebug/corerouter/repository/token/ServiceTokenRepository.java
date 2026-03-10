package com.fleebug.corerouter.repository.token;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.fleebug.corerouter.entity.token.ServiceToken;
import com.fleebug.corerouter.enums.token.ServiceRole;

@Repository
public interface ServiceTokenRepository extends JpaRepository<ServiceToken, Long> {

    Optional<ServiceToken> findByTokenId(String tokenId);

    Optional<ServiceToken> findByName(String name);

    List<ServiceToken> findByRole(ServiceRole role);

    List<ServiceToken> findByActiveTrue();

    List<ServiceToken> findByRoleAndActiveTrue(ServiceRole role);

    boolean existsByName(String name);
}
