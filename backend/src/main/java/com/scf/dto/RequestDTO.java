package com.scf.dto;

import com.scf.entity.Enterprise;
import lombok.Data;
import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class RequestDTO {

    @Data
    public static class EnterpriseRegister {
        @NotBlank(message = "统一社会信用代码不能为空")
        @Size(min = 18, max = 18, message = "统一社会信用代码必须为18位")
        private String creditCode;

        @NotBlank(message = "企业名称不能为空")
        private String name;

        private String legalPerson;
        private String phone;
        private String address;

        @NotNull(message = "企业角色不能为空")
        private Enterprise.EnterpriseRole role;
    }

    @Data
    public static class UserRegister {
        @NotBlank(message = "用户名不能为空")
        @Size(min = 4, max = 50)
        private String username;

        @NotBlank(message = "密码不能为空")
        @Size(min = 6)
        private String password;

        private String realName;
        private String phone;
        private String email;

        @NotNull(message = "企业ID不能为空")
        private Long enterpriseId;
    }

    @Data
    public static class UserLogin {
        @NotBlank
        private String username;
        @NotBlank
        private String password;
    }

    @Data
    public static class IssueReceivable {
        @NotNull(message = "持有方企业ID不能为空")
        private Long holderId;

        @NotNull(message = "票面金额不能为空")
        @DecimalMin(value = "0.01", message = "票面金额必须大于0")
        private BigDecimal faceValue;

        @NotNull(message = "到期日期不能为空")
        @FutureOrPresent(message = "到期日期不能是过去日期")
        private LocalDate maturityDate;

        @NotNull(message = "年化利率不能为空")
        @DecimalMin(value = "0")
        @DecimalMax(value = "0.30", message = "年化利率不能超过30%")
        private BigDecimal annualRate;

        private String contractHash;
        private String description;
    }

    @Data
    public static class SplitReceivable {
        @NotNull(message = "通证ID不能为空")
        private Long tokenId;

        @NotEmpty(message = "拆分目标不能为空")
        private List<SplitTarget> targets;
    }

    @Data
    public static class SplitTarget {
        @NotNull
        private Long recipientId;
        @NotNull
        @DecimalMin(value = "0.01")
        private BigDecimal amount;
        private String purpose;
    }

    @Data
    public static class TransferReceivable {
        @NotNull
        private Long tokenId;
        @NotNull
        private Long toEnterpriseId;
        private String reason;
    }

    @Data
    public static class ApplyFinancing {
        @NotNull(message = "通证ID不能为空")
        private Long tokenId;

        @NotNull(message = "金融机构ID不能为空")
        private Long financierId;

        @NotNull(message = "融资金额不能为空")
        @DecimalMin(value = "0.01")
        private BigDecimal amount;
    }

    @Data
    public static class ApproveFinancing {
        @NotNull
        private Long financingId;

        private Boolean approved;

        @DecimalMin(value = "0")
        @DecimalMax(value = "0.08", message = "融资利率不能超过8%")
        private BigDecimal rate;
    }
}
