package com.scf.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 电子合同存证实体
 */
@Data
@Entity
@Table(name = "contract")
public class Contract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String fileName;  // 原始文件名

    @Column(nullable = false, length = 50)
    private String fileType;  // 文件类型 (pdf, doc, docx, jpg, png等)

    @Column(nullable = false)
    private Long fileSize;  // 文件大小（字节）

    @Column(nullable = false, length = 64)
    private String fileHash;  // 文件SHA256哈希值（用于验证）

    @Lob
    @Column(nullable = false, columnDefinition = "LONGBLOB")
    private byte[] fileData;  // 文件二进制数据

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploader_enterprise_id")
    private Enterprise uploaderEnterprise;  // 上传企业

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receivable_id")
    private Receivable receivable;  // 关联的应收账款（可选）

    @Column(length = 66)
    private String txHash;  // 哈希上链的交易哈希

    @Column(length = 100)
    private String ipfsCid;  // IPFS内容标识符 (CID)

    @Column(length = 500)
    private String description;  // 合同描述

    @Column(nullable = false)
    private LocalDateTime createdAt;  // 上传时间

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
