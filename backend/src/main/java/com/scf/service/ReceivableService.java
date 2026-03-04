package com.scf.service;

import com.scf.blockchain.BlockchainResult;
import com.scf.blockchain.BlockchainService;
import com.scf.dto.RequestDTO.*;
import com.scf.dto.ResponseDTO.*;
import com.scf.entity.*;
import com.scf.exception.BusinessException;
import com.scf.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReceivableService {

    private final ReceivableRepository receivableRepository;
    private final EnterpriseRepository enterpriseRepository;
    private final FinancingRecordRepository financingRecordRepository;
    private final TransferRecordRepository transferRecordRepository;
    private final BlockchainService blockchainService;
    private final RiskService riskService;

    /**
     * 签发应收账款通证（核心企业调用）
     */
    @Transactional
    public ReceivableInfo issueReceivable(Long issuerId, IssueReceivable request) {
        Enterprise issuer = enterpriseRepository.findById(issuerId)
                .orElseThrow(() -> new BusinessException("签发企业不存在"));

        if (issuer.getRole() != Enterprise.EnterpriseRole.CORE_ENTERPRISE) {
            throw new BusinessException("只有核心企业可以签发应收账款");
        }

        Enterprise holder = enterpriseRepository.findById(request.getHolderId())
                .orElseThrow(() -> new BusinessException("持有方企业不存在"));

        // 链上签发通证（必须真实上链）
        if (!blockchainService.isAvailable()) {
            throw new BusinessException("区块链服务不可用，无法签发应收账款");
        }

        Long tokenId;
        String txHash;

        try {
            BigInteger faceValue = request.getFaceValue()
                    .multiply(BigDecimal.valueOf(100))
                    .toBigInteger();  // 转换为分
            // FISCO-BCOS 2.x 的 block.timestamp (now) 返回毫秒级时间戳，需要 * 1000
            BigInteger maturityDate = BigInteger.valueOf(
                    request.getMaturityDate().atTime(23, 59, 59)
                            .atZone(java.time.ZoneId.systemDefault())
                            .toEpochSecond() * 1000
            );
            BigInteger annualRate = request.getAnnualRate()
                    .multiply(BigDecimal.valueOf(10000))
                    .toBigInteger();  // 转换为基点

            BlockchainResult<BigInteger> issueResult = blockchainService.issueToken(
                    holder.getBlockchainAddress(),
                    faceValue,
                    maturityDate,
                    annualRate,
                    request.getContractHash() != null ? request.getContractHash() : ""
            );
            tokenId = issueResult.getValue().longValue();
            txHash = issueResult.getTxHash();
            log.info("链上签发成功, tokenId: {}, txHash: {}", tokenId, txHash);
        } catch (Exception e) {
            log.error("链上签发失败: {}", e.getMessage());
            throw new BusinessException("链上签发失败: " + e.getMessage());
        }

        // 保存到数据库
        Receivable receivable = new Receivable();
        receivable.setTokenId(tokenId);
        receivable.setIssuer(issuer);
        receivable.setHolder(holder);
        receivable.setFaceValue(request.getFaceValue());
        receivable.setRemainingValue(request.getFaceValue());
        receivable.setIssueDate(LocalDate.now());
        receivable.setMaturityDate(request.getMaturityDate());
        receivable.setAnnualRate(request.getAnnualRate());
        receivable.setContractHash(request.getContractHash());
        receivable.setStatus(Receivable.TokenStatus.CONFIRMED);  // 核心企业签发即确权
        receivable.setDescription(request.getDescription());
        receivable.setTxHash(txHash);

        receivable = receivableRepository.save(receivable);

        // 记录流转
        TransferRecord record = new TransferRecord();
        record.setToReceivable(receivable);
        record.setFromEnterprise(issuer);
        record.setToEnterprise(holder);
        record.setAmount(request.getFaceValue());
        record.setType(TransferRecord.TransferType.ISSUE);
        record.setReason("核心企业签发应收账款");
        record.setTxHash(txHash);
        transferRecordRepository.save(record);

        log.info("应收账款签发成功, tokenId: {}, 金额: {}", tokenId, request.getFaceValue());

        return ReceivableInfo.fromEntity(receivable);
    }

    /**
     * 拆分应收账款通证
     */
    @Transactional
    public List<ReceivableInfo> splitReceivable(Long holderId, SplitReceivable request) {
        Receivable receivable = receivableRepository.findByTokenId(request.getTokenId())
                .orElseThrow(() -> new BusinessException("通证不存在"));

        if (!receivable.getHolder().getId().equals(holderId)) {
            throw new BusinessException("无权操作此通证");
        }

        if (!receivable.getIsSplittable()) {
            throw new BusinessException("此通证不可拆分");
        }

        if (receivable.getStatus() != Receivable.TokenStatus.CONFIRMED) {
            throw new BusinessException("只有已确权的通证可以拆分");
        }

        // 验证拆分金额
        BigDecimal totalAmount = request.getTargets().stream()
                .map(SplitTarget::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalAmount.compareTo(receivable.getRemainingValue()) > 0) {
            throw new BusinessException("拆分金额超过剩余可用金额");
        }

        // 链上拆分
        List<String> recipients = new ArrayList<>();
        List<BigInteger> amounts = new ArrayList<>();

        for (SplitTarget target : request.getTargets()) {
            Enterprise recipient = enterpriseRepository.findById(target.getRecipientId())
                    .orElseThrow(() -> new BusinessException("接收方企业不存在: " + target.getRecipientId()));
            recipients.add(recipient.getBlockchainAddress());
            amounts.add(target.getAmount().multiply(BigDecimal.valueOf(100)).toBigInteger());
        }

        // 链上拆分（必须真实上链）
        if (!blockchainService.isAvailable()) {
            throw new BusinessException("区块链服务不可用，无法拆分通证");
        }

        List<BigInteger> newTokenIds;
        String splitTxHash;
        try {
            BlockchainResult<List<BigInteger>> splitResult = blockchainService.splitToken(
                    BigInteger.valueOf(request.getTokenId()),
                    recipients,
                    amounts
            );
            newTokenIds = splitResult.getValue();
            splitTxHash = splitResult.getTxHash();
            if (newTokenIds == null || newTokenIds.isEmpty()) {
                throw new BusinessException("链上拆分返回空结果");
            }
            log.info("链上拆分成功, 新tokenIds: {}, txHash: {}", newTokenIds, splitTxHash);
        } catch (Exception e) {
            log.error("链上拆分失败: {}", e.getMessage());
            throw new BusinessException("链上拆分失败: " + e.getMessage());
        }

        // 保存新通证
        List<Receivable> newReceivables = new ArrayList<>();
        for (int i = 0; i < request.getTargets().size(); i++) {
            SplitTarget target = request.getTargets().get(i);
            Enterprise recipient = enterpriseRepository.findById(target.getRecipientId()).get();

            Receivable newReceivable = new Receivable();
            newReceivable.setTokenId(newTokenIds.get(i).longValue());
            newReceivable.setParentTokenId(receivable.getTokenId());
            newReceivable.setIssuer(receivable.getIssuer());
            newReceivable.setHolder(recipient);
            newReceivable.setFaceValue(target.getAmount());
            newReceivable.setRemainingValue(target.getAmount());
            newReceivable.setIssueDate(receivable.getIssueDate());
            newReceivable.setMaturityDate(receivable.getMaturityDate());
            newReceivable.setAnnualRate(receivable.getAnnualRate());
            newReceivable.setContractHash(receivable.getContractHash());
            newReceivable.setStatus(Receivable.TokenStatus.CONFIRMED);
            newReceivable.setTxHash(splitTxHash);

            newReceivable = receivableRepository.save(newReceivable);
            newReceivables.add(newReceivable);

            // 记录流转
            TransferRecord record = new TransferRecord();
            record.setFromReceivable(receivable);
            record.setToReceivable(newReceivable);
            record.setFromEnterprise(receivable.getHolder());
            record.setToEnterprise(recipient);
            record.setAmount(target.getAmount());
            record.setType(TransferRecord.TransferType.SPLIT);
            record.setReason(target.getPurpose());
            record.setTxHash(splitTxHash);
            transferRecordRepository.save(record);
        }

        // 更新原通证剩余金额
        receivable.setRemainingValue(receivable.getRemainingValue().subtract(totalAmount));
        if (receivable.getRemainingValue().compareTo(BigDecimal.ZERO) == 0) {
            receivable.setIsSplittable(false);
        }
        receivableRepository.save(receivable);

        log.info("应收账款拆分成功, 原tokenId: {}, 新tokenIds: {}", request.getTokenId(), newTokenIds);

        return newReceivables.stream()
                .map(ReceivableInfo::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 转让应收账款通证
     */
    @Transactional
    public ReceivableInfo transferReceivable(Long fromId, TransferReceivable request) {
        Receivable receivable = receivableRepository.findByTokenId(request.getTokenId())
                .orElseThrow(() -> new BusinessException("通证不存在"));

        if (!receivable.getHolder().getId().equals(fromId)) {
            throw new BusinessException("无权操作此通证");
        }

        Enterprise toEnterprise = enterpriseRepository.findById(request.getToEnterpriseId())
                .orElseThrow(() -> new BusinessException("目标企业不存在"));

        // 链上转让（必须真实上链）
        if (!blockchainService.isAvailable()) {
            throw new BusinessException("区块链服务不可用，无法转让通证");
        }

        String transferTxHash;
        try {
            BlockchainResult<Void> transferResult = blockchainService.transferToken(
                    BigInteger.valueOf(request.getTokenId()),
                    toEnterprise.getBlockchainAddress()
            );
            transferTxHash = transferResult.getTxHash();
            log.info("链上转让成功, tokenId: {}, txHash: {}", request.getTokenId(), transferTxHash);
        } catch (Exception e) {
            log.error("链上转让失败: {}", e.getMessage());
            throw new BusinessException("链上转让失败: " + e.getMessage());
        }

        Enterprise fromEnterprise = receivable.getHolder();
        receivable.setHolder(toEnterprise);
        receivable.setTxHash(transferTxHash);
        receivable = receivableRepository.save(receivable);

        // 记录流转
        TransferRecord record = new TransferRecord();
        record.setFromReceivable(receivable);
        record.setToReceivable(receivable);
        record.setFromEnterprise(fromEnterprise);
        record.setToEnterprise(toEnterprise);
        record.setAmount(receivable.getRemainingValue());
        record.setType(TransferRecord.TransferType.TRANSFER);
        record.setReason(request.getReason());
        record.setTxHash(transferTxHash);
        transferRecordRepository.save(record);

        log.info("应收账款转让成功, tokenId: {}, from: {} -> to: {}",
                request.getTokenId(), fromEnterprise.getName(), toEnterprise.getName());

        return ReceivableInfo.fromEntity(receivable);
    }

    /**
     * 申请融资
     */
    @Transactional
    public FinancingInfo applyFinancing(Long borrowerId, ApplyFinancing request) {
        Receivable receivable = receivableRepository.findByTokenId(request.getTokenId())
                .orElseThrow(() -> new BusinessException("通证不存在"));

        if (!receivable.getHolder().getId().equals(borrowerId)) {
            throw new BusinessException("无权操作此通证");
        }

        if (receivable.getStatus() != Receivable.TokenStatus.CONFIRMED) {
            throw new BusinessException("只有已确权的通证可以申请融资");
        }

        if (request.getAmount().compareTo(receivable.getRemainingValue()) > 0) {
            throw new BusinessException("融资金额超过通证剩余金额");
        }

        Enterprise financier = enterpriseRepository.findById(request.getFinancierId())
                .orElseThrow(() -> new BusinessException("金融机构不存在"));

        if (financier.getRole() != Enterprise.EnterpriseRole.FINANCIAL_INST) {
            throw new BusinessException("目标企业不是金融机构");
        }

        // 风险评估
        RiskAssessment assessment = riskService.assessRisk(receivable.getId());
        if (assessment.getSuggestedRate().compareTo(BigDecimal.valueOf(0.08)) > 0) {
            throw new BusinessException("风险过高，无法融资");
        }

        // 计算折价
        long daysToMaturity = java.time.temporal.ChronoUnit.DAYS.between(
                LocalDate.now(), receivable.getMaturityDate()
        );
        BigDecimal discount = request.getAmount()
                .multiply(assessment.getSuggestedRate())
                .multiply(BigDecimal.valueOf(daysToMaturity))
                .divide(BigDecimal.valueOf(365), 2, RoundingMode.HALF_UP);
        BigDecimal actualAmount = request.getAmount().subtract(discount);

        // 链上融资申请
        if (!blockchainService.isAvailable()) {
            throw new BusinessException("区块链服务不可用，无法申请融资");
        }

        Long chainRecordId;
        String applyTxHash;
        try {
            BlockchainResult<BigInteger> applyResult = blockchainService.applyFinancing(
                    BigInteger.valueOf(receivable.getTokenId()),
                    financier.getBlockchainAddress(),
                    request.getAmount().multiply(BigDecimal.valueOf(100)).toBigInteger(),
                    assessment.getSuggestedRate().multiply(BigDecimal.valueOf(10000)).toBigInteger()
            );
            chainRecordId = applyResult.getValue().longValue();
            applyTxHash = applyResult.getTxHash();
            log.info("链上融资申请成功, chainRecordId: {}, txHash: {}", chainRecordId, applyTxHash);
        } catch (Exception e) {
            log.error("链上融资申请失败: {}", e.getMessage());
            throw new BusinessException("链上融资申请失败: " + e.getMessage());
        }

        // 创建融资记录
        FinancingRecord record = new FinancingRecord();
        record.setRecordId(chainRecordId);
        record.setReceivable(receivable);
        record.setFinancier(financier);
        record.setBorrower(receivable.getHolder());
        record.setFinancingAmount(request.getAmount());
        record.setAnnualRate(assessment.getSuggestedRate());
        record.setDiscountAmount(discount);
        record.setActualAmount(actualAmount);
        record.setStatus(FinancingRecord.FinancingStatus.PENDING);
        record.setTxHash(applyTxHash);

        record = financingRecordRepository.save(record);

        log.info("融资申请创建成功, recordId: {}, 金额: {}, 利率: {}",
                record.getId(), request.getAmount(), assessment.getSuggestedRate());

        return toFinancingInfo(record);
    }

    /**
     * 审批融资（金融机构调用）
     */
    @Transactional
    public FinancingInfo approveFinancing(Long financierId, ApproveFinancing request) {
        FinancingRecord record = financingRecordRepository.findById(request.getFinancingId())
                .orElseThrow(() -> new BusinessException("融资记录不存在"));

        if (!record.getFinancier().getId().equals(financierId)) {
            throw new BusinessException("无权审批此融资申请");
        }

        if (record.getStatus() != FinancingRecord.FinancingStatus.PENDING) {
            throw new BusinessException("融资申请状态不正确");
        }

        // 链上审批
        if (!blockchainService.isAvailable()) {
            throw new BusinessException("区块链服务不可用，无法审批融资");
        }

        String approveTxHash;
        try {
            BlockchainResult<Void> approveResult = blockchainService.approveFinancing(
                    BigInteger.valueOf(record.getRecordId()),
                    request.getApproved()
            );
            approveTxHash = approveResult.getTxHash();
            log.info("链上融资审批成功, recordId: {}, approved: {}, txHash: {}", record.getRecordId(), request.getApproved(), approveTxHash);
        } catch (Exception e) {
            log.error("链上融资审批失败: {}", e.getMessage());
            throw new BusinessException("链上融资审批失败: " + e.getMessage());
        }

        if (request.getApproved()) {
            // 批准融资
            if (request.getRate() != null) {
                record.setAnnualRate(request.getRate());
                // 重新计算折价
                Receivable receivable = record.getReceivable();
                long daysToMaturity = java.time.temporal.ChronoUnit.DAYS.between(
                        LocalDate.now(), receivable.getMaturityDate()
                );
                BigDecimal discount = record.getFinancingAmount()
                        .multiply(request.getRate())
                        .multiply(BigDecimal.valueOf(daysToMaturity))
                        .divide(BigDecimal.valueOf(365), 2, RoundingMode.HALF_UP);
                record.setDiscountAmount(discount);
                record.setActualAmount(record.getFinancingAmount().subtract(discount));
            }
            record.setStatus(FinancingRecord.FinancingStatus.APPROVED);
            record.setTxHash(approveTxHash);

            // 更新通证状态
            Receivable receivable = record.getReceivable();
            receivable.setStatus(Receivable.TokenStatus.FINANCED);
            receivableRepository.save(receivable);

            log.info("融资申请已批准, recordId: {}", record.getId());
        } else {
            record.setStatus(FinancingRecord.FinancingStatus.REJECTED);
            record.setTxHash(approveTxHash);
            log.info("融资申请已拒绝, recordId: {}", record.getId());
        }

        record = financingRecordRepository.save(record);

        return toFinancingInfo(record);
    }

    /**
     * 放款（金融机构调用）
     */
    @Transactional
    public FinancingInfo disburseFinancing(Long financierId, Long financingId) {
        FinancingRecord record = financingRecordRepository.findById(financingId)
                .orElseThrow(() -> new BusinessException("融资记录不存在"));

        if (!record.getFinancier().getId().equals(financierId)) {
            throw new BusinessException("无权操作此融资记录");
        }

        if (record.getStatus() != FinancingRecord.FinancingStatus.APPROVED) {
            throw new BusinessException("只有已批准的融资可以放款");
        }

        // 链上放款
        if (!blockchainService.isAvailable()) {
            throw new BusinessException("区块链服务不可用，无法放款");
        }

        String disburseTxHash;
        try {
            BlockchainResult<Void> disburseResult = blockchainService.disburseFinancing(
                    BigInteger.valueOf(record.getRecordId())
            );
            disburseTxHash = disburseResult.getTxHash();
            log.info("链上放款成功, recordId: {}, txHash: {}", record.getRecordId(), disburseTxHash);
        } catch (Exception e) {
            log.error("链上放款失败: {}", e.getMessage());
            throw new BusinessException("链上放款失败: " + e.getMessage());
        }

        // 更新状态为已放款
        record.setStatus(FinancingRecord.FinancingStatus.DISBURSED);
        record.setDisbursedAt(LocalDateTime.now());
        record.setTxHash(disburseTxHash);
        record = financingRecordRepository.save(record);

        // 记录流转
        TransferRecord transferRecord = new TransferRecord();
        transferRecord.setFromReceivable(record.getReceivable());
        transferRecord.setToReceivable(record.getReceivable());
        transferRecord.setFromEnterprise(record.getFinancier());
        transferRecord.setToEnterprise(record.getBorrower());
        transferRecord.setAmount(record.getActualAmount());
        transferRecord.setType(TransferRecord.TransferType.FINANCING);
        transferRecord.setReason("融资放款");
        transferRecord.setTxHash(disburseTxHash);
        transferRecordRepository.save(transferRecord);

        log.info("融资已放款, recordId: {}, 实际放款金额: {}", record.getId(), record.getActualAmount());

        return toFinancingInfo(record);
    }

    /**
     * 获取通证详情（包含流转历史和融资记录）
     */
    public ReceivableDetailInfo getReceivableDetail(Long receivableId) {
        Receivable receivable = receivableRepository.findById(receivableId)
                .orElseThrow(() -> new BusinessException("通证不存在"));

        // 获取流转历史
        List<TransferRecord> transferRecords = new ArrayList<>();
        transferRecords.addAll(transferRecordRepository.findByFromReceivable(receivable));
        transferRecords.addAll(transferRecordRepository.findByToReceivable(receivable));

        // 去重并排序
        List<TransferInfo> transferHistory = transferRecords.stream()
                .distinct()
                .sorted((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                .map(this::toTransferInfo)
                .collect(Collectors.toList());

        // 获取融资记录
        List<FinancingInfo> financingRecords = financingRecordRepository.findByReceivable(receivable).stream()
                .map(this::toFinancingInfo)
                .collect(Collectors.toList());

        // 获取风险评估
        RiskAssessment riskAssessment = null;
        try {
            riskAssessment = riskService.assessRisk(receivableId);
        } catch (Exception e) {
            log.warn("风险评估失败: {}", e.getMessage());
        }

        return ReceivableDetailInfo.builder()
                .receivable(ReceivableInfo.fromEntity(receivable))
                .transferHistory(transferHistory)
                .financingRecords(financingRecords)
                .riskAssessment(riskAssessment)
                .build();
    }

    public ReceivableInfo getByTokenId(Long tokenId) {
        Receivable receivable = receivableRepository.findByTokenId(tokenId)
                .orElseThrow(() -> new BusinessException("通证不存在"));
        return ReceivableInfo.fromEntity(receivable);
    }

    public List<ReceivableInfo> getByHolder(Long holderId) {
        Enterprise holder = enterpriseRepository.findById(holderId)
                .orElseThrow(() -> new BusinessException("企业不存在"));
        return receivableRepository.findByHolder(holder).stream()
                .map(ReceivableInfo::fromEntity)
                .collect(Collectors.toList());
    }

    public List<ReceivableInfo> getByIssuer(Long issuerId) {
        Enterprise issuer = enterpriseRepository.findById(issuerId)
                .orElseThrow(() -> new BusinessException("企业不存在"));
        return receivableRepository.findByIssuer(issuer).stream()
                .map(ReceivableInfo::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 获取所有可融资的应收账款（已确权状态）
     */
    public List<ReceivableInfo> getAvailableForFinancing() {
        return receivableRepository.findByStatus(Receivable.TokenStatus.CONFIRMED).stream()
                .map(ReceivableInfo::fromEntity)
                .collect(Collectors.toList());
    }

    public List<FinancingInfo> getFinancingByBorrower(Long borrowerId) {
        Enterprise borrower = enterpriseRepository.findById(borrowerId)
                .orElseThrow(() -> new BusinessException("企业不存在"));
        return financingRecordRepository.findByBorrower(borrower).stream()
                .map(this::toFinancingInfo)
                .collect(Collectors.toList());
    }

    public List<FinancingInfo> getFinancingByFinancier(Long financierId) {
        Enterprise financier = enterpriseRepository.findById(financierId)
                .orElseThrow(() -> new BusinessException("企业不存在"));
        return financingRecordRepository.findByFinancier(financier).stream()
                .map(this::toFinancingInfo)
                .collect(Collectors.toList());
    }

    public List<FinancingInfo> getFinancingByIssuer(Long issuerId) {
        Enterprise issuer = enterpriseRepository.findById(issuerId)
                .orElseThrow(() -> new BusinessException("企业不存在"));
        return financingRecordRepository.findByReceivable_Issuer(issuer).stream()
                .map(this::toFinancingInfo)
                .collect(Collectors.toList());
    }

    public List<FinancingInfo> getPendingFinancing(Long financierId) {
        Enterprise financier = enterpriseRepository.findById(financierId)
                .orElseThrow(() -> new BusinessException("企业不存在"));
        return financingRecordRepository.findByFinancierAndStatus(financier, FinancingRecord.FinancingStatus.PENDING)
                .stream()
                .map(this::toFinancingInfo)
                .collect(Collectors.toList());
    }

    public FinancingInfo getFinancingById(Long id) {
        FinancingRecord record = financingRecordRepository.findById(id)
                .orElseThrow(() -> new BusinessException("融资记录不存在"));
        return toFinancingInfo(record);
    }

    /**
     * 还款（借款方调用）
     * 将融资状态从 DISBURSED → REPAID
     */
    @Transactional
    public FinancingInfo repayFinancing(Long borrowerId, Long financingId) {
        FinancingRecord record = financingRecordRepository.findById(financingId)
                .orElseThrow(() -> new BusinessException("融资记录不存在"));

        if (!record.getBorrower().getId().equals(borrowerId)) {
            throw new BusinessException("无权操作此融资记录");
        }

        if (record.getStatus() != FinancingRecord.FinancingStatus.DISBURSED) {
            throw new BusinessException("只有已放款的融资可以还款");
        }

        // 链上确认还款（如果Settlement合约可用）
        String repayTxHash = null;
        if (blockchainService.isSettlementAvailable()) {
            try {
                BlockchainResult<Void> repayResult = blockchainService.confirmRepayment(
                        BigInteger.valueOf(record.getRecordId())
                );
                repayTxHash = repayResult.getTxHash();
                log.info("链上还款确认成功, recordId: {}, txHash: {}", record.getRecordId(), repayTxHash);
            } catch (Exception e) {
                log.warn("链上还款确认失败（将仅更新链下状态）: {}", e.getMessage());
            }
        }

        // 更新融资记录
        record.setStatus(FinancingRecord.FinancingStatus.REPAID);
        record.setIsRepaid(true);
        record.setRepaidAt(LocalDateTime.now());
        if (repayTxHash != null) {
            record.setTxHash(repayTxHash);
        }
        record = financingRecordRepository.save(record);

        // 更新通证状态为已结算
        Receivable receivable = record.getReceivable();
        receivable.setStatus(Receivable.TokenStatus.SETTLED);
        receivableRepository.save(receivable);

        // 记录流转
        TransferRecord transferRecord = new TransferRecord();
        transferRecord.setFromReceivable(receivable);
        transferRecord.setToReceivable(receivable);
        transferRecord.setFromEnterprise(record.getBorrower());
        transferRecord.setToEnterprise(record.getFinancier());
        transferRecord.setAmount(record.getFinancingAmount());
        transferRecord.setType(TransferRecord.TransferType.SETTLEMENT);
        transferRecord.setReason("融资还款");
        transferRecord.setTxHash(repayTxHash);
        transferRecordRepository.save(transferRecord);

        log.info("融资已还款, recordId: {}, 还款金额: {}", record.getId(), record.getFinancingAmount());

        return toFinancingInfo(record);
    }

    /**
     * 到期结算（核心企业向持有方/金融机构付款）
     */
    @Transactional
    public ReceivableInfo settleReceivable(Long payerId, Long receivableId) {
        Receivable receivable = receivableRepository.findById(receivableId)
                .orElseThrow(() -> new BusinessException("通证不存在"));

        if (!receivable.getIssuer().getId().equals(payerId)) {
            throw new BusinessException("只有签发企业可以结算");
        }

        if (receivable.getStatus() == Receivable.TokenStatus.SETTLED) {
            throw new BusinessException("通证已结算");
        }

        if (receivable.getStatus() == Receivable.TokenStatus.CANCELLED ||
                receivable.getStatus() == Receivable.TokenStatus.EXPIRED) {
            throw new BusinessException("通证状态不允许结算");
        }

        // 链上创建结算记录（如果Settlement合约可用）
        String settleTxHash = null;
        if (blockchainService.isSettlementAvailable()) {
            try {
                Enterprise payer = receivable.getIssuer();
                Enterprise payee = receivable.getHolder();
                BigInteger principal = receivable.getRemainingValue()
                        .multiply(BigDecimal.valueOf(100)).toBigInteger();
                BigInteger interest = BigInteger.ZERO;

                BlockchainResult<BigInteger> settleResult = blockchainService.createSettlement(
                        BigInteger.valueOf(receivable.getTokenId()),
                        payer.getBlockchainAddress(),
                        payee.getBlockchainAddress(),
                        principal,
                        interest
                );
                settleTxHash = settleResult.getTxHash();
                log.info("链上结算创建成功, tokenId: {}, txHash: {}", receivable.getTokenId(), settleTxHash);
            } catch (Exception e) {
                log.warn("链上结算创建失败（将仅更新链下状态）: {}", e.getMessage());
            }
        }

        // 更新通证状态
        receivable.setStatus(Receivable.TokenStatus.SETTLED);
        if (settleTxHash != null) {
            receivable.setTxHash(settleTxHash);
        }
        receivable = receivableRepository.save(receivable);

        // 同时将关联的未还款融资记录标记为已还款
        List<FinancingRecord> relatedFinancing = financingRecordRepository.findByReceivable(receivable);
        for (FinancingRecord fr : relatedFinancing) {
            if (fr.getStatus() == FinancingRecord.FinancingStatus.DISBURSED && !fr.getIsRepaid()) {
                fr.setStatus(FinancingRecord.FinancingStatus.REPAID);
                fr.setIsRepaid(true);
                fr.setRepaidAt(LocalDateTime.now());
                financingRecordRepository.save(fr);
            }
        }

        // 记录流转
        TransferRecord transferRecord = new TransferRecord();
        transferRecord.setFromReceivable(receivable);
        transferRecord.setToReceivable(receivable);
        transferRecord.setFromEnterprise(receivable.getIssuer());
        transferRecord.setToEnterprise(receivable.getHolder());
        transferRecord.setAmount(receivable.getRemainingValue());
        transferRecord.setType(TransferRecord.TransferType.SETTLEMENT);
        transferRecord.setReason("到期结算");
        transferRecord.setTxHash(settleTxHash);
        transferRecordRepository.save(transferRecord);

        log.info("应收账款已结算, tokenId: {}, 金额: {}", receivable.getTokenId(), receivable.getRemainingValue());

        return ReceivableInfo.fromEntity(receivable);
    }

    // 辅助方法：转换为FinancingInfo
    private FinancingInfo toFinancingInfo(FinancingRecord r) {
        return FinancingInfo.builder()
                .id(r.getId())
                .recordId(r.getRecordId())
                .receivable(ReceivableInfo.fromEntity(r.getReceivable()))
                .financier(EnterpriseInfo.fromEntity(r.getFinancier()))
                .borrower(EnterpriseInfo.fromEntity(r.getBorrower()))
                .financingAmount(r.getFinancingAmount())
                .annualRate(r.getAnnualRate())
                .discountAmount(r.getDiscountAmount())
                .actualAmount(r.getActualAmount())
                .status(r.getStatus())
                .isRepaid(r.getIsRepaid())
                .txHash(r.getTxHash())
                .createdAt(r.getCreatedAt())
                .disbursedAt(r.getDisbursedAt())
                .repaidAt(r.getRepaidAt())
                .build();
    }

    // 辅助方法：转换为TransferInfo
    private TransferInfo toTransferInfo(TransferRecord record) {
        return TransferInfo.builder()
                .id(record.getId())
                .type(record.getType())
                .fromEnterprise(record.getFromEnterprise() != null ?
                        EnterpriseInfo.fromEntity(record.getFromEnterprise()) : null)
                .toEnterprise(record.getToEnterprise() != null ?
                        EnterpriseInfo.fromEntity(record.getToEnterprise()) : null)
                .fromTokenId(record.getFromReceivable() != null ?
                        record.getFromReceivable().getTokenId() : null)
                .toTokenId(record.getToReceivable() != null ?
                        record.getToReceivable().getTokenId() : null)
                .amount(record.getAmount())
                .reason(record.getReason())
                .txHash(record.getTxHash())
                .createdAt(record.getCreatedAt())
                .build();
    }
}
