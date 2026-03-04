package com.scf.controller;

import com.scf.dto.ResponseDTO.*;
import com.scf.entity.TransferRecord;
import com.scf.security.SecurityUtils;
import com.scf.service.TransferService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/transfers")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;
    private final SecurityUtils securityUtils;

    /**
     * 获取当前企业相关的所有流转记录
     */
    @GetMapping("/my")
    public ApiResponse<List<TransferInfo>> getMyTransfers() {
        Long enterpriseId = securityUtils.getCurrentEnterpriseId();
        return ApiResponse.success(transferService.getTransfersByEnterprise(enterpriseId));
    }

    /**
     * 获取所有流转记录（管理员）
     */
    @GetMapping
    public ApiResponse<List<TransferInfo>> getAllTransfers() {
        return ApiResponse.success(transferService.getAllTransfers());
    }

    /**
     * 获取通证相关的流转记录
     */
    @GetMapping("/receivable/{receivableId}")
    public ApiResponse<List<TransferInfo>> getByReceivable(@PathVariable Long receivableId) {
        return ApiResponse.success(transferService.getTransfersByReceivable(receivableId));
    }

    /**
     * 获取通证的完整流转历史（包括父通证）
     */
    @GetMapping("/token/{tokenId}/history")
    public ApiResponse<List<TransferInfo>> getFullHistory(@PathVariable Long tokenId) {
        return ApiResponse.success(transferService.getFullTransferHistory(tokenId));
    }

    /**
     * 按类型获取流转记录
     */
    @GetMapping("/type/{type}")
    public ApiResponse<List<TransferInfo>> getByType(@PathVariable TransferRecord.TransferType type) {
        return ApiResponse.success(transferService.getTransfersByType(type));
    }

    /**
     * 获取指定企业的流转记录（兼容旧接口）
     */
    @GetMapping("/enterprise/{enterpriseId}")
    public ApiResponse<List<TransferInfo>> getByEnterprise(@PathVariable Long enterpriseId) {
        return ApiResponse.success(transferService.getTransfersByEnterprise(enterpriseId));
    }
}
