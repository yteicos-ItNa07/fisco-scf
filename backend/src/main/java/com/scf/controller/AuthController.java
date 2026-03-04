package com.scf.controller;

import com.scf.dto.RequestDTO.*;
import com.scf.dto.ResponseDTO.*;
import com.scf.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody UserLogin request) {
        return ApiResponse.success(userService.login(request));
    }

    @PostMapping("/register")
    public ApiResponse<UserInfo> register(@Valid @RequestBody UserRegister request) {
        return ApiResponse.success(userService.register(request));
    }
}
