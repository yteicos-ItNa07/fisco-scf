package com.scf.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.fisco.bcos.sdk.BcosSDK;
import org.fisco.bcos.sdk.client.Client;
import org.fisco.bcos.sdk.crypto.keypair.CryptoKeyPair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PreDestroy;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Data
@Configuration
public class FiscoConfig {

    @Value("${fisco.group-id:1}")
    private int groupId;

    @Value("${fisco.contracts.access-control}")
    private String accessControlAddress;

    @Value("${fisco.contracts.receivables-token}")
    private String receivablesTokenAddress;

    @Value("${fisco.contracts.flow-router}")
    private String flowRouterAddress;

    @Value("${fisco.contracts.settlement}")
    private String settlementAddress;

    @Value("${fisco.enabled:true}")
    private boolean fiscoEnabled;

    private BcosSDK bcosSDK;

    @Bean
    @ConditionalOnProperty(name = "fisco.enabled", havingValue = "true", matchIfMissing = false)
    public BcosSDK bcosSDK() {
        try {
            // 获取配置文件路径
            URL configUrl = this.getClass().getClassLoader().getResource("fisco/config.toml");
            if (configUrl == null) {
                throw new RuntimeException("Cannot find fisco/config.toml in classpath");
            }

            // URL解码处理中文路径
            String configFile = URLDecoder.decode(configUrl.getPath(), StandardCharsets.UTF_8.name());

            // 处理Windows路径 (去掉开头的/)
            if (configFile.startsWith("/") && configFile.contains(":")) {
                configFile = configFile.substring(1);
            }

            log.info("Loading FISCO config from: {}", configFile);
            this.bcosSDK = BcosSDK.build(configFile);
            log.info("FISCO-BCOS SDK initialized successfully");
            return bcosSDK;
        } catch (Exception e) {
            log.error("Failed to initialize FISCO-BCOS SDK: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to initialize FISCO-BCOS SDK", e);
        }
    }

    @Bean
    @ConditionalOnProperty(name = "fisco.enabled", havingValue = "true", matchIfMissing = false)
    public Client fiscoClient(BcosSDK bcosSDK) {
        return bcosSDK.getClient(groupId);
    }

    @Bean
    @ConditionalOnProperty(name = "fisco.enabled", havingValue = "true", matchIfMissing = false)
    public CryptoKeyPair cryptoKeyPair(Client client) {
        return client.getCryptoSuite().getCryptoKeyPair();
    }

    @PreDestroy
    public void destroy() {
        if (bcosSDK != null) {
            bcosSDK.stopAll();
        }
    }
}
