package com.scf.entity;

import lombok.Data;
import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 应收账款实体
 */
@Data
@Entity
@Table(name = "receivable")
public class Receivable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long tokenId;  // 链上通证ID

    private Long parentTokenId;  // 父通证ID（拆分时）

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issuer_id", nullable = false)
    private Enterprise issuer;  // 签发方（核心企业）

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "holder_id", nullable = false)
    private Enterprise holder;  // 当前持有方

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal faceValue;  // 票面金额

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal remainingValue;  // 剩余可用金额

    @Column(nullable = false)
    private LocalDate issueDate;  // 签发日期

    @Column(nullable = false)
    private LocalDate maturityDate;  // 到期日期

    @Column(nullable = false, precision = 6, scale = 4)
    private BigDecimal annualRate;  // 年化利率

    @Column(length = 100)
    private String contractHash;  // 电子合同IPFS哈希

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TokenStatus status = TokenStatus.CREATED;

    @Column(nullable = false)
    private Boolean isSplittable = true;  // 是否可拆分

    @Column(length = 66)
    private String txHash;  // 上链交易哈希

    @Column(length = 500)
    private String description;  // 描述

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public enum TokenStatus {
        CREATED,            // 已创建，待确权
        CONFIRMED,          // 已确权
        FINANCED,           // 已融资
        PARTIALLY_FINANCED, // 部分融资
        SETTLED,            // 已结算
        EXPIRED,            // 已过期
        CANCELLED           // 已取消
    }
}
