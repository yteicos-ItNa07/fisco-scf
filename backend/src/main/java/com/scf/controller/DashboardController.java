package com.scf.controller;

import com.scf.dto.ResponseDTO.*;
import com.scf.security.SecurityUtils;
import com.scf.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final SecurityUtils securityUtils;

    /**
     * 获取全局统计数据
     */
    @GetMapping("/stats")
    public ApiResponse<DashboardStats> getGlobalStats() {
        return ApiResponse.success(dashboardService.getGlobalStats());
    }

    /**
     * 获取当前企业的统计数据
     */
    @GetMapping("/my/stats")
    public ApiResponse<DashboardStats> getMyStats() {
        Long enterpriseId = securityUtils.getCurrentEnterpriseId();
        return ApiResponse.success(dashboardService.getEnterpriseStats(enterpriseId));
    }

    /**
     * 获取指定企业的统计数据
     */
    @GetMapping("/enterprise/{enterpriseId}/stats")
    public ApiResponse<DashboardStats> getEnterpriseStats(@PathVariable Long enterpriseId) {
        return ApiResponse.success(dashboardService.getEnterpriseStats(enterpriseId));
    }
}
