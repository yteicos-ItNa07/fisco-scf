package com.scf.repository;

import com.scf.entity.Enterprise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EnterpriseRepository extends JpaRepository<Enterprise, Long> {

    Optional<Enterprise> findByCreditCode(String creditCode);

    Optional<Enterprise> findByBlockchainAddress(String blockchainAddress);

    List<Enterprise> findByRole(Enterprise.EnterpriseRole role);

    List<Enterprise> findByIsActiveTrue();

    boolean existsByCreditCode(String creditCode);
}
