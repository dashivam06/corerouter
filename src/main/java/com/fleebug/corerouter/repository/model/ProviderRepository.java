package com.fleebug.corerouter.repository.model;

import com.fleebug.corerouter.entity.model.Provider;
import com.fleebug.corerouter.enums.model.ProviderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProviderRepository extends JpaRepository<Provider, Integer> {
    Optional<Provider> findByProviderName(String providerName);

    Optional<Provider> findByCompanyName(String companyName);

    List<Provider> findByStatus(ProviderStatus status);

    @Query("SELECT p FROM Provider p WHERE p.status = :status AND p.providerCountry = :country")
    List<Provider> findByStatusAndCountry(@Param("status") ProviderStatus status, @Param("country") String country);

    boolean existsByProviderName(String providerName);
}
