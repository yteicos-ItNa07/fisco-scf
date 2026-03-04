package com.scf.controller;

import com.scf.dto.ResponseDTO.ApiResponse;
import com.scf.dto.ResponseDTO.ContractInfo;
import com.scf.dto.ResponseDTO.ContractVerifyResult;
import com.scf.entity.Contract;
import com.scf.entity.Enterprise;
import com.scf.security.SecurityUtils;
import com.scf.service.ContractService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 电子合同存证控制器
 */
@Slf4j
@RestController
@RequestMapping("/contracts")
@RequiredArgsConstructor
public class ContractController {

    private final ContractService contractService;
    private final SecurityUtils securityUtils;

    /**
     * 上传合同文件
     */
    @PostMapping("/upload")
    public ApiResponse<ContractInfo> uploadContract(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "receivableId", required = false) Long receivableId,
            @RequestParam(value = "description", required = false) String description) {

        Enterprise uploader = securityUtils.getCurrentEnterprise();
        ContractInfo info = contractService.uploadContract(file, uploader, receivableId, description);
        return ApiResponse.success(info);
    }

    /**
     * 获取合同信息
     */
    @GetMapping("/{id}")
    public ApiResponse<ContractInfo> getContractInfo(@PathVariable Long id) {
        return ApiResponse.success(contractService.getContractInfo(id));
    }

    /**
     * 下载合同文件
     */
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadContract(@PathVariable Long id) {
        Contract contract = contractService.downloadContract(id);

        String encodedFileName = URLEncoder.encode(contract.getFileName(), StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");

        ByteArrayResource resource = new ByteArrayResource(contract.getFileData());

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + encodedFileName)
                .contentLength(contract.getFileSize())
                .body(resource);
    }

    /**
     * 验证合同（哈希校验）
     */
    @GetMapping("/{id}/verify")
    public ApiResponse<ContractVerifyResult> verifyContract(@PathVariable Long id) {
        ContractInfo info = contractService.getContractInfo(id);
        boolean valid = contractService.verifyContract(id);

        ContractVerifyResult result = ContractVerifyResult.builder()
                .contractId(id)
                .fileHash(info.getFileHash())
                .valid(valid)
                .message(valid ? "合同验证通过，文件未被篡改" : "合同验证失败，文件可能已被篡改！")
                .verifiedAt(LocalDateTime.now())
                .build();

        return ApiResponse.success(result);
    }

    /**
     * 根据应收账款获取关联合同列表
     */
    @GetMapping("/receivable/{receivableId}")
    public ApiResponse<List<ContractInfo>> getContractsByReceivable(@PathVariable Long receivableId) {
        return ApiResponse.success(contractService.getContractsByReceivable(receivableId));
    }

    /**
     * 获取当前企业上传的所有合同
     */
    @GetMapping("/my")
    public ApiResponse<List<ContractInfo>> getMyContracts() {
        Enterprise enterprise = securityUtils.getCurrentEnterprise();
        return ApiResponse.success(contractService.getContractsByEnterprise(enterprise));
    }

    /**
     * 通过哈希查找合同
     */
    @GetMapping("/hash/{hash}")
    public ApiResponse<ContractInfo> getContractByHash(@PathVariable String hash) {
        return ApiResponse.success(contractService.getContractByHash(hash));
    }

    /**
     * 删除合同
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteContract(@PathVariable Long id) {
        Enterprise operator = securityUtils.getCurrentEnterprise();
        contractService.deleteContract(id, operator);
        return ApiResponse.success(null);
    }
}
