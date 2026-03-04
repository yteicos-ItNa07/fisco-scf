package com.scf.controller;

import com.scf.dto.RequestDTO.EnterpriseRegister;
import com.scf.dto.ResponseDTO.*;
import com.scf.entity.Enterprise;
import com.scf.service.EnterpriseService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/enterprises")
@RequiredArgsConstructor
public class EnterpriseController {

    private final EnterpriseService enterpriseService;

    @PostMapping
    public ApiResponse<EnterpriseInfo> register(@Valid @RequestBody EnterpriseRegister request) {
        return ApiResponse.success(enterpriseService.register(request));
    }

    @GetMapping
    public ApiResponse<List<EnterpriseInfo>> getAll() {
        return ApiResponse.success(enterpriseService.getAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<EnterpriseInfo> getById(@PathVariable Long id) {
        return ApiResponse.success(enterpriseService.getById(id));
    }

    @GetMapping("/role/{role}")
    public ApiResponse<List<EnterpriseInfo>> getByRole(@PathVariable Enterprise.EnterpriseRole role) {
        return ApiResponse.success(enterpriseService.getByRole(role));
    }

    @PutMapping("/{id}/credit-score")
    public ApiResponse<EnterpriseInfo> updateCreditScore(
            @PathVariable Long id,
            @RequestParam Integer score) {
        return ApiResponse.success(enterpriseService.updateCreditScore(id, score));
    }
}
