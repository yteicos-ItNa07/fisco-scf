package com.scf.config;

import com.scf.blockchain.BlockchainResult;
import com.scf.blockchain.BlockchainService;
import com.scf.entity.Contract;
import com.scf.entity.Enterprise;
import com.scf.entity.Enterprise.EnterpriseRole;
import com.scf.entity.FinancingRecord;
import com.scf.entity.FinancingRecord.FinancingStatus;
import com.scf.entity.Receivable;
import com.scf.entity.Receivable.TokenStatus;
import com.scf.entity.TransferRecord;
import com.scf.entity.TransferRecord.TransferType;
import com.scf.entity.User;
import com.scf.entity.User.UserRole;
import com.scf.repository.ContractRepository;
import com.scf.repository.EnterpriseRepository;
import com.scf.repository.FinancingRecordRepository;
import com.scf.repository.ReceivableRepository;
import com.scf.repository.TransferRecordRepository;
import com.scf.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 数据初始化器 - 应用启动时自动创建完整的示例数据
 * 企业和应收账款通过真实区块链调用上链，融资记录也经过链上操作
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final EnterpriseRepository enterpriseRepository;
    private final UserRepository userRepository;
    private final ReceivableRepository receivableRepository;
    private final TransferRecordRepository transferRecordRepository;
    private final FinancingRecordRepository financingRecordRepository;
    private final ContractRepository contractRepository;
    private final PasswordEncoder passwordEncoder;
    private final BlockchainService blockchainService;

    private boolean blockchainAvailable;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("========== 开始检查初始化数据 ==========");

        blockchainAvailable = blockchainService.isAvailable();
        if (blockchainAvailable) {
            log.info("区块链服务可用，将使用真实上链操作初始化数据");
        } else {
            log.warn("区块链服务不可用，企业和用户将使用离线模式初始化（地址标记为OFFLINE_），应收账款和融资记录将跳过");
        }

        // 如果合约被重新部署了，需要清空旧的区块链相关数据
        if (blockchainAvailable && blockchainService.isContractsRedeployed()) {
            log.warn("检测到合约已重新部署，清空旧的区块链相关数据...");
            contractRepository.deleteAll();
            transferRecordRepository.deleteAll();
            financingRecordRepository.deleteAll();
            receivableRepository.deleteAll();
            userRepository.deleteAll();
            enterpriseRepository.deleteAll();
            log.info("旧数据已清空，将重新初始化");
        }

        initEnterprises();
        initUsers();
        initReceivables();
        initFinancingRecords();
        initContracts();

        log.info("========== 数据初始化完成！==========");
        log.info("");
        log.info("╔════════════════════════════════════════════════════════╗");
        log.info("║           供应链金融平台 - 演示账号                      ║");
        log.info("╠════════════════════════════════════════════════════════╣");
        log.info("║  角色        │ 用户名           │ 密码                  ║");
        log.info("╠════════════════════════════════════════════════════════╣");
        log.info("║  核心企业    │ huawei_admin     │ 123456               ║");
        log.info("║  核心企业    │ zhongche_admin   │ 123456               ║");
        log.info("║  供应商      │ supplier1        │ 123456               ║");
        log.info("║  供应商      │ supplier2        │ 123456               ║");
        log.info("║  金融机构    │ icbc_admin       │ 123456               ║");
        log.info("║  金融机构    │ cmb_admin        │ 123456               ║");
        log.info("║  系统管理    │ admin            │ 123456               ║");
        log.info("╚════════════════════════════════════════════════════════╝");
        log.info("");
    }

    private void initEnterprises() {
        if (enterpriseRepository.count() > 0) {
            log.info("✓ 企业数据已存在 ({} 条)", enterpriseRepository.count());
            return;
        }

        log.info("→ 初始化企业数据...");

        // 核心企业
        createEnterprise("91110000MA01ABCD1X", "华为技术有限公司", "任正非",
                "13800138001", "广东省深圳市龙岗区坂田华为基地",
                EnterpriseRole.CORE_ENTERPRISE, 950);

        createEnterprise("91110000MA01ABCD2Y", "中国中车股份有限公司", "孙永才",
                "13800138002", "北京市海淀区中关村大街1号",
                EnterpriseRole.CORE_ENTERPRISE, 920);

        // 供应商
        createEnterprise("91110000MA01SUPP1A", "深圳市精密零件有限公司", "张明",
                "13900139001", "广东省深圳市宝安区工业园A区",
                EnterpriseRole.SUPPLIER, 780);

        createEnterprise("91110000MA01SUPP2B", "苏州电子元器件有限公司", "李华",
                "13900139002", "江苏省苏州市工业园区科技路88号",
                EnterpriseRole.SUPPLIER, 750);

        createEnterprise("91110000MA01SUPP3C", "东莞市模具制造有限公司", "王强",
                "13900139003", "广东省东莞市长安镇工业区",
                EnterpriseRole.SUPPLIER, 720);

        createEnterprise("91110000MA01SUPP4D", "杭州软件科技有限公司", "赵伟",
                "13900139004", "浙江省杭州市西湖区文三路100号",
                EnterpriseRole.SUPPLIER, 800);

        // 金融机构
        createEnterprise("91110000MA01BANK1F", "中国工商银行深圳分行", "陈行长",
                "13700137001", "广东省深圳市福田区深南大道1000号",
                EnterpriseRole.FINANCIAL_INST, 980);

        createEnterprise("91110000MA01BANK2G", "招商银行总行", "田惠宇",
                "13700137002", "广东省深圳市福田区招商银行大厦",
                EnterpriseRole.FINANCIAL_INST, 970);

        // 监管机构
        createEnterprise("91110000MA01REGU1H", "深圳市金融监督管理局", "刘局长",
                "13600136001", "广东省深圳市福田区市民中心",
                EnterpriseRole.REGULATOR, 1000);

        log.info("✓ 企业数据初始化完成，共 {} 条", enterpriseRepository.count());
    }

    private void initUsers() {
        if (userRepository.count() > 0) {
            log.info("✓ 用户数据已存在 ({} 条)", userRepository.count());
            return;
        }

        log.info("→ 初始化用户数据...");

        String encodedPassword = passwordEncoder.encode("123456");

        // 系统管理员
        createUser("admin", encodedPassword, "系统管理员", "13000000000", "admin@scf.com", null, UserRole.ADMIN);

        // 核心企业用户
        Enterprise huawei = enterpriseRepository.findByCreditCode("91110000MA01ABCD1X").orElse(null);
        Enterprise zhongche = enterpriseRepository.findByCreditCode("91110000MA01ABCD2Y").orElse(null);
        if (huawei != null) createUser("huawei_admin", encodedPassword, "华为管理员", "13800138001", "admin@huawei.com", huawei, UserRole.MANAGER);
        if (zhongche != null) createUser("zhongche_admin", encodedPassword, "中车管理员", "13800138002", "admin@crrc.com", zhongche, UserRole.MANAGER);

        // 供应商用户
        Enterprise supplier1 = enterpriseRepository.findByCreditCode("91110000MA01SUPP1A").orElse(null);
        Enterprise supplier2 = enterpriseRepository.findByCreditCode("91110000MA01SUPP2B").orElse(null);
        Enterprise supplier3 = enterpriseRepository.findByCreditCode("91110000MA01SUPP3C").orElse(null);
        Enterprise supplier4 = enterpriseRepository.findByCreditCode("91110000MA01SUPP4D").orElse(null);
        if (supplier1 != null) createUser("supplier1", encodedPassword, "精密零件管理员", "13900139001", "admin@jingmi.com", supplier1, UserRole.MANAGER);
        if (supplier2 != null) createUser("supplier2", encodedPassword, "电子元器件管理员", "13900139002", "admin@dianzi.com", supplier2, UserRole.MANAGER);
        if (supplier3 != null) createUser("supplier3", encodedPassword, "模具制造管理员", "13900139003", "admin@moju.com", supplier3, UserRole.MANAGER);
        if (supplier4 != null) createUser("supplier4", encodedPassword, "软件科技管理员", "13900139004", "admin@ruanjian.com", supplier4, UserRole.MANAGER);

        // 金融机构用户
        Enterprise icbc = enterpriseRepository.findByCreditCode("91110000MA01BANK1F").orElse(null);
        Enterprise cmb = enterpriseRepository.findByCreditCode("91110000MA01BANK2G").orElse(null);
        if (icbc != null) createUser("icbc_admin", encodedPassword, "工商银行管理员", "13700137001", "admin@icbc.com", icbc, UserRole.MANAGER);
        if (cmb != null) createUser("cmb_admin", encodedPassword, "招商银行管理员", "13700137002", "admin@cmb.com", cmb, UserRole.MANAGER);

        // 监管机构用户
        Enterprise regulator = enterpriseRepository.findByCreditCode("91110000MA01REGU1H").orElse(null);
        if (regulator != null) createUser("regulator", encodedPassword, "监管员", "13600136001", "admin@jrjg.gov.cn", regulator, UserRole.MANAGER);

        log.info("✓ 用户数据初始化完成，共 {} 条", userRepository.count());
    }

    private void initReceivables() {
        long currentCount = receivableRepository.count();
        if (currentCount >= 8) {
            log.info("✓ 应收账款数据已存在 ({} 条)", currentCount);
            return;
        }

        if (!blockchainAvailable) {
            log.warn("! 区块链服务不可用，跳过应收账款初始化");
            return;
        }

        if (currentCount > 0) {
            log.info("→ 应收账款数据不完整 ({} 条)，补充初始化...", currentCount);
        }

        log.info("→ 初始化应收账款数据...");

        Enterprise huawei = enterpriseRepository.findByCreditCode("91110000MA01ABCD1X").orElse(null);
        Enterprise zhongche = enterpriseRepository.findByCreditCode("91110000MA01ABCD2Y").orElse(null);
        Enterprise supplier1 = enterpriseRepository.findByCreditCode("91110000MA01SUPP1A").orElse(null);
        Enterprise supplier2 = enterpriseRepository.findByCreditCode("91110000MA01SUPP2B").orElse(null);
        Enterprise supplier3 = enterpriseRepository.findByCreditCode("91110000MA01SUPP3C").orElse(null);
        Enterprise supplier4 = enterpriseRepository.findByCreditCode("91110000MA01SUPP4D").orElse(null);

        if (huawei == null || supplier1 == null) {
            log.warn("! 企业数据不完整，跳过应收账款初始化");
            return;
        }

        // ========== 华为签发的应收账款 ==========
        // 已确权 - 可以融资
        createReceivable(huawei, supplier1,
                new BigDecimal("5000000.00"),
                LocalDate.now().minusDays(20),
                LocalDate.now().plusMonths(6),
                new BigDecimal("0.0600"),
                TokenStatus.CONFIRMED,
                "华为2026年Q1零件采购款");

        createReceivable(huawei, supplier2,
                new BigDecimal("3000000.00"),
                LocalDate.now().minusDays(15),
                LocalDate.now().plusMonths(5),
                new BigDecimal("0.0580"),
                TokenStatus.CONFIRMED,
                "华为2026年Q1电子元器件采购款");

        createReceivable(huawei, supplier4,
                new BigDecimal("2000000.00"),
                LocalDate.now().minusDays(5),
                LocalDate.now().plusMonths(6),
                new BigDecimal("0.0550"),
                TokenStatus.CONFIRMED,
                "华为软件开发服务费");

        // 待确权
        createReceivable(huawei, supplier1,
                new BigDecimal("1500000.00"),
                LocalDate.now(),
                LocalDate.now().plusMonths(4),
                new BigDecimal("0.0650"),
                TokenStatus.CREATED,
                "华为新一批零件采购款（待确权）");

        // 已融资
        createReceivable(huawei, supplier2,
                new BigDecimal("2500000.00"),
                LocalDate.now().minusDays(30),
                LocalDate.now().plusMonths(3),
                new BigDecimal("0.0580"),
                TokenStatus.FINANCED,
                "华为元器件采购款（已融资）");

        // ========== 中车签发的应收账款 ==========
        if (zhongche != null && supplier3 != null) {
            createReceivable(zhongche, supplier3,
                    new BigDecimal("8000000.00"),
                    LocalDate.now().minusDays(25),
                    LocalDate.now().plusMonths(5),
                    new BigDecimal("0.0620"),
                    TokenStatus.CONFIRMED,
                    "中车2026年模具采购款");

            createReceivable(zhongche, supplier1,
                    new BigDecimal("4500000.00"),
                    LocalDate.now().minusDays(3),
                    LocalDate.now().plusMonths(6),
                    new BigDecimal("0.0600"),
                    TokenStatus.CONFIRMED,
                    "中车精密零件采购款");

            // 部分融资
            createReceivable(zhongche, supplier3,
                    new BigDecimal("6000000.00"),
                    LocalDate.now().minusDays(40),
                    LocalDate.now().plusMonths(4),
                    new BigDecimal("0.0610"),
                    TokenStatus.PARTIALLY_FINANCED,
                    "中车车轮组件采购款（部分融资）");
        }

        log.info("✓ 应收账款数据初始化完成，共 {} 条", receivableRepository.count());
    }

    private void initFinancingRecords() {
        long currentCount = financingRecordRepository.count();
        if (currentCount >= 6) {
            log.info("✓ 融资记录已存在 ({} 条)", currentCount);
            return;
        }

        if (!blockchainAvailable) {
            log.warn("! 区块链服务不可用，跳过融资记录初始化");
            return;
        }

        if (currentCount > 0) {
            log.info("→ 融资记录数据不完整 ({} 条)，补充初始化...", currentCount);
        }

        log.info("→ 初始化融资记录...");

        Enterprise icbc = enterpriseRepository.findByCreditCode("91110000MA01BANK1F").orElse(null);
        Enterprise cmb = enterpriseRepository.findByCreditCode("91110000MA01BANK2G").orElse(null);
        Enterprise supplier1 = enterpriseRepository.findByCreditCode("91110000MA01SUPP1A").orElse(null);
        Enterprise supplier2 = enterpriseRepository.findByCreditCode("91110000MA01SUPP2B").orElse(null);
        Enterprise supplier3 = enterpriseRepository.findByCreditCode("91110000MA01SUPP3C").orElse(null);

        if (icbc == null || supplier1 == null) {
            log.warn("! 企业数据不完整，跳过融资记录初始化");
            return;
        }

        // 通过 description 查找应收账款（因为链上 tokenId 不再是硬编码的）
        Receivable receivable1005 = receivableRepository.findByDescription("华为元器件采购款（已融资）").orElse(null);
        Receivable receivable1001 = receivableRepository.findByDescription("华为2026年Q1零件采购款").orElse(null);
        Receivable receivable2003 = receivableRepository.findByDescription("中车车轮组件采购款（部分融资）").orElse(null);
        Receivable receivable1002 = receivableRepository.findByDescription("华为2026年Q1电子元器件采购款").orElse(null);
        Receivable receivable2002 = receivableRepository.findByDescription("中车精密零件采购款").orElse(null);

        // ========== supplier2 (苏州电子元器件) 的融资记录 ==========
        // 1. 已放款的融资
        if (receivable1005 != null && supplier2 != null) {
            createFinancingRecord(receivable1005, icbc, supplier2,
                    new BigDecimal("2500000.00"),
                    new BigDecimal("0.0680"),
                    FinancingStatus.DISBURSED,
                    LocalDateTime.now().minusDays(25));
        }

        // ========== supplier1 (深圳市精密零件) 的融资记录 ==========
        // 1. 待审批的融资
        if (receivable1001 != null && supplier1 != null) {
            createFinancingRecord(receivable1001, icbc, supplier1,
                    new BigDecimal("3000000.00"),
                    new BigDecimal("0.0700"),
                    FinancingStatus.PENDING,
                    null);
        }

        // 2. 已放款的融资（中车的应收账款）
        if (receivable2002 != null && supplier1 != null && cmb != null) {
            createFinancingRecord(receivable2002, cmb, supplier1,
                    new BigDecimal("3500000.00"),
                    new BigDecimal("0.0680"),
                    FinancingStatus.DISBURSED,
                    LocalDateTime.now().minusDays(10));
        }

        // ========== supplier2 (苏州电子元器件) 的另一个融资记录 ==========
        // 待审批的融资
        if (receivable1002 != null && supplier2 != null && cmb != null) {
            createFinancingRecord(receivable1002, cmb, supplier2,
                    new BigDecimal("2000000.00"),
                    new BigDecimal("0.0650"),
                    FinancingStatus.PENDING,
                    null);
        }

        // ========== supplier3 (东莞市模具制造) 的融资记录 ==========
        // 已批准待放款
        if (receivable2003 != null && supplier3 != null && cmb != null) {
            createFinancingRecord(receivable2003, cmb, supplier3,
                    new BigDecimal("4000000.00"),
                    new BigDecimal("0.0720"),
                    FinancingStatus.APPROVED,
                    null);
        }

        // 已放款
        if (receivable2003 != null && supplier3 != null && icbc != null) {
            createFinancingRecord(receivable2003, icbc, supplier3,
                    new BigDecimal("2000000.00"),
                    new BigDecimal("0.0690"),
                    FinancingStatus.DISBURSED,
                    LocalDateTime.now().minusDays(15));
        }

        log.info("✓ 融资记录初始化完成，共 {} 条", financingRecordRepository.count());
    }

    private void initContracts() {
        if (contractRepository.count() > 0) {
            log.info("✓ 合同数据已存在 ({} 条)", contractRepository.count());
            return;
        }

        log.info("→ 初始化合同存证数据...");

        Enterprise huawei = enterpriseRepository.findByCreditCode("91110000MA01ABCD1X").orElse(null);
        Enterprise zhongche = enterpriseRepository.findByCreditCode("91110000MA01ABCD2Y").orElse(null);

        Receivable receivable1001 = receivableRepository.findByDescription("华为2026年Q1零件采购款").orElse(null);
        Receivable receivable1002 = receivableRepository.findByDescription("华为2026年Q1电子元器件采购款").orElse(null);
        Receivable receivable2001 = receivableRepository.findByDescription("中车2026年模具采购款").orElse(null);

        if (huawei != null && receivable1001 != null) {
            createContract("华为采购合同_2026Q1_001.pdf", "pdf", huawei, receivable1001,
                    "华为2026年Q1零件采购合同");
        }

        if (huawei != null && receivable1002 != null) {
            createContract("华为采购合同_2026Q1_002.pdf", "pdf", huawei, receivable1002,
                    "华为2026年Q1电子元器件采购合同");
        }

        if (zhongche != null && receivable2001 != null) {
            createContract("中车模具采购合同_2026.pdf", "pdf", zhongche, receivable2001,
                    "中车2026年模具采购合同");
        }

        log.info("✓ 合同存证数据初始化完成，共 {} 条", contractRepository.count());
    }

    // ========== 辅助方法 ==========

    private void createEnterprise(String creditCode, String name, String legalPerson,
                                   String phone, String address, EnterpriseRole role,
                                   int creditScore) {
        if (enterpriseRepository.findByCreditCode(creditCode).isPresent()) return;

        String blockchainAddress;
        if (blockchainAvailable) {
            try {
                blockchainAddress = blockchainService.createAccount();
            } catch (Exception e) {
                log.error("创建区块链账户失败: {}, 使用离线地址", e.getMessage());
                blockchainAddress = "OFFLINE_" + creditCode;
            }
        } else {
            blockchainAddress = "OFFLINE_" + creditCode;
        }

        Enterprise enterprise = new Enterprise();
        enterprise.setCreditCode(creditCode);
        enterprise.setName(name);
        enterprise.setLegalPerson(legalPerson);
        enterprise.setPhone(phone);
        enterprise.setAddress(address);
        enterprise.setRole(role);
        enterprise.setCreditScore(creditScore);
        enterprise.setBlockchainAddress(blockchainAddress);
        enterprise.setIsActive(true);
        enterprise = enterpriseRepository.save(enterprise);

        // 区块链上注册企业
        if (blockchainAvailable && !blockchainAddress.startsWith("OFFLINE_")) {
            try {
                BlockchainResult<Void> result = blockchainService.registerEnterprise(
                        blockchainAddress, name, creditCode, mapRole(role)
                );
                log.info("企业 {} 已上链注册, 地址: {}, txHash: {}", name, blockchainAddress, result.getTxHash());

                // 非默认信用分（700）需要更新链上信用分
                if (creditScore != 700) {
                    BlockchainResult<Void> scoreResult = blockchainService.updateCreditScore(blockchainAddress, creditScore);
                    log.info("企业 {} 信用分已上链更新为 {}, txHash: {}", name, creditScore, scoreResult.getTxHash());
                }
            } catch (Exception e) {
                log.error("企业 {} 上链注册失败: {}", name, e.getMessage());
            }
        }
    }

    private void createUser(String username, String encodedPassword, String realName,
                            String phone, String email, Enterprise enterprise, UserRole role) {
        if (userRepository.findByUsername(username).isPresent()) return;

        User user = new User();
        user.setUsername(username);
        user.setPassword(encodedPassword);
        user.setRealName(realName);
        user.setPhone(phone);
        user.setEmail(email);
        user.setEnterprise(enterprise);
        user.setRole(role);
        user.setIsActive(true);
        userRepository.save(user);
    }

    private void createReceivable(Enterprise issuer, Enterprise holder,
                                   BigDecimal faceValue, LocalDate issueDate, LocalDate maturityDate,
                                   BigDecimal annualRate, TokenStatus status, String description) {
        // 通过 description 检查是否已存在
        if (receivableRepository.findByDescription(description).isPresent()) return;

        // 链上签发通证
        Long tokenId;
        String issueTxHash;
        try {
            BigInteger chainFaceValue = faceValue.multiply(BigDecimal.valueOf(100)).toBigInteger();
            // FISCO-BCOS 2.x 的 block.timestamp (now) 返回毫秒级时间戳，需要 * 1000
            BigInteger chainMaturityDate = BigInteger.valueOf(
                    maturityDate.atTime(23, 59, 59)
                            .atZone(java.time.ZoneId.systemDefault())
                            .toEpochSecond() * 1000
            );
            BigInteger chainAnnualRate = annualRate.multiply(BigDecimal.valueOf(10000)).toBigInteger();

            BlockchainResult<BigInteger> issueResult = blockchainService.issueToken(
                    holder.getBlockchainAddress(),
                    chainFaceValue,
                    chainMaturityDate,
                    chainAnnualRate,
                    ""
            );
            tokenId = issueResult.getValue().longValue();
            issueTxHash = issueResult.getTxHash();
            log.info("应收账款链上签发成功: {}, tokenId: {}, txHash: {}", description, tokenId, issueTxHash);
        } catch (Exception e) {
            log.error("应收账款链上签发失败: {}, 错误: {}", description, e.getMessage());
            return;
        }

        // 确权（CONFIRMED 及以上状态）
        String confirmTxHash = null;
        if (status != TokenStatus.CREATED) {
            try {
                BlockchainResult<Void> confirmResult = blockchainService.confirmToken(BigInteger.valueOf(tokenId));
                confirmTxHash = confirmResult.getTxHash();
                log.info("应收账款链上确权成功: tokenId: {}, txHash: {}", tokenId, confirmTxHash);
            } catch (Exception e) {
                log.error("应收账款链上确权失败: tokenId: {}, 错误: {}", tokenId, e.getMessage());
            }
        }

        // 保存到数据库
        Receivable receivable = new Receivable();
        receivable.setTokenId(tokenId);
        receivable.setIssuer(issuer);
        receivable.setHolder(holder);
        receivable.setFaceValue(faceValue);
        receivable.setRemainingValue(faceValue);
        receivable.setIssueDate(issueDate);
        receivable.setMaturityDate(maturityDate);
        receivable.setAnnualRate(annualRate);
        receivable.setStatus(status);
        receivable.setIsSplittable(true);
        receivable.setDescription(description);
        receivable.setTxHash(confirmTxHash != null ? confirmTxHash : issueTxHash);
        receivable = receivableRepository.save(receivable);

        // 创建签发流转记录
        TransferRecord record = new TransferRecord();
        record.setToReceivable(receivable);
        record.setFromEnterprise(issuer);
        record.setToEnterprise(holder);
        record.setAmount(faceValue);
        record.setType(TransferType.ISSUE);
        record.setReason("核心企业签发应收账款通证");
        record.setTxHash(issueTxHash);
        transferRecordRepository.save(record);
    }

    private void createFinancingRecord(Receivable receivable, Enterprise financier,
                                        Enterprise borrower, BigDecimal amount, BigDecimal rate,
                                        FinancingStatus targetStatus, LocalDateTime disbursedAt) {
        // 计算折价金额
        long daysToMaturity = ChronoUnit.DAYS.between(LocalDate.now(), receivable.getMaturityDate());
        BigDecimal discount = amount.multiply(rate).multiply(BigDecimal.valueOf(daysToMaturity))
                .divide(BigDecimal.valueOf(365), 2, RoundingMode.HALF_UP);
        BigDecimal actualAmount = amount.subtract(discount);

        // 第一步：链上申请融资
        Long chainRecordId;
        String applyTxHash;
        try {
            BlockchainResult<BigInteger> applyResult = blockchainService.applyFinancing(
                    BigInteger.valueOf(receivable.getTokenId()),
                    financier.getBlockchainAddress(),
                    amount.multiply(BigDecimal.valueOf(100)).toBigInteger(),
                    rate.multiply(BigDecimal.valueOf(10000)).toBigInteger()
            );
            chainRecordId = applyResult.getValue().longValue();
            applyTxHash = applyResult.getTxHash();
            log.info("融资申请上链成功, chainRecordId: {}, txHash: {}", chainRecordId, applyTxHash);
        } catch (Exception e) {
            log.error("融资申请上链失败: {}", e.getMessage());
            return;
        }

        FinancingRecord record = new FinancingRecord();
        record.setRecordId(chainRecordId);
        record.setReceivable(receivable);
        record.setFinancier(financier);
        record.setBorrower(borrower);
        record.setFinancingAmount(amount);
        record.setAnnualRate(rate);
        record.setDiscountAmount(discount);
        record.setActualAmount(actualAmount);
        record.setStatus(FinancingStatus.PENDING);
        record.setIsRepaid(false);
        record.setTxHash(applyTxHash);

        // 第二步：如果目标状态需要审批
        if (targetStatus == FinancingStatus.APPROVED || targetStatus == FinancingStatus.DISBURSED) {
            try {
                BlockchainResult<Void> approveResult = blockchainService.approveFinancing(
                        BigInteger.valueOf(chainRecordId), true
                );
                record.setStatus(FinancingStatus.APPROVED);
                record.setTxHash(approveResult.getTxHash());
                log.info("融资审批上链成功, chainRecordId: {}, txHash: {}", chainRecordId, approveResult.getTxHash());
            } catch (Exception e) {
                log.error("融资审批上链失败: {}", e.getMessage());
            }
        }

        // 第三步：如果目标状态需要放款
        if (targetStatus == FinancingStatus.DISBURSED) {
            try {
                BlockchainResult<Void> disburseResult = blockchainService.disburseFinancing(
                        BigInteger.valueOf(chainRecordId)
                );
                record.setStatus(FinancingStatus.DISBURSED);
                record.setTxHash(disburseResult.getTxHash());
                if (disbursedAt != null) {
                    record.setDisbursedAt(disbursedAt);
                } else {
                    record.setDisbursedAt(LocalDateTime.now());
                }
                log.info("融资放款上链成功, chainRecordId: {}, txHash: {}", chainRecordId, disburseResult.getTxHash());
            } catch (Exception e) {
                log.error("融资放款上链失败: {}", e.getMessage());
            }
        }

        financingRecordRepository.save(record);

        // 如果已放款，创建融资流转记录
        if (record.getStatus() == FinancingStatus.DISBURSED) {
            TransferRecord transferRecord = new TransferRecord();
            transferRecord.setFromReceivable(receivable);
            transferRecord.setFromEnterprise(borrower);
            transferRecord.setToEnterprise(financier);
            transferRecord.setAmount(amount);
            transferRecord.setType(TransferType.FINANCING);
            transferRecord.setReason("融资放款 - 实际金额: ¥" + actualAmount);
            transferRecord.setTxHash(record.getTxHash());
            transferRecordRepository.save(transferRecord);
        }
    }

    private void createContract(String fileName, String fileType, Enterprise uploader,
                                 Receivable receivable, String description) {
        // 生成模拟的PDF文件内容
        String content = "模拟合同内容\n合同编号: " + receivable.getTokenId() + "\n" +
                "签发方: " + receivable.getIssuer().getName() + "\n" +
                "持有方: " + receivable.getHolder().getName() + "\n" +
                "金额: " + receivable.getFaceValue() + "\n" +
                "描述: " + description;
        byte[] fileData = content.getBytes();

        // 计算SHA256哈希
        String fileHash;
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(fileData);
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            fileHash = sb.toString();
        } catch (Exception e) {
            fileHash = java.util.UUID.randomUUID().toString().replace("-", "") +
                       java.util.UUID.randomUUID().toString().replace("-", "");
        }

        Contract contract = new Contract();
        contract.setFileName(fileName);
        contract.setFileType(fileType);
        contract.setFileSize((long) fileData.length);
        contract.setFileHash(fileHash);
        contract.setFileData(fileData);
        contract.setUploaderEnterprise(uploader);
        contract.setReceivable(receivable);
        contract.setDescription(description);
        // 使用应收账款的txHash作为合同的txHash
        contract.setTxHash(receivable.getTxHash());

        contractRepository.save(contract);

        // 同时更新应收账款的合同哈希
        receivable.setContractHash(fileHash);
        receivableRepository.save(receivable);
    }

    private String mapRole(EnterpriseRole role) {
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
