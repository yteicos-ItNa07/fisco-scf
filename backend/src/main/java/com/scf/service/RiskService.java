package com.scf.service;

import com.scf.dto.ResponseDTO.RiskAssessment;
import com.scf.entity.Enterprise;
import com.scf.entity.Receivable;
import com.scf.exception.BusinessException;
import com.scf.repository.ReceivableRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RiskService {

    private final ReceivableRepository receivableRepository;

    @Value("${risk.base-rate:0.06}")
    private BigDecimal baseRate;

    @Value("${risk.max-rate:0.08}")
    private BigDecimal maxRate;

    @Value("${risk.min-credit-score:600}")
    private int minCreditScore;

    /**
     * 风险评估算法
     * 考虑因素：
     * 1. 核心企业（签发方）信用评分
     * 2. 持有方信用评分
     * 3. 距离到期日天数
     * 4. 应收账款金额
     */
    public RiskAssessment assessRisk(Long receivableId) {
        Receivable receivable = receivableRepository.findById(receivableId)
                .orElseThrow(() -> new BusinessException("应收账款不存在"));

        Enterprise issuer = receivable.getIssuer();
        Enterprise holder = receivable.getHolder();

        List<String> riskFactors = new ArrayList<>();
        BigDecimal riskScore = BigDecimal.ZERO;

        // 1. 核心企业信用评分权重 40%
        int issuerScore = issuer.getCreditScore();
        if (issuerScore < minCreditScore) {
            riskFactors.add("核心企业信用评分过低: " + issuerScore);
            riskScore = riskScore.add(BigDecimal.valueOf(0.02));
        } else if (issuerScore < 750) {
            riskFactors.add("核心企业信用评分中等: " + issuerScore);
            riskScore = riskScore.add(BigDecimal.valueOf(0.01));
        }

        // 2. 持有方信用评分权重 30%
        int holderScore = holder.getCreditScore();
        if (holderScore < minCreditScore) {
            riskFactors.add("持有方信用评分过低: " + holderScore);
            riskScore = riskScore.add(BigDecimal.valueOf(0.015));
        } else if (holderScore < 700) {
            riskFactors.add("持有方信用评分一般: " + holderScore);
            riskScore = riskScore.add(BigDecimal.valueOf(0.008));
        }

        // 3. 到期时间风险权重 20%
        long daysToMaturity = ChronoUnit.DAYS.between(LocalDate.now(), receivable.getMaturityDate());
        if (daysToMaturity > 180) {
            riskFactors.add("账期较长: " + daysToMaturity + "天");
            riskScore = riskScore.add(BigDecimal.valueOf(0.01));
        } else if (daysToMaturity > 90) {
            riskScore = riskScore.add(BigDecimal.valueOf(0.005));
        }

        // 4. 金额风险权重 10%
        if (receivable.getFaceValue().compareTo(BigDecimal.valueOf(1000000)) > 0) {
            riskFactors.add("大额应收账款: " + receivable.getFaceValue() + "元");
            riskScore = riskScore.add(BigDecimal.valueOf(0.005));
        }

        // 计算建议利率
        BigDecimal suggestedRate = baseRate.add(riskScore);
        if (suggestedRate.compareTo(maxRate) > 0) {
            suggestedRate = maxRate;
        }

        // 确定风险等级
        String riskLevel;
        if (suggestedRate.compareTo(BigDecimal.valueOf(0.065)) <= 0) {
            riskLevel = "LOW";
        } else if (suggestedRate.compareTo(BigDecimal.valueOf(0.075)) <= 0) {
            riskLevel = "MEDIUM";
        } else {
            riskLevel = "HIGH";
        }

        // 计算最大融资金额（基于风险等级）
        BigDecimal maxFinancingRatio;
        switch (riskLevel) {
            case "LOW":
                maxFinancingRatio = BigDecimal.valueOf(0.95);
                break;
            case "MEDIUM":
                maxFinancingRatio = BigDecimal.valueOf(0.85);
                break;
            default:
                maxFinancingRatio = BigDecimal.valueOf(0.70);
        }
        BigDecimal maxFinancingAmount = receivable.getRemainingValue().multiply(maxFinancingRatio);

        // 计算预计折价
        BigDecimal estimatedDiscount = maxFinancingAmount
                .multiply(suggestedRate)
                .multiply(BigDecimal.valueOf(daysToMaturity))
                .divide(BigDecimal.valueOf(365), 2, RoundingMode.HALF_UP);

        if (riskFactors.isEmpty()) {
            riskFactors.add("无明显风险因素");
        }

        log.info("风险评估完成 - 通证: {}, 利率: {}, 等级: {}", receivableId, suggestedRate, riskLevel);

        return RiskAssessment.builder()
                .receivableId(receivableId)
                .issuerCreditScore(issuerScore)
                .holderCreditScore(holderScore)
                .suggestedRate(suggestedRate)
                .riskLevel(riskLevel)
                .riskFactors(riskFactors)
                .maxFinancingAmount(maxFinancingAmount)
                .estimatedDiscount(estimatedDiscount)
                .build();
    }
}
