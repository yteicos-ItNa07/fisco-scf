package com.scf.entity;

import lombok.Data;
import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 流转记录实体
 */
@Data
@Entity
@Table(name = "transfer_record")
public class TransferRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_receivable_id")
    private Receivable fromReceivable;  // 源通证

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_receivable_id")
    private Receivable toReceivable;  // 目标通证

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_enterprise_id", nullable = false)
    private Enterprise fromEnterprise;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_enterprise_id", nullable = false)
    private Enterprise toEnterprise;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransferType type;

    @Column(length = 200)
    private String reason;

    @Column(length = 66)
    private String txHash;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum TransferType {
        ISSUE,      // 签发
        TRANSFER,   // 转让
        SPLIT,      // 拆分
        FINANCING,  // 融资
        SETTLEMENT  // 结算
    }
}
