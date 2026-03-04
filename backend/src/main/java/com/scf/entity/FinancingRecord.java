package com.scf.entity;

import lombok.Data;
import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 融资记录实体
 */
@Data
@Entity
@Table(name = "financing_record")
public class FinancingRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long recordId;  // 链上记录ID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receivable_id", nullable = false)
    private Receivable receivable;  // 关联的应收账款

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "financier_id", nullable = false)
    private Enterprise financier;  // 金融机构

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "borrower_id", nullable = false)
    private Enterprise borrower;  // 借款人

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal financingAmount;  // 融资金额

    @Column(nullable = false, precision = 6, scale = 4)
    private BigDecimal annualRate;  // 融资利率

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal discountAmount;  // 折价金额

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal actualAmount;  // 实际放款金额

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FinancingStatus status = FinancingStatus.PENDING;

    @Column(nullable = false)
    private Boolean isRepaid = false;

    @Column(length = 66)
    private String txHash;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime disbursedAt;  // 放款时间

    private LocalDateTime repaidAt;  // 还款时间

    public enum FinancingStatus {
        PENDING,    // 待审批
        APPROVED,   // 已批准
        REJECTED,   // 已拒绝
        DISBURSED,  // 已放款
        REPAID      // 已还款
    }
}
