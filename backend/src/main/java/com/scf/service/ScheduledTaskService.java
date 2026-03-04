package com.scf.service;

import com.scf.entity.FinancingRecord;
import com.scf.entity.Receivable;
import com.scf.repository.FinancingRecordRepository;
import com.scf.repository.ReceivableRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 定时任务服务
 * 处理通证到期、逾期检测等自动化任务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledTaskService {

    private final ReceivableRepository receivableRepository;
    private final FinancingRecordRepository financingRecordRepository;

    /**
     * 每天凌晨1点执行：检查到期通证
     * 将已过到期日且仍为 CONFIRMED/FINANCED/PARTIALLY_FINANCED 的通证标记为 EXPIRED
     */
    @Scheduled(cron = "0 0 1 * * ?")
    @Transactional
    public void checkExpiredTokens() {
        log.info("========== 开始检查到期通证 ==========");
        LocalDate today = LocalDate.now();
        int expiredCount = 0;

        // 查找所有已过期但状态仍为活跃的通证
        List<Receivable> activeReceivables = receivableRepository.findByStatusIn(List.of(
                Receivable.TokenStatus.CONFIRMED,
                Receivable.TokenStatus.FINANCED,
                Receivable.TokenStatus.PARTIALLY_FINANCED
        ));

        for (Receivable receivable : activeReceivables) {
            if (receivable.getMaturityDate().isBefore(today)) {
                // 检查是否有未完成的融资
                List<FinancingRecord> activeFinancing = financingRecordRepository.findByReceivable(receivable)
                        .stream()
                        .filter(f -> f.getStatus() == FinancingRecord.FinancingStatus.DISBURSED && !f.getIsRepaid())
                        .collect(Collectors.toList());

                if (activeFinancing.isEmpty()) {
                    // 无未还款融资，直接标记为过期
                    receivable.setStatus(Receivable.TokenStatus.EXPIRED);
                    receivableRepository.save(receivable);
                    expiredCount++;
                    log.info("通证已过期: tokenId={}, 到期日={}", receivable.getTokenId(), receivable.getMaturityDate());
                } else {
                    // 有未还款融资，记录逾期警告
                    log.warn("通证已到期但有未还款融资: tokenId={}, 到期日={}, 未还款融资数={}",
                            receivable.getTokenId(), receivable.getMaturityDate(), activeFinancing.size());
                }
            }
        }

        log.info("========== 到期检查完成: 新增过期通证 {} 个 ==========", expiredCount);
    }

    /**
     * 每天上午9点执行：检查即将到期的通证（提前7天预警）
     */
    @Scheduled(cron = "0 0 9 * * ?")
    public void checkUpcomingMaturity() {
        log.info("========== 开始检查即将到期通证 ==========");
        LocalDate today = LocalDate.now();
        LocalDate warningDate = today.plusDays(7);

        List<Receivable> activeReceivables = receivableRepository.findByStatusIn(List.of(
                Receivable.TokenStatus.CONFIRMED,
                Receivable.TokenStatus.FINANCED,
                Receivable.TokenStatus.PARTIALLY_FINANCED
        ));

        int warningCount = 0;
        for (Receivable receivable : activeReceivables) {
            if (receivable.getMaturityDate().isAfter(today) && receivable.getMaturityDate().isBefore(warningDate)) {
                long daysLeft = java.time.temporal.ChronoUnit.DAYS.between(today, receivable.getMaturityDate());
                log.warn("通证即将到期: tokenId={}, 到期日={}, 剩余{}天, 持有方={}, 签发方={}",
                        receivable.getTokenId(),
                        receivable.getMaturityDate(),
                        daysLeft,
                        receivable.getHolder().getName(),
                        receivable.getIssuer().getName());
                warningCount++;
            }
        }

        log.info("========== 到期预警完成: {} 个通证将在7天内到期 ==========", warningCount);
    }

    /**
     * 每天凌晨2点执行：检查逾期融资
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void checkOverdueFinancing() {
        log.info("========== 开始检查逾期融资 ==========");
        LocalDate today = LocalDate.now();
        int overdueCount = 0;

        List<FinancingRecord> disbursedRecords = financingRecordRepository.findByStatus(
                FinancingRecord.FinancingStatus.DISBURSED
        );

        for (FinancingRecord record : disbursedRecords) {
            if (!record.getIsRepaid() && record.getReceivable().getMaturityDate().isBefore(today)) {
                long overdueDays = java.time.temporal.ChronoUnit.DAYS.between(
                        record.getReceivable().getMaturityDate(), today
                );
                log.warn("融资逾期: recordId={}, 借款方={}, 金融机构={}, 金额={}, 逾期{}天",
                        record.getId(),
                        record.getBorrower().getName(),
                        record.getFinancier().getName(),
                        record.getFinancingAmount(),
                        overdueDays);
                overdueCount++;
            }
        }

        log.info("========== 逾期检查完成: {} 笔融资逾期 ==========", overdueCount);
    }
}
