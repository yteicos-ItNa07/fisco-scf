package com.scf.repository;

import com.scf.entity.FinancingRecord;
import com.scf.entity.Enterprise;
import com.scf.entity.Receivable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FinancingRecordRepository extends JpaRepository<FinancingRecord, Long> {

    Optional<FinancingRecord> findByRecordId(Long recordId);

    List<FinancingRecord> findByReceivable(Receivable receivable);

    List<FinancingRecord> findByBorrower(Enterprise borrower);

    List<FinancingRecord> findByFinancier(Enterprise financier);

    List<FinancingRecord> findByStatus(FinancingRecord.FinancingStatus status);

    List<FinancingRecord> findByFinancierAndStatus(Enterprise financier, FinancingRecord.FinancingStatus status);

    List<FinancingRecord> findByIsRepaidFalse();

    List<FinancingRecord> findByReceivable_Issuer(Enterprise issuer);
}
