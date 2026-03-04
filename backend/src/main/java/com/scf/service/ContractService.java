package com.scf.service;

import com.scf.dto.ResponseDTO.ContractInfo;
import com.scf.entity.Contract;
import com.scf.entity.Enterprise;
import com.scf.entity.Receivable;
import com.scf.exception.BusinessException;
import com.scf.repository.ContractRepository;
import com.scf.repository.ReceivableRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 电子合同存证服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContractService {

    private final ContractRepository contractRepository;
    private final ReceivableRepository receivableRepository;
    private final IpfsService ipfsService;

    // 允许的文件类型
    private static final List<String> ALLOWED_TYPES = List.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "image/jpeg",
            "image/png",
            "image/jpg"
    );

    // 最大文件大小 10MB
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    /**
     * 上传合同文件
     */
    @Transactional
    public ContractInfo uploadContract(MultipartFile file, Enterprise uploader,
                                        Long receivableId, String description) {
        // 验证文件
        validateFile(file);

        try {
            byte[] fileData = file.getBytes();
            String fileHash = calculateSHA256(fileData);

            // 检查是否已存在相同哈希的文件
            if (contractRepository.existsByFileHash(fileHash)) {
                throw new BusinessException("该合同文件已存在（哈希重复）");
            }

            // 创建合同记录
            Contract contract = new Contract();
            contract.setFileName(file.getOriginalFilename());
            contract.setFileType(getFileExtension(file.getOriginalFilename()));
            contract.setFileSize(file.getSize());
            contract.setFileHash(fileHash);
            contract.setFileData(fileData);
            contract.setUploaderEnterprise(uploader);
            contract.setDescription(description);

            // 上传到IPFS
            String ipfsCid = ipfsService.uploadToIpfs(fileData, file.getOriginalFilename());
            if (ipfsCid != null) {
                contract.setIpfsCid(ipfsCid);
                log.info("合同已上传至IPFS: CID={}", ipfsCid);
            }

            // 关联应收账款（如果有）
            if (receivableId != null) {
                Receivable receivable = receivableRepository.findById(receivableId)
                        .orElseThrow(() -> new BusinessException("应收账款不存在"));
                contract.setReceivable(receivable);

                // 同时更新应收账款的contractHash字段
                receivable.setContractHash(fileHash);
                receivableRepository.save(receivable);
            }

            contract = contractRepository.save(contract);
            log.info("合同上传成功: id={}, hash={}, fileName={}",
                    contract.getId(), fileHash, file.getOriginalFilename());

            return toContractInfo(contract);

        } catch (IOException e) {
            log.error("读取文件失败", e);
            throw new BusinessException("文件读取失败");
        }
    }

    /**
     * 获取合同信息（不含文件数据）
     */
    public ContractInfo getContractInfo(Long contractId) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new BusinessException("合同不存在"));
        return toContractInfo(contract);
    }

    /**
     * 下载合同文件
     */
    public Contract downloadContract(Long contractId) {
        return contractRepository.findById(contractId)
                .orElseThrow(() -> new BusinessException("合同不存在"));
    }

    /**
     * 根据应收账款ID获取关联合同列表
     */
    public List<ContractInfo> getContractsByReceivable(Long receivableId) {
        return contractRepository.findByReceivableId(receivableId).stream()
                .map(this::toContractInfo)
                .collect(Collectors.toList());
    }

    /**
     * 获取企业上传的所有合同
     */
    public List<ContractInfo> getContractsByEnterprise(Enterprise enterprise) {
        return contractRepository.findByUploaderEnterprise(enterprise).stream()
                .map(this::toContractInfo)
                .collect(Collectors.toList());
    }

    /**
     * 验证合同哈希（防篡改验证）
     */
    public boolean verifyContract(Long contractId) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new BusinessException("合同不存在"));

        String currentHash = calculateSHA256(contract.getFileData());
        boolean isValid = currentHash.equals(contract.getFileHash());

        log.info("合同验证: id={}, 存储哈希={}, 计算哈希={}, 结果={}",
                contractId, contract.getFileHash(), currentHash, isValid ? "通过" : "失败");

        return isValid;
    }

    /**
     * 通过哈希查找合同
     */
    public ContractInfo getContractByHash(String hash) {
        Contract contract = contractRepository.findByFileHash(hash)
                .orElseThrow(() -> new BusinessException("未找到该哈希对应的合同"));
        return toContractInfo(contract);
    }

    /**
     * 删除合同
     */
    @Transactional
    public void deleteContract(Long contractId, Enterprise operator) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new BusinessException("合同不存在"));

        // 只有上传者可以删除
        if (!contract.getUploaderEnterprise().getId().equals(operator.getId())) {
            throw new BusinessException(403, "无权删除此合同");
        }

        // 如果关联了应收账款，清除关联
        if (contract.getReceivable() != null) {
            Receivable receivable = contract.getReceivable();
            receivable.setContractHash(null);
            receivableRepository.save(receivable);
        }

        contractRepository.delete(contract);
        log.info("合同删除成功: id={}", contractId);
    }

    // ========== 私有方法 ==========

    /**
     * 验证文件
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的文件");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException("文件大小不能超过10MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new BusinessException("不支持的文件类型，仅支持 PDF、Word、JPG、PNG");
        }
    }

    /**
     * 计算文件SHA256哈希
     */
    private String calculateSHA256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(data);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256算法不可用", e);
        }
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "unknown";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }

    /**
     * 转换为DTO
     */
    private ContractInfo toContractInfo(Contract contract) {
        ContractInfo info = new ContractInfo();
        info.setId(contract.getId());
        info.setFileName(contract.getFileName());
        info.setFileType(contract.getFileType());
        info.setFileSize(contract.getFileSize());
        info.setFileHash(contract.getFileHash());
        info.setIpfsCid(contract.getIpfsCid());
        info.setIpfsUrl(ipfsService.getIpfsUrl(contract.getIpfsCid()));
        info.setDescription(contract.getDescription());
        info.setCreatedAt(contract.getCreatedAt());
        info.setTxHash(contract.getTxHash());

        if (contract.getUploaderEnterprise() != null) {
            info.setUploaderEnterpriseId(contract.getUploaderEnterprise().getId());
            info.setUploaderEnterpriseName(contract.getUploaderEnterprise().getName());
        }

        if (contract.getReceivable() != null) {
            info.setReceivableId(contract.getReceivable().getId());
            info.setReceivableTokenId(contract.getReceivable().getTokenId());
        }

        return info;
    }
}
