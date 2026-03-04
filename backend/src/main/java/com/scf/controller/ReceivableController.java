package com.scf.controller;

import com.scf.dto.RequestDTO.*;
import com.scf.dto.ResponseDTO.*;
import com.scf.security.SecurityUtils;
import com.scf.service.ReceivableService;
import com.scf.service.RiskService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/receivables")
@RequiredArgsConstructor
public class ReceivableController {

    private final ReceivableService receivableService;
    private final RiskService riskService;
    private final SecurityUtils securityUtils;

    /**
     * 签发应收账款（核心企业）
     * 从JWT获取当前用户企业ID，而非从请求参数传递
     */
    @PostMapping("/issue")
    public ApiResponse<ReceivableInfo> issue(@Valid @RequestBody IssueReceivable request) {
        securityUtils.validateCoreEnterprise();
        Long issuerId = securityUtils.getCurrentEnterpriseId();
        return ApiResponse.success(receivableService.issueReceivable(issuerId, request));
    }

    /**
     * 拆分应收账款
     */
    @PostMapping("/split")
    public ApiResponse<List<ReceivableInfo>> split(@Valid @RequestBody SplitReceivable request) {
        Long holderId = securityUtils.getCurrentEnterpriseId();
        return ApiResponse.success(receivableService.splitReceivable(holderId, request));
    }

    /**
     * 转让应收账款
     */
    @PostMapping("/transfer")
    public ApiResponse<ReceivableInfo> transfer(@Valid @RequestBody TransferReceivable request) {
        Long fromId = securityUtils.getCurrentEnterpriseId();
        return ApiResponse.success(receivableService.transferReceivable(fromId, request));
    }

    /**
     * 获取单个通证详情
     */
    @GetMapping("/token/{tokenId}")
    public ApiResponse<ReceivableInfo> getByTokenId(@PathVariable Long tokenId) {
        return ApiResponse.success(receivableService.getByTokenId(tokenId));
    }

    /**
     * 获取通证详情（包含流转历史）
     */
    @GetMapping("/{id}/detail")
    public ApiResponse<ReceivableDetailInfo> getDetail(@PathVariable Long id) {
        return ApiResponse.success(receivableService.getReceivableDetail(id));
    }

    /**
     * 获取当前企业持有的所有通证
     */
    @GetMapping("/my/holding")
    public ApiResponse<List<ReceivableInfo>> getMyHolding() {
        Long holderId = securityUtils.getCurrentEnterpriseId();
        return ApiResponse.success(receivableService.getByHolder(holderId));
    }

    /**
     * 获取当前企业签发的所有通证
     */
    @GetMapping("/my/issued")
    public ApiResponse<List<ReceivableInfo>> getMyIssued() {
        Long issuerId = securityUtils.getCurrentEnterpriseId();
        return ApiResponse.success(receivableService.getByIssuer(issuerId));
    }

    /**
     * 获取企业持有的所有通证（兼容旧接口）
     */
    @GetMapping("/holder/{holderId}")
    public ApiResponse<List<ReceivableInfo>> getByHolder(@PathVariable Long holderId) {
        // 只能查询自己企业的通证，或者管理员可以查询所有
        if (!securityUtils.belongsToEnterprise(holderId)) {
            securityUtils.validateEnterpriseAccess(holderId); // 会抛出403异常
        }
        return ApiResponse.success(receivableService.getByHolder(holderId));
    }

    /**
     * 获取企业签发的所有通证（兼容旧接口）
     */
    @GetMapping("/issuer/{issuerId}")
    public ApiResponse<List<ReceivableInfo>> getByIssuer(@PathVariable Long issuerId) {
        if (!securityUtils.belongsToEnterprise(issuerId)) {
            securityUtils.validateEnterpriseAccess(issuerId);
        }
        return ApiResponse.success(receivableService.getByIssuer(issuerId));
    }

    /**
     * 获取所有可融资的应收账款（金融机构用）
     */
    @GetMapping("/available-for-financing")
    public ApiResponse<List<ReceivableInfo>> getAvailableForFinancing() {
        securityUtils.validateFinancialInst();
        return ApiResponse.success(receivableService.getAvailableForFinancing());
    }

    /**
     * 风险评估
     */
    @GetMapping("/{id}/risk-assessment")
    public ApiResponse<RiskAssessment> assessRisk(@PathVariable Long id) {
        return ApiResponse.success(riskService.assessRisk(id));
    }

    /**
     * 到期结算（核心企业向持有方付款）
     */
    @PostMapping("/{id}/settle")
    public ApiResponse<ReceivableInfo> settle(@PathVariable Long id) {
        securityUtils.validateCoreEnterprise();
        Long payerId = securityUtils.getCurrentEnterpriseId();
        return ApiResponse.success(receivableService.settleReceivable(payerId, id));
    }
}
