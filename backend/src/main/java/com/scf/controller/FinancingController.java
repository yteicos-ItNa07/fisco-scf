package com.scf.controller;

import com.scf.dto.RequestDTO.*;
import com.scf.dto.ResponseDTO.*;
import com.scf.entity.Enterprise;
import com.scf.security.SecurityUtils;
import com.scf.service.ReceivableService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/financing")
@RequiredArgsConstructor
public class FinancingController {

    private final ReceivableService receivableService;
    private final SecurityUtils securityUtils;

    /**
     * 申请融资（供应商）
     * 从JWT获取当前用户企业ID
     */
    @PostMapping("/apply")
    public ApiResponse<FinancingInfo> apply(@Valid @RequestBody ApplyFinancing request) {
        Long borrowerId = securityUtils.getCurrentEnterpriseId();
        return ApiResponse.success(receivableService.applyFinancing(borrowerId, request));
    }

    /**
     * 审批融资（金融机构）
     */
    @PostMapping("/approve")
    public ApiResponse<FinancingInfo> approve(@Valid @RequestBody ApproveFinancing request) {
        securityUtils.validateFinancialInst();
        Long financierId = securityUtils.getCurrentEnterpriseId();
        return ApiResponse.success(receivableService.approveFinancing(financierId, request));
    }

    /**
     * 放款（金融机构）
     */
    @PostMapping("/{id}/disburse")
    public ApiResponse<FinancingInfo> disburse(@PathVariable Long id) {
        securityUtils.validateFinancialInst();
        Long financierId = securityUtils.getCurrentEnterpriseId();
        return ApiResponse.success(receivableService.disburseFinancing(financierId, id));
    }

    /**
     * 还款（借款方/供应商）
     */
    @PostMapping("/{id}/repay")
    public ApiResponse<FinancingInfo> repay(@PathVariable Long id) {
        Long borrowerId = securityUtils.getCurrentEnterpriseId();
        return ApiResponse.success(receivableService.repayFinancing(borrowerId, id));
    }

    /**
     * 获取当前企业的融资记录（借款人视角）
     */
    @GetMapping("/my/borrowing")
    public ApiResponse<List<FinancingInfo>> getMyBorrowing() {
        Long borrowerId = securityUtils.getCurrentEnterpriseId();
        return ApiResponse.success(receivableService.getFinancingByBorrower(borrowerId));
    }

    /**
     * 获取当前企业签发的应收账款相关融资记录（核心企业视角）
     */
    @GetMapping("/my/issued")
    public ApiResponse<List<FinancingInfo>> getMyIssued() {
        Long issuerId = securityUtils.getCurrentEnterpriseId();
        return ApiResponse.success(receivableService.getFinancingByIssuer(issuerId));
    }

    /**
     * 获取当前企业的融资记录（金融机构视角）
     */
    @GetMapping("/my/lending")
    public ApiResponse<List<FinancingInfo>> getMyLending() {
        securityUtils.validateFinancialInst();
        Long financierId = securityUtils.getCurrentEnterpriseId();
        return ApiResponse.success(receivableService.getFinancingByFinancier(financierId));
    }

    /**
     * 获取待审批的融资申请（金融机构）
     */
    @GetMapping("/my/pending")
    public ApiResponse<List<FinancingInfo>> getMyPending() {
        securityUtils.validateFinancialInst();
        Long financierId = securityUtils.getCurrentEnterpriseId();
        return ApiResponse.success(receivableService.getPendingFinancing(financierId));
    }

    /**
     * 获取借款人的融资记录（兼容旧接口）
     */
    @GetMapping("/borrower/{borrowerId}")
    public ApiResponse<List<FinancingInfo>> getByBorrower(@PathVariable Long borrowerId) {
        if (!securityUtils.belongsToEnterprise(borrowerId)) {
            securityUtils.validateEnterpriseAccess(borrowerId);
        }
        return ApiResponse.success(receivableService.getFinancingByBorrower(borrowerId));
    }

    /**
     * 获取待审批的融资申请（兼容旧接口）
     */
    @GetMapping("/pending/{financierId}")
    public ApiResponse<List<FinancingInfo>> getPending(@PathVariable Long financierId) {
        if (!securityUtils.belongsToEnterprise(financierId)) {
            securityUtils.validateEnterpriseAccess(financierId);
        }
        return ApiResponse.success(receivableService.getPendingFinancing(financierId));
    }

    /**
     * 获取融资详情
     */
    @GetMapping("/{id}")
    public ApiResponse<FinancingInfo> getById(@PathVariable Long id) {
        return ApiResponse.success(receivableService.getFinancingById(id));
    }
}
