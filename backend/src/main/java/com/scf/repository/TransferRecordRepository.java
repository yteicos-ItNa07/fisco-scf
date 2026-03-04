package com.scf.repository;

import com.scf.entity.TransferRecord;
import com.scf.entity.Enterprise;
import com.scf.entity.Receivable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransferRecordRepository extends JpaRepository<TransferRecord, Long> {

    List<TransferRecord> findByFromReceivable(Receivable receivable);

    List<TransferRecord> findByToReceivable(Receivable receivable);

    List<TransferRecord> findByFromEnterprise(Enterprise enterprise);

    List<TransferRecord> findByToEnterprise(Enterprise enterprise);

    List<TransferRecord> findByType(TransferRecord.TransferType type);
}
