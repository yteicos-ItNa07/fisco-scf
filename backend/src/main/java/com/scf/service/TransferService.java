package com.scf.service;

import com.scf.dto.ResponseDTO.TransferInfo;
import com.scf.dto.ResponseDTO.EnterpriseInfo;
import com.scf.dto.ResponseDTO.ReceivableInfo;
import com.scf.entity.Enterprise;
import com.scf.entity.Receivable;
import com.scf.entity.TransferRecord;
import com.scf.exception.BusinessException;
import com.scf.repository.EnterpriseRepository;
import com.scf.repository.ReceivableRepository;
import com.scf.repository.TransferRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferService {

    private final TransferRecordRepository transferRecordRepository;
    private final EnterpriseRepository enterpriseRepository;
    private final ReceivableRepository receivableRepository;

    /**
     * 获取所有流转记录
     */
    public List<TransferInfo> getAllTransfers() {
        return transferRecordRepository.findAll().stream()
                .map(this::toTransferInfo)
                .collect(Collectors.toList());
    }

    /**
     * 获取企业相关的流转记录（作为转出方或转入方）
     */
    public List<TransferInfo> getTransfersByEnterprise(Long enterpriseId) {
        Enterprise enterprise = enterpriseRepository.findById(enterpriseId)
                .orElseThrow(() -> new BusinessException("企业不存在"));

        List<TransferRecord> fromRecords = transferRecordRepository.findByFromEnterprise(enterprise);
        List<TransferRecord> toRecords = transferRecordRepository.findByToEnterprise(enterprise);

        // 合并并去重
        List<TransferRecord> allRecords = new ArrayList<>(fromRecords);
        for (TransferRecord record : toRecords) {
            if (!allRecords.contains(record)) {
                allRecords.add(record);
            }
        }

        // 按创建时间倒序排列
        allRecords.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));

        return allRecords.stream()
                .map(this::toTransferInfo)
                .collect(Collectors.toList());
    }

    /**
     * 获取通证相关的流转记录
     */
    public List<TransferInfo> getTransfersByReceivable(Long receivableId) {
        Receivable receivable = receivableRepository.findById(receivableId)
                .orElseThrow(() -> new BusinessException("通证不存在"));

        List<TransferRecord> records = new ArrayList<>();
        records.addAll(transferRecordRepository.findByFromReceivable(receivable));
        records.addAll(transferRecordRepository.findByToReceivable(receivable));

        // 去重并按时间排序
        return records.stream()
                .distinct()
                .sorted((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                .map(this::toTransferInfo)
                .collect(Collectors.toList());
    }

    /**
     * 按类型获取流转记录
     */
    public List<TransferInfo> getTransfersByType(TransferRecord.TransferType type) {
        return transferRecordRepository.findByType(type).stream()
                .map(this::toTransferInfo)
                .collect(Collectors.toList());
    }

    /**
     * 获取通证的完整流转历史（包括父通证的历史）
     */
    public List<TransferInfo> getFullTransferHistory(Long tokenId) {
        Receivable receivable = receivableRepository.findByTokenId(tokenId)
                .orElseThrow(() -> new BusinessException("通证不存在"));

        List<TransferInfo> history = new ArrayList<>();

        // 递归获取父通证的历史
        collectTransferHistory(receivable, history);

        // 按时间排序
        history.sort((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()));

        return history;
    }

    private void collectTransferHistory(Receivable receivable, List<TransferInfo> history) {
        // 先获取父通证的历史
        if (receivable.getParentTokenId() != null) {
            receivableRepository.findByTokenId(receivable.getParentTokenId())
                    .ifPresent(parent -> collectTransferHistory(parent, history));
        }

        // 获取当前通证的流转记录
        List<TransferRecord> records = new ArrayList<>();
        records.addAll(transferRecordRepository.findByFromReceivable(receivable));
        records.addAll(transferRecordRepository.findByToReceivable(receivable));

        records.stream()
                .distinct()
                .map(this::toTransferInfo)
                .forEach(history::add);
    }

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
