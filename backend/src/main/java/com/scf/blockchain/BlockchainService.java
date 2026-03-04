package com.scf.blockchain;

import com.scf.config.FiscoConfig;
import lombok.extern.slf4j.Slf4j;
import org.fisco.bcos.sdk.client.Client;
import org.fisco.bcos.sdk.crypto.keypair.CryptoKeyPair;
import org.fisco.bcos.sdk.model.TransactionReceipt;
import org.fisco.bcos.sdk.transaction.manager.AssembleTransactionProcessor;
import org.fisco.bcos.sdk.transaction.manager.TransactionProcessorFactory;
import org.fisco.bcos.sdk.transaction.model.dto.CallResponse;
import org.fisco.bcos.sdk.transaction.model.dto.TransactionResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class BlockchainService {

    @Autowired(required = false)
    private Client client;

    @Autowired(required = false)
    private CryptoKeyPair cryptoKeyPair;

    @Autowired
    private FiscoConfig fiscoConfig;

    @Value("${fisco.enabled:false}")
    private boolean fiscoEnabled;

    @Value("${fisco.auto-deploy:false}")
    private boolean autoDeploy;

    private AssembleTransactionProcessor transactionProcessor;
    private boolean available = false;
    private boolean contractsRedeployed = false;

    @PostConstruct
    public void init() {
        if (!fiscoEnabled) {
            log.error("FISCO-BCOS已禁用，所有需要上链的操作将无法执行");
            log.error("请设置fisco.enabled=true并确保区块链节点正常运行");
            return;
        }
        if (client == null || cryptoKeyPair == null) {
            log.error("区块链服务不可用 - Client或CryptoKeyPair未初始化");
            log.error("所有需要上链的操作将无法执行，请检查FISCO-BCOS节点连接");
            return;
        }
        try {
            this.transactionProcessor = TransactionProcessorFactory.createAssembleTransactionProcessor(
                    client, cryptoKeyPair, "abi/", "bin/"
            );
            this.available = true;
            log.info("区块链服务初始化完成，已连接到FISCO-BCOS网络");
            log.info("SDK默认账户地址: {}", cryptoKeyPair.getAddress());

            // 输出区块链时间戳诊断信息
            try {
                BigInteger blockNum = client.getBlockNumber().getBlockNumber();
                log.info("========== 区块链时间诊断 ==========");
                log.info("最新区块号: {}", blockNum);
                log.info("本机系统时间: {} (epoch_sec={})", java.time.Instant.now(), System.currentTimeMillis() / 1000);
                log.info("====================================");
            } catch (Exception diagEx) {
                log.warn("区块链诊断失败: {}", diagEx.getMessage());
            }

            // 自动部署合约
            if (autoDeploy) {
                deployContracts();
            }
        } catch (Exception e) {
            log.error("区块链服务初始化失败: {}", e.getMessage());
        }
    }

    /**
     * 部署新合约并更新地址
     * auto-deploy=true 时始终重新部署，确保使用最新合约代码
     * 部署成功后设置 contractsRedeployed 标志，通知 DataInitializer 清空旧数据
     */
    private void deployContracts() {
        log.info("========== 开始部署合约（auto-deploy=true）==========");
        try {
            // 部署 AccessControl
            TransactionResponse acResponse = transactionProcessor.deployByContractLoader(
                    "AccessControl", new ArrayList<>()
            );
            if (acResponse.getTransactionReceipt() != null) {
                String acAddress = acResponse.getTransactionReceipt().getContractAddress();
                if (acAddress != null && !acAddress.isEmpty()) {
                    fiscoConfig.setAccessControlAddress(acAddress);
                    log.info("AccessControl 合约已部署: {}", acAddress);
                } else {
                    log.error("AccessControl 合约部署失败: 未获取到合约地址");
                }
            }

            // 部署 ReceivablesToken
            TransactionResponse rtResponse = transactionProcessor.deployByContractLoader(
                    "ReceivablesToken", new ArrayList<>()
            );
            if (rtResponse.getTransactionReceipt() != null) {
                String rtAddress = rtResponse.getTransactionReceipt().getContractAddress();
                if (rtAddress != null && !rtAddress.isEmpty()) {
                    fiscoConfig.setReceivablesTokenAddress(rtAddress);
                    log.info("ReceivablesToken 合约已部署: {}", rtAddress);
                } else {
                    log.error("ReceivablesToken 合约部署失败: 未获取到合约地址");
                }
            }

            this.contractsRedeployed = true;
            log.info("========== 合约部署完成 ==========");
            log.info("AccessControl: {}", fiscoConfig.getAccessControlAddress());
            log.info("ReceivablesToken: {}", fiscoConfig.getReceivablesTokenAddress());

            // 部署 Settlement（如果BIN文件存在）
            try {
                TransactionResponse stResponse = transactionProcessor.deployByContractLoader(
                        "Settlement", new ArrayList<>()
                );
                if (stResponse.getTransactionReceipt() != null) {
                    String stAddress = stResponse.getTransactionReceipt().getContractAddress();
                    if (stAddress != null && !stAddress.isEmpty()) {
                        fiscoConfig.setSettlementAddress(stAddress);
                        log.info("Settlement 合约已部署: {}", stAddress);
                    }
                }
            } catch (Exception stEx) {
                log.warn("Settlement 合约部署跳过（BIN文件可能不存在）: {}", stEx.getMessage());
            }

            log.info("Settlement: {}", fiscoConfig.getSettlementAddress());
        } catch (Exception e) {
            log.error("合约自动部署失败: {}", e.getMessage(), e);
        }
    }

    public boolean isAvailable() {
        return available;
    }

    public boolean isContractsRedeployed() {
        return contractsRedeployed;
    }

    private void checkAvailable() {
        if (!available) {
            throw new RuntimeException("区块链服务不可用，请检查FISCO-BCOS网络连接");
        }
    }

    public String createAccount() {
        checkAvailable();
        CryptoKeyPair keyPair = client.getCryptoSuite().createKeyPair();
        return keyPair.getAddress();
    }

    /**
     * 获取SDK默认账户地址（即msg.sender）
     */
    public String getDefaultAccount() {
        checkAvailable();
        return cryptoKeyPair.getAddress();
    }

    private void checkReceipt(TransactionReceipt receipt, String operation) {
        if (receipt == null) {
            throw new RuntimeException(operation + " - 未获取到交易回执");
        }
        String status = receipt.getStatus();
        if (!"0x0".equals(status) && !"0".equals(status)) {
            String msg = receipt.getMessage();
            String output = receipt.getOutput();
            log.error("链上交易失败 [{}]: status={}, message={}, output={}", operation, status, msg, output);
            throw new RuntimeException(operation + " 失败: " + (msg != null ? msg : "status=" + status));
        }
    }

    private String extractTxHash(TransactionResponse response) {
        return response.getTransactionReceipt().getTransactionHash();
    }

    public BlockchainResult<Void> registerEnterprise(String account, String name, String creditCode, String role) throws Exception {
        checkAvailable();
        List<Object> params = new ArrayList<>();
        params.add(account);
        params.add(name);
        params.add(creditCode);
        params.add(roleToBytes32(role));

        log.debug("registerEnterprise 参数: account={}, name={}, creditCode={}, role={}", account, name, creditCode, role);

        TransactionResponse response = transactionProcessor.sendTransactionAndGetResponseByContractLoader(
                "AccessControl",
                fiscoConfig.getAccessControlAddress(),
                "registerEnterprise",
                params
        );

        checkReceipt(response.getTransactionReceipt(), "企业注册");
        log.info("企业注册上链成功: {}", name);
        return BlockchainResult.ofVoid(extractTxHash(response));
    }

    public BlockchainResult<Void> updateCreditScore(String account, Integer score) throws Exception {
        checkAvailable();
        List<Object> params = new ArrayList<>();
        params.add(account);
        params.add(BigInteger.valueOf(score));

        TransactionResponse response = transactionProcessor.sendTransactionAndGetResponseByContractLoader(
                "AccessControl",
                fiscoConfig.getAccessControlAddress(),
                "updateCreditScore",
                params
        );

        checkReceipt(response.getTransactionReceipt(), "更新信用分");
        return BlockchainResult.ofVoid(extractTxHash(response));
    }

    public BlockchainResult<BigInteger> issueToken(String holder, BigInteger faceValue, BigInteger maturityDate,
                                  BigInteger annualRate, String contractHash) throws Exception {
        checkAvailable();
        List<Object> params = new ArrayList<>();
        params.add(holder);
        params.add(faceValue);
        params.add(maturityDate);
        params.add(annualRate);
        params.add(contractHash);

        log.info("issueToken 参数: holder={}, faceValue={}, maturityDate={}, annualRate={}, contractHash={}",
                holder, faceValue, maturityDate, annualRate, contractHash);
        log.info("issueToken maturityDate解析: {} 对应时间 {} (毫秒级, FISCO-BCOS 2.x)", maturityDate,
                java.time.Instant.ofEpochMilli(maturityDate.longValue()));
        log.info("issueToken 当前系统时间: {} (epoch={})", java.time.Instant.now(), System.currentTimeMillis() / 1000);
        log.info("issueToken 合约地址: {}", fiscoConfig.getReceivablesTokenAddress());

        TransactionResponse response = transactionProcessor.sendTransactionAndGetResponseByContractLoader(
                "ReceivablesToken",
                fiscoConfig.getReceivablesTokenAddress(),
                "issueToken",
                params
        );

        checkReceipt(response.getTransactionReceipt(), "签发通证");

        String txHash = extractTxHash(response);

        // 从返回值获取tokenId
        List<Object> returnValues = response.getReturnObject();
        if (returnValues != null && !returnValues.isEmpty()) {
            return new BlockchainResult<>(txHash, (BigInteger) returnValues.get(0));
        }
        // 如果没有返回值，使用tokenCount作为tokenId
        return new BlockchainResult<>(txHash, getTokenCount());
    }

    public BlockchainResult<Void> confirmToken(BigInteger tokenId) throws Exception {
        checkAvailable();
        List<Object> params = new ArrayList<>();
        params.add(tokenId);

        TransactionResponse response = transactionProcessor.sendTransactionAndGetResponseByContractLoader(
                "ReceivablesToken",
                fiscoConfig.getReceivablesTokenAddress(),
                "confirmToken",
                params
        );

        checkReceipt(response.getTransactionReceipt(), "确权通证 tokenId=" + tokenId);
        return BlockchainResult.ofVoid(extractTxHash(response));
    }

    /**
     * 拆分通证
     * 合约允许 owner 或 holder 调用
     */
    @SuppressWarnings("unchecked")
    public BlockchainResult<List<BigInteger>> splitToken(BigInteger tokenId, List<String> recipients, List<BigInteger> amounts) throws Exception {
        checkAvailable();
        List<Object> params = new ArrayList<>();
        params.add(tokenId);
        params.add(recipients);
        params.add(amounts);

        log.info("splitToken 参数: tokenId={}, recipients={}, amounts={}", tokenId, recipients, amounts);

        TransactionResponse response = transactionProcessor.sendTransactionAndGetResponseByContractLoader(
                "ReceivablesToken",
                fiscoConfig.getReceivablesTokenAddress(),
                "splitToken",
                params
        );

        checkReceipt(response.getTransactionReceipt(), "拆分通证 tokenId=" + tokenId);

        String txHash = extractTxHash(response);

        List<Object> returnValues = response.getReturnObject();
        if (returnValues != null && !returnValues.isEmpty()) {
            Object result = returnValues.get(0);
            if (result instanceof List) {
                return new BlockchainResult<>(txHash, (List<BigInteger>) result);
            }
        }
        return new BlockchainResult<>(txHash, new ArrayList<>());
    }

    /**
     * 转让通证
     * 合约允许 owner 或 holder 调用
     */
    public BlockchainResult<Void> transferToken(BigInteger tokenId, String to) throws Exception {
        checkAvailable();
        List<Object> params = new ArrayList<>();
        params.add(tokenId);
        params.add(to);

        TransactionResponse response = transactionProcessor.sendTransactionAndGetResponseByContractLoader(
                "ReceivablesToken",
                fiscoConfig.getReceivablesTokenAddress(),
                "transferToken",
                params
        );

        checkReceipt(response.getTransactionReceipt(), "转让通证 tokenId=" + tokenId);
        return BlockchainResult.ofVoid(extractTxHash(response));
    }

    /**
     * 申请融资
     * 合约允许 owner 或 holder 调用
     */
    public BlockchainResult<BigInteger> applyFinancing(BigInteger tokenId, String financier, BigInteger amount, BigInteger rate) throws Exception {
        checkAvailable();
        List<Object> params = new ArrayList<>();
        params.add(tokenId);
        params.add(financier);
        params.add(amount);
        params.add(rate);

        TransactionResponse response = transactionProcessor.sendTransactionAndGetResponseByContractLoader(
                "ReceivablesToken",
                fiscoConfig.getReceivablesTokenAddress(),
                "applyFinancing",
                params
        );

        checkReceipt(response.getTransactionReceipt(), "融资申请 tokenId=" + tokenId);

        String txHash = extractTxHash(response);

        List<Object> returnValues = response.getReturnObject();
        if (returnValues != null && !returnValues.isEmpty()) {
            return new BlockchainResult<>(txHash, (BigInteger) returnValues.get(0));
        }
        return new BlockchainResult<>(txHash, BigInteger.ZERO);
    }

    /**
     * 审批融资
     * 合约允许 owner 或 financier 调用
     */
    public BlockchainResult<Void> approveFinancing(BigInteger recordId, boolean approved) throws Exception {
        checkAvailable();
        List<Object> params = new ArrayList<>();
        params.add(recordId);
        params.add(approved);

        TransactionResponse response = transactionProcessor.sendTransactionAndGetResponseByContractLoader(
                "ReceivablesToken",
                fiscoConfig.getReceivablesTokenAddress(),
                "approveFinancing",
                params
        );

        checkReceipt(response.getTransactionReceipt(), "融资审批 recordId=" + recordId);
        return BlockchainResult.ofVoid(extractTxHash(response));
    }

    /**
     * 放款
     * 合约允许 owner 或 financier 调用
     */
    public BlockchainResult<Void> disburseFinancing(BigInteger recordId) throws Exception {
        checkAvailable();
        List<Object> params = new ArrayList<>();
        params.add(recordId);

        TransactionResponse response = transactionProcessor.sendTransactionAndGetResponseByContractLoader(
                "ReceivablesToken",
                fiscoConfig.getReceivablesTokenAddress(),
                "disburseFinancing",
                params
        );

        checkReceipt(response.getTransactionReceipt(), "融资放款 recordId=" + recordId);
        return BlockchainResult.ofVoid(extractTxHash(response));
    }

    // ============ Settlement 结算合约方法 ============

    /**
     * 创建结算记录
     */
    public BlockchainResult<BigInteger> createSettlement(BigInteger tokenId, String payer, String payee,
                                                          BigInteger principal, BigInteger interest) throws Exception {
        checkAvailable();
        if (fiscoConfig.getSettlementAddress() == null ||
                fiscoConfig.getSettlementAddress().equals("0x0000000000000000000000000000000000000000")) {
            throw new RuntimeException("Settlement合约未部署");
        }

        List<Object> params = new ArrayList<>();
        params.add(tokenId);
        params.add(payer);
        params.add(payee);
        params.add(principal);
        params.add(interest);

        TransactionResponse response = transactionProcessor.sendTransactionAndGetResponseByContractLoader(
                "Settlement",
                fiscoConfig.getSettlementAddress(),
                "createSettlement",
                params
        );

        checkReceipt(response.getTransactionReceipt(), "创建结算 tokenId=" + tokenId);

        String txHash = extractTxHash(response);
        List<Object> returnValues = response.getReturnObject();
        if (returnValues != null && !returnValues.isEmpty()) {
            return new BlockchainResult<>(txHash, (BigInteger) returnValues.get(0));
        }
        return new BlockchainResult<>(txHash, BigInteger.ZERO);
    }

    /**
     * 确认结算完成
     */
    public BlockchainResult<Void> confirmSettlement(BigInteger settlementId, String txReference) throws Exception {
        checkAvailable();
        List<Object> params = new ArrayList<>();
        params.add(settlementId);
        params.add(txReference);

        TransactionResponse response = transactionProcessor.sendTransactionAndGetResponseByContractLoader(
                "Settlement",
                fiscoConfig.getSettlementAddress(),
                "confirmSettlement",
                params
        );

        checkReceipt(response.getTransactionReceipt(), "确认结算 settlementId=" + settlementId);
        return BlockchainResult.ofVoid(extractTxHash(response));
    }

    /**
     * 创建还款计划
     */
    public BlockchainResult<BigInteger> createRepaymentPlan(BigInteger financingRecordId, BigInteger tokenId,
                                                              String borrower, String financier,
                                                              BigInteger principal, BigInteger interest,
                                                              BigInteger dueDate) throws Exception {
        checkAvailable();
        if (fiscoConfig.getSettlementAddress() == null ||
                fiscoConfig.getSettlementAddress().equals("0x0000000000000000000000000000000000000000")) {
            throw new RuntimeException("Settlement合约未部署");
        }

        List<Object> params = new ArrayList<>();
        params.add(financingRecordId);
        params.add(tokenId);
        params.add(borrower);
        params.add(financier);
        params.add(principal);
        params.add(interest);
        params.add(dueDate);

        TransactionResponse response = transactionProcessor.sendTransactionAndGetResponseByContractLoader(
                "Settlement",
                fiscoConfig.getSettlementAddress(),
                "createRepaymentPlan",
                params
        );

        checkReceipt(response.getTransactionReceipt(), "创建还款计划");

        String txHash = extractTxHash(response);
        List<Object> returnValues = response.getReturnObject();
        if (returnValues != null && !returnValues.isEmpty()) {
            return new BlockchainResult<>(txHash, (BigInteger) returnValues.get(0));
        }
        return new BlockchainResult<>(txHash, BigInteger.ZERO);
    }

    /**
     * 确认还款
     */
    public BlockchainResult<Void> confirmRepayment(BigInteger planId) throws Exception {
        checkAvailable();
        List<Object> params = new ArrayList<>();
        params.add(planId);

        TransactionResponse response = transactionProcessor.sendTransactionAndGetResponseByContractLoader(
                "Settlement",
                fiscoConfig.getSettlementAddress(),
                "confirmRepayment",
                params
        );

        checkReceipt(response.getTransactionReceipt(), "确认还款 planId=" + planId);
        return BlockchainResult.ofVoid(extractTxHash(response));
    }

    public boolean isSettlementAvailable() {
        return available && fiscoConfig.getSettlementAddress() != null
                && !fiscoConfig.getSettlementAddress().equals("0x0000000000000000000000000000000000000000");
    }

    public BigInteger getTokenCount() throws Exception {
        checkAvailable();
        List<Object> params = new ArrayList<>();

        CallResponse response = transactionProcessor.sendCallByContractLoader(
                "ReceivablesToken",
                fiscoConfig.getReceivablesTokenAddress(),
                "getTokenCount",
                params
        );

        List<Object> returnValues = response.getReturnObject();
        if (returnValues != null && !returnValues.isEmpty()) {
            Object result = returnValues.get(0);
            if (result instanceof BigInteger) {
                return (BigInteger) result;
            }
            return new BigInteger(result.toString());
        }
        return BigInteger.ZERO;
    }

    public String getTransactionHash(TransactionReceipt receipt) {
        return receipt.getTransactionHash();
    }

    private byte[] roleToBytes32(String role) {
        byte[] bytes = new byte[32];
        byte[] roleBytes = role.getBytes();
        System.arraycopy(roleBytes, 0, bytes, 0, Math.min(roleBytes.length, 32));
        return bytes;
    }
}
