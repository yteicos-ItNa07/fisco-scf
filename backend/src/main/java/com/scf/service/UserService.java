package com.scf.service;

import com.scf.dto.RequestDTO.*;
import com.scf.dto.ResponseDTO.*;
import com.scf.entity.Enterprise;
import com.scf.entity.User;
import com.scf.exception.BusinessException;
import com.scf.repository.EnterpriseRepository;
import com.scf.repository.UserRepository;
import com.scf.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final EnterpriseRepository enterpriseRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public UserInfo register(UserRegister request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException("用户名已存在");
        }

        Enterprise enterprise = enterpriseRepository.findById(request.getEnterpriseId())
                .orElseThrow(() -> new BusinessException("企业不存在"));

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRealName(request.getRealName());
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setEnterprise(enterprise);
        user.setRole(User.UserRole.USER);

        user = userRepository.save(user);
        log.info("用户注册成功: {}", user.getUsername());

        return buildUserInfo(user);
    }

    public LoginResponse login(UserLogin request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BusinessException("用户名或密码错误"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }

        if (!user.getIsActive()) {
            throw new BusinessException("用户已被禁用");
        }

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        String token = jwtTokenProvider.generateToken(
                user.getId(),
                user.getUsername(),
                user.getRole().name()
        );

        log.info("用户登录成功: {}", user.getUsername());

        return LoginResponse.builder()
                .token(token)
                .user(buildUserInfo(user))
                .build();
    }

    public UserInfo getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        return buildUserInfo(user);
    }

    public List<UserInfo> getUsersByEnterprise(Long enterpriseId) {
        Enterprise enterprise = enterpriseRepository.findById(enterpriseId)
                .orElseThrow(() -> new BusinessException("企业不存在"));
        return userRepository.findByEnterprise(enterprise).stream()
                .map(this::buildUserInfo)
                .collect(Collectors.toList());
    }

    private UserInfo buildUserInfo(User user) {
        return UserInfo.builder()
                .id(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .role(user.getRole())
                .enterprise(EnterpriseInfo.fromEntity(user.getEnterprise()))
                .build();
    }
}
