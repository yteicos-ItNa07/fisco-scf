package com.scf.security;

import com.scf.entity.Enterprise;
import com.scf.entity.User;
import com.scf.exception.BusinessException;
import com.scf.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * 安全工具类 - 从JWT获取当前用户信息
 */
@Component
@RequiredArgsConstructor
public class SecurityUtils {

    private final UserRepository userRepository;

    /**
     * 获取当前登录用户ID
     */
    public Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw new BusinessException(401, "用户未登录");
        }
        try {
            return Long.valueOf(auth.getPrincipal().toString());
        } catch (NumberFormatException e) {
            throw new BusinessException(401, "无效的用户凭证");
        }
    }

    /**
     * 获取当前登录用户
     */
    public User getCurrentUser() {
        Long userId = getCurrentUserId();
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(401, "用户不存在"));
    }

    /**
     * 获取当前用户所属企业
     */
    public Enterprise getCurrentEnterprise() {
        User user = getCurrentUser();
        Enterprise enterprise = user.getEnterprise();
        if (enterprise == null) {
            throw new BusinessException("当前用户未关联企业");
        }
        return enterprise;
    }

    /**
     * 获取当前用户所属企业ID
     */
    public Long getCurrentEnterpriseId() {
        return getCurrentEnterprise().getId();
    }

    /**
     * 检查当前用户是否属于指定企业
     */
    public boolean belongsToEnterprise(Long enterpriseId) {
        try {
            return getCurrentEnterpriseId().equals(enterpriseId);
        } catch (BusinessException e) {
            return false;
        }
    }

    /**
     * 检查当前用户是否为指定角色
     */
    public boolean hasEnterpriseRole(Enterprise.EnterpriseRole role) {
        try {
            return getCurrentEnterprise().getRole() == role;
        } catch (BusinessException e) {
            return false;
        }
    }

    /**
     * 验证当前用户是否有权操作指定企业的资源
     */
    public void validateEnterpriseAccess(Long enterpriseId) {
        if (!belongsToEnterprise(enterpriseId)) {
            throw new BusinessException(403, "无权访问该企业资源");
        }
    }

    /**
     * 验证当前用户是否为核心企业
     */
    public void validateCoreEnterprise() {
        if (!hasEnterpriseRole(Enterprise.EnterpriseRole.CORE_ENTERPRISE)) {
            throw new BusinessException(403, "只有核心企业可以执行此操作");
        }
    }

    /**
     * 验证当前用户是否为金融机构
     */
    public void validateFinancialInst() {
        if (!hasEnterpriseRole(Enterprise.EnterpriseRole.FINANCIAL_INST)) {
            throw new BusinessException(403, "只有金融机构可以执行此操作");
        }
    }
}
