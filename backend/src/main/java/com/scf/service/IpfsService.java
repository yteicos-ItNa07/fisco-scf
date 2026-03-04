package com.scf.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.util.Map;

/**
 * IPFS存证服务 - 通过Pinata API上传文件到IPFS
 */
@Slf4j
@Service
public class IpfsService {

    @Value("${ipfs.pinata.api-key:}")
    private String apiKey;

    @Value("${ipfs.pinata.api-secret:}")
    private String apiSecret;

    @Value("${ipfs.pinata.jwt:}")
    private String jwt;

    @Value("${ipfs.pinata.gateway:https://gateway.pinata.cloud}")
    private String gateway;

    @Value("${ipfs.enabled:false}")
    private boolean enabled;

    private final RestTemplate restTemplate = new RestTemplate();

    private static final String PINATA_PIN_URL = "https://api.pinata.cloud/pinning/pinFileToIPFS";

    @PostConstruct
    public void init() {
        if (enabled) {
            if (jwt != null && !jwt.isEmpty()) {
                log.info("IPFS存证服务已启用 (Pinata, JWT认证)");
            } else if (apiKey != null && !apiKey.isEmpty()) {
                log.info("IPFS存证服务已启用 (Pinata, API Key认证)");
            } else {
                log.warn("IPFS存证服务已启用但未配置认证信息，上传将失败");
            }
        } else {
            log.info("IPFS存证服务未启用，合同将仅存储在本地数据库");
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 上传文件到IPFS (Pinata)
     * @param fileData 文件字节
     * @param fileName 文件名
     * @return IPFS CID (Content Identifier)，失败返回null
     */
    public String uploadToIpfs(byte[] fileData, String fileName) {
        if (!enabled) {
            log.debug("IPFS未启用，跳过上传");
            return null;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            // 优先使用JWT，其次API Key
            if (jwt != null && !jwt.isEmpty()) {
                headers.set("Authorization", "Bearer " + jwt);
            } else {
                headers.set("pinata_api_key", apiKey);
                headers.set("pinata_secret_api_key", apiSecret);
            }

            // 构建multipart请求
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new NamedByteArrayResource(fileData, fileName));

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    PINATA_PIN_URL,
                    HttpMethod.POST,
                    requestEntity,
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String ipfsHash = (String) response.getBody().get("IpfsHash");
                log.info("文件上传IPFS成功: fileName={}, CID={}", fileName, ipfsHash);
                return ipfsHash;
            } else {
                log.error("IPFS上传失败: status={}", response.getStatusCode());
                return null;
            }
        } catch (Exception e) {
            log.error("IPFS上传异常: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 获取IPFS文件的访问URL
     */
    public String getIpfsUrl(String cid) {
        if (cid == null || cid.isEmpty()) return null;
        return gateway + "/ipfs/" + cid;
    }

    /**
     * 自定义ByteArrayResource，支持传入文件名
     */
    private static class NamedByteArrayResource extends ByteArrayResource {
        private final String filename;

        public NamedByteArrayResource(byte[] byteArray, String filename) {
            super(byteArray);
            this.filename = filename;
        }

        @Override
        public String getFilename() {
            return this.filename;
        }
    }
}
