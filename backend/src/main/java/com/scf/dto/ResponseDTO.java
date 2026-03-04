package com.scf.dto;

import com.scf.entity.*;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class ResponseDTO {

    @Data
    @Builder
    public static class ApiResponse<T> {
        private int code;
        private String message;
        private T data;
        private long timestamp;

        public static <T> ApiResponse<T> success(T data) {
            return ApiResponse.<T>builder()
                    .code(200)
                    .message("success")
                    .data(data)
                    .timestamp(System.currentTimeMillis())
                    .build();
        }

        public static <T> ApiResponse<T> error(int code, String message) {
            return ApiResponse.<T>builder()
                    .code(code)
                    .message(message)
                    .timestamp(System.currentTimeMillis())
                    .build();
        }
    }

    @Data
    @Builder
    public static class LoginResponse {
        private String token;
        private UserInfo user;
    }

    @Data
    @Builder
    public static class UserInfo {
        private Long id;
        private String username;
        private String realName;
        private User.UserRole role;
        private EnterpriseInfo enterprise;
    }

    @Data
    @Builder
    public static class EnterpriseInfo {
        private Long id;
        private String creditCode;
        private String name;
        private String legalPerson;
        private String phone;
        private String address;
        private Enterprise.EnterpriseRole role;
        private Integer creditScore;
        private String blockchainAddress;
        private Boolean isActive;
        private LocalDateTime createdAt;

        public static EnterpriseInfo fromEntity(Enterprise e) {
            if (e == null) return null;
            return EnterpriseInfo.builder()
                    .id(e.getId())
                    .creditCode(e.getCreditCode())
                    .name(e.getName())
                    .legalPerson(e.getLegalPerson())
                    .phone(e.getPhone())
                    .address(e.getAddress())
                    .role(e.getRole())
                    .creditScore(e.getCreditScore())
                    .blockchainAddress(e.getBlockchainAddress())
                    .isActive(e.getIsActive())
                    .createdAt(e.getCreatedAt())
                    .build();
        }
    }

    @Data
    @Builder
    public static class ReceivableInfo {
        private Long id;
        private Long tokenId;
        private Long parentTokenId;
        private EnterpriseInfo issuer;
        private EnterpriseInfo holder;
        private BigDecimal faceValue;
        private BigDecimal remainingValue;
        private LocalDate issueDate;
        private LocalDate maturityDate;
        private BigDecimal annualRate;
        private String contractHash;
        private Receivable.TokenStatus status;
        private Boolean isSplittable;
        private String txHash;
        private String description;
        private LocalDateTime createdAt;
        private Integer daysToMaturity;

        public static ReceivableInfo fromEntity(Receivable r) {
            if (r == null) return null;
            int days = (int) java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), r.getMaturityDate());
            return ReceivableInfo.builder()
                    .id(r.getId())
                    .tokenId(r.getTokenId())
                    .parentTokenId(r.getParentTokenId())
                    .issuer(EnterpriseInfo.fromEntity(r.getIssuer()))
                    .holder(EnterpriseInfo.fromEntity(r.getHolder()))
                    .faceValue(r.getFaceValue())
                    .remainingValue(r.getRemainingValue())
                    .issueDate(r.getIssueDate())
                    .maturityDate(r.getMaturityDate())
                    .annualRate(r.getAnnualRate())
                    .contractHash(r.getContractHash())
                    .status(r.getStatus())
                    .isSplittable(r.getIsSplittable())
                    .txHash(r.getTxHash())
                    .description(r.getDescription())
                    .createdAt(r.getCreatedAt())
                    .daysToMaturity(days)
                    .build();
        }
    }

    /**
     * 通证详情（包含流转历史）
     */
    @Data
    @Builder
    public static class ReceivableDetailInfo {
        private ReceivableInfo receivable;
        private List<TransferInfo> transferHistory;
        private List<FinancingInfo> financingRecords;
        private RiskAssessment riskAssessment;
    }

    /**
     * 流转记录信息
     */
    @Data
    @Builder
    public static class TransferInfo {
        private Long id;
        private TransferRecord.TransferType type;
        private EnterpriseInfo fromEnterprise;
        private EnterpriseInfo toEnterprise;
        private Long fromTokenId;
        private Long toTokenId;
        private BigDecimal amount;
        private String reason;
        private String txHash;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    public static class FinancingInfo {
        private Long id;
        private Long recordId;
        private ReceivableInfo receivable;
        private EnterpriseInfo financier;
        private EnterpriseInfo borrower;
        private BigDecimal financingAmount;
        private BigDecimal annualRate;
        private BigDecimal discountAmount;
        private BigDecimal actualAmount;
        private FinancingRecord.FinancingStatus status;
        private Boolean isRepaid;
        private String txHash;
        private LocalDateTime createdAt;
        private LocalDateTime disbursedAt;
        private LocalDateTime repaidAt;
    }

    @Data
    @Builder
    public static class RiskAssessment {
        private Long receivableId;
        private Integer issuerCreditScore;
        private Integer holderCreditScore;
        private BigDecimal suggestedRate;
        private String riskLevel;  // LOW, MEDIUM, HIGH
        private List<String> riskFactors;
        private BigDecimal maxFinancingAmount;
        private BigDecimal estimatedDiscount;
    }

    @Data
    @Builder
    public static class DashboardStats {
        private long totalEnterprises;
        private long coreEnterprises;
        private long suppliers;
        private long financialInsts;
        private long activeReceivables;
        private long confirmedReceivables;
        private long financedReceivables;
        private BigDecimal totalReceivableValue;
        private BigDecimal totalFinancedValue;
        private BigDecimal totalDisbursedValue;
        private long pendingFinancing;
        private long approvedFinancing;
        private double averageCreditScore;
        private double averageFinancingRate;
    }

    /**
     * 电子合同存证信息
     */
    @Data
    public static class ContractInfo {
        private Long id;
        private String fileName;
        private String fileType;
        private Long fileSize;
        private String fileHash;
        private String ipfsCid;
        private String ipfsUrl;
        private String description;
        private LocalDateTime createdAt;
        private String txHash;
        private Long uploaderEnterpriseId;
        private String uploaderEnterpriseName;
        private Long receivableId;
        private Long receivableTokenId;

        // 格式化文件大小
        public String getFileSizeFormatted() {
            if (fileSize == null) return "0 B";
            if (fileSize < 1024) return fileSize + " B";
            if (fileSize < 1024 * 1024) return String.format("%.1f KB", fileSize / 1024.0);
            return String.format("%.1f MB", fileSize / (1024.0 * 1024));
        }
    }

    /**
     * 合同验证结果
     */
    @Data
    @Builder
    public static class ContractVerifyResult {
        private Long contractId;
        private String fileHash;
        private boolean valid;
        private String message;
        private LocalDateTime verifiedAt;
    }
}
