package com.scf.repository;

import com.scf.entity.Enterprise;
import com.scf.entity.Receivable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReceivableRepository extends JpaRepository<Receivable, Long> {

    Optional<Receivable> findByTokenId(Long tokenId);

    List<Receivable> findByHolder(Enterprise holder);

    List<Receivable> findByIssuer(Enterprise issuer);

    List<Receivable> findByStatus(Receivable.TokenStatus status);

    List<Receivable> findByStatusIn(List<Receivable.TokenStatus> statuses);

    List<Receivable> findByHolderAndStatus(Enterprise holder, Receivable.TokenStatus status);

    @Query("SELECT r FROM Receivable r WHERE r.maturityDate <= :date AND r.status = 'CONFIRMED'")
    List<Receivable> findNearMaturity(LocalDate date);

    List<Receivable> findByParentTokenId(Long parentTokenId);

    Optional<Receivable> findByDescription(String description);
}
