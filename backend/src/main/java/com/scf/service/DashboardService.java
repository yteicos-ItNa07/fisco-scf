package com.scf.service;

import com.scf.dto.ResponseDTO.DashboardStats;
import com.scf.entity.Enterprise;
import com.scf.entity.FinancingRecord;
import com.scf.entity.Receivable;
import com.scf.repository.EnterpriseRepository;
import com.scf.repository.FinancingRecordRepository;
import com.scf.repository.ReceivableRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final EnterpriseRepository enterpriseRepository;
    private final ReceivableRepository receivableRepository;
    private final FinancingRecordRepository financingRecordRepository;

    /**
     * 获取全局统计数据（管理员视角）
     */
    public DashboardStats getGlobalStats() {
        List<Enterprise> allEnterprises = enterpriseRepository.findAll();
        List<Receivable> allReceivables = receivableRepository.findAll();
        List<FinancingRecord> allFinancing = financingRecordRepository.findAll();

        long coreEnterprises = allEnterprises.stream()
                .filter(e -> e.getRole() == Enterprise.EnterpriseRole.CORE_ENTERPRISE)
                .count();
        long suppliers = allEnterprises.stream()
                .filter(e -> e.getRole() == Enterprise.EnterpriseRole.SUPPLIER)
                .count();
        long financialInsts = allEnterprises.stream()
                .filter(e -> e.getRole() == Enterprise.EnterpriseRole.FINANCIAL_INST)
                .count();

        long confirmedReceivables = allReceivables.stream()
                .filter(r -> r.getStatus() == Receivable.TokenStatus.CONFIRMED)
                .count();
        long financedReceivables = allReceivables.stream()
                .filter(r -> r.getStatus() == Receivable.TokenStatus.FINANCED ||
                        r.getStatus() == Receivable.TokenStatus.PARTIALLY_FINANCED)
                .count();

        BigDecimal totalReceivableValue = allReceivables.stream()
                .map(Receivable::getFaceValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalFinancedValue = allFinancing.stream()
                .filter(f -> f.getStatus() == FinancingRecord.FinancingStatus.APPROVED ||
                        f.getStatus() == FinancingRecord.FinancingStatus.DISBURSED ||
                        f.getStatus() == FinancingRecord.FinancingStatus.REPAID)
                .map(FinancingRecord::getFinancingAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalDisbursedValue = allFinancing.stream()
                .filter(f -> f.getStatus() == FinancingRecord.FinancingStatus.DISBURSED ||
                        f.getStatus() == FinancingRecord.FinancingStatus.REPAID)
                .map(FinancingRecord::getActualAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long pendingFinancing = allFinancing.stream()
                .filter(f -> f.getStatus() == FinancingRecord.FinancingStatus.PENDING)
                .count();
        long approvedFinancing = allFinancing.stream()
                .filter(f -> f.getStatus() == FinancingRecord.FinancingStatus.APPROVED)
                .count();

        double averageCreditScore = allEnterprises.stream()
                .mapToInt(Enterprise::getCreditScore)
                .average()
                .orElse(0.0);

        double averageFinancingRate = allFinancing.stream()
                .filter(f -> f.getStatus() != FinancingRecord.FinancingStatus.PENDING &&
                        f.getStatus() != FinancingRecord.FinancingStatus.REJECTED)
                .mapToDouble(f -> f.getAnnualRate().doubleValue())
                .average()
                .orElse(0.0);

        return DashboardStats.builder()
                .totalEnterprises(allEnterprises.size())
                .coreEnterprises(coreEnterprises)
                .suppliers(suppliers)
                .financialInsts(financialInsts)
                .activeReceivables(confirmedReceivables + financedReceivables)
                .confirmedReceivables(confirmedReceivables)
                .financedReceivables(financedReceivables)
                .totalReceivableValue(totalReceivableValue)
                .totalFinancedValue(totalFinancedValue)
                .totalDisbursedValue(totalDisbursedValue)
                .pendingFinancing(pendingFinancing)
                .approvedFinancing(approvedFinancing)
                .averageCreditScore(Math.round(averageCreditScore * 10) / 10.0)
                .averageFinancingRate(Math.round(averageFinancingRate * 10000) / 10000.0)
                .build();
    }

    /**
     * 获取企业统计数据
     */
    public DashboardStats getEnterpriseStats(Long enterpriseId) {
        Enterprise enterprise = enterpriseRepository.findById(enterpriseId)
                .orElseThrow(() -> new RuntimeException("企业不存在"));

        List<Receivable> holdingReceivables = receivableRepository.findByHolder(enterprise);
        List<Receivable> issuedReceivables = receivableRepository.findByIssuer(enterprise);
        List<FinancingRecord> borrowingRecords = financingRecordRepository.findByBorrower(enterprise);
        List<FinancingRecord> lendingRecords = financingRecordRepository.findByFinancier(enterprise);

        // 根据企业角色计算不同的统计数据
        List<Receivable> relevantReceivables;
        List<FinancingRecord> relevantFinancing;

        if (enterprise.getRole() == Enterprise.EnterpriseRole.CORE_ENTERPRISE) {
            relevantReceivables = issuedReceivables;
            relevantFinancing = borrowingRecords; // 核心企业通常不借款，但保留
        } else if (enterprise.getRole() == Enterprise.EnterpriseRole.FINANCIAL_INST) {
            relevantReceivables = holdingReceivables;
            relevantFinancing = lendingRecords;
        } else {
            relevantReceivables = holdingReceivables;
            relevantFinancing = borrowingRecords;
        }

        long confirmedReceivables = relevantReceivables.stream()
                .filter(r -> r.getStatus() == Receivable.TokenStatus.CONFIRMED)
                .count();
        long financedReceivables = relevantReceivables.stream()
                .filter(r -> r.getStatus() == Receivable.TokenStatus.FINANCED ||
                        r.getStatus() == Receivable.TokenStatus.PARTIALLY_FINANCED)
                .count();

        BigDecimal totalReceivableValue = relevantReceivables.stream()
                .map(Receivable::getFaceValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalFinancedValue = relevantFinancing.stream()
                .filter(f -> f.getStatus() != FinancingRecord.FinancingStatus.PENDING &&
                        f.getStatus() != FinancingRecord.FinancingStatus.REJECTED)
                .map(FinancingRecord::getFinancingAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalDisbursedValue = relevantFinancing.stream()
                .filter(f -> f.getStatus() == FinancingRecord.FinancingStatus.DISBURSED ||
                        f.getStatus() == FinancingRecord.FinancingStatus.REPAID)
                .map(FinancingRecord::getActualAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long pendingFinancing = relevantFinancing.stream()
                .filter(f -> f.getStatus() == FinancingRecord.FinancingStatus.PENDING)
                .count();

        return DashboardStats.builder()
                .activeReceivables(confirmedReceivables + financedReceivables)
                .confirmedReceivables(confirmedReceivables)
                .financedReceivables(financedReceivables)
                .totalReceivableValue(totalReceivableValue)
                .totalFinancedValue(totalFinancedValue)
                .totalDisbursedValue(totalDisbursedValue)
                .pendingFinancing(pendingFinancing)
                .averageCreditScore(enterprise.getCreditScore())
                .build();
    }
}
