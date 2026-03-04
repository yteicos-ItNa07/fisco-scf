package com.scf.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 企业实体
 */
@Data
@Entity
@Table(name = "enterprise")
public class Enterprise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 18)
    private String creditCode;  // 统一社会信用代码

    @Column(nullable = false, length = 100)
    private String name;  // 企业名称

    @Column(length = 50)
    private String legalPerson;  // 法人代表

    @Column(length = 20)
    private String phone;  // 联系电话

    @Column(length = 200)
    private String address;  // 企业地址

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EnterpriseRole role;  // 企业角色

    @Column(nullable = false)
    private Integer creditScore = 700;  // 信用评分 (0-1000)

    @Column(length = 42)
    private String blockchainAddress;  // 区块链地址

    @Column(nullable = false)
    private Boolean isActive = true;  // 是否激活

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public enum EnterpriseRole {
        CORE_ENTERPRISE,    // 核心企业
        SUPPLIER,           // 供应商
        FINANCIAL_INST,     // 金融机构
        REGULATOR           // 监管机构
    }
}
