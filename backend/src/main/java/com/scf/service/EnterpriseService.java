package com.scf.service;

import com.scf.blockchain.BlockchainResult;
import com.scf.blockchain.BlockchainService;
import com.scf.dto.RequestDTO.EnterpriseRegister;
import com.scf.dto.ResponseDTO.EnterpriseInfo;
import com.scf.entity.Enterprise;
import com.scf.exception.BusinessException;
import com.scf.repository.EnterpriseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnterpriseService {

    private final EnterpriseRepository enterpriseRepository;
    private final BlockchainService blockchainService;

    @Transactional
    public EnterpriseInfo register(EnterpriseRegister request) {
        if (enterpriseRepository.existsByCreditCode(request.getCreditCode())) {
            throw new BusinessException("企业已注册");
        }

        // 生成区块链账户（必须真实上链）
        if (!blockchainService.isAvailable()) {
            throw new BusinessException("区块链服务不可用，无法注册企业");
        }

        String blockchainAddress = blockchainService.createAccount();

        Enterprise enterprise = new Enterprise();
        enterprise.setCreditCode(request.getCreditCode());
        enterprise.setName(request.getName());
        enterprise.setLegalPerson(request.getLegalPerson());
        enterprise.setPhone(request.getPhone());
        enterprise.setAddress(request.getAddress());
        enterprise.setRole(request.getRole());
        enterprise.setBlockchainAddress(blockchainAddress);
        enterprise.setCreditScore(700);  // 默认信用分

        enterprise = enterpriseRepository.save(enterprise);

        // 在区块链上注册企业
        try {
            BlockchainResult<Void> result = blockchainService.registerEnterprise(
                    blockchainAddress,
                    enterprise.getName(),
                    enterprise.getCreditCode(),
                    mapRole(enterprise.getRole())
            );
            log.info("企业注册成功并上链: {}, txHash: {}", enterprise.getName(), result.getTxHash());
        } catch (Exception e) {
            log.error("企业上链失败", e);
            throw new BusinessException("企业上链失败: " + e.getMessage());
        }

        return EnterpriseInfo.fromEntity(enterprise);
    }

    public EnterpriseInfo getById(Long id) {
        Enterprise enterprise = enterpriseRepository.findById(id)
                .orElseThrow(() -> new BusinessException("企业不存在"));
        return EnterpriseInfo.fromEntity(enterprise);
    }

    public EnterpriseInfo getByCreditCode(String creditCode) {
        Enterprise enterprise = enterpriseRepository.findByCreditCode(creditCode)
                .orElseThrow(() -> new BusinessException("企业不存在"));
        return EnterpriseInfo.fromEntity(enterprise);
    }

    public List<EnterpriseInfo> getAll() {
        return enterpriseRepository.findAll().stream()
                .map(EnterpriseInfo::fromEntity)
                .collect(Collectors.toList());
    }

    public List<EnterpriseInfo> getByRole(Enterprise.EnterpriseRole role) {
        return enterpriseRepository.findByRole(role).stream()
                .map(EnterpriseInfo::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public EnterpriseInfo updateCreditScore(Long id, Integer score) {
        Enterprise enterprise = enterpriseRepository.findById(id)
                .orElseThrow(() -> new BusinessException("企业不存在"));

        if (score < 0 || score > 1000) {
            throw new BusinessException("信用分必须在0-1000之间");
        }

        // 必须真实上链
        if (!blockchainService.isAvailable()) {
            throw new BusinessException("区块链服务不可用，无法更新信用分");
        }

        try {
            BlockchainResult<Void> result = blockchainService.updateCreditScore(enterprise.getBlockchainAddress(), score);
            log.info("链上信用分更新成功, 企业: {}, 新分数: {}, txHash: {}", enterprise.getName(), score, result.getTxHash());
        } catch (Exception e) {
            log.error("更新链上信用分失败", e);
            throw new BusinessException("更新链上信用分失败: " + e.getMessage());
        }

        enterprise.setCreditScore(score);
        enterprise = enterpriseRepository.save(enterprise);

        return EnterpriseInfo.fromEntity(enterprise);
    }

    private String mapRole(Enterprise.EnterpriseRole role) {
        switch (role) {
            case CORE_ENTERPRISE:
                return "CORE_ENTERPRISE";
            case SUPPLIER:
                return "SUPPLIER";
            case FINANCIAL_INST:
                return "FINANCIAL_INST";
            case REGULATOR:
                return "REGULATOR";
            default:
                return "SUPPLIER";
        }
    }
}
