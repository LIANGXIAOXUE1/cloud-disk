package com.cloud.disk.controller;

import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import com.cloud.disk.common.dto.LoginDto;
import com.cloud.disk.common.dto.RegisterDto;
import com.cloud.disk.common.dto.UserDto;
import com.cloud.disk.common.exception.BusinessException;
import com.cloud.disk.common.result.Result;
import com.cloud.disk.repository.entity.User;
import com.cloud.disk.service.api.IUserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private IUserService userService;

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterDto dto) {
        boolean ok = userService.register(dto.getUsername(), dto.getPassword(),
                dto.getNickname(), dto.getEmail());
        if (!ok) {
            throw new BusinessException("用户名已存在");
        }
        return Result.success(null);
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginDto dto) {
        User user = userService.login(dto.getUsername(), dto.getPassword());

        // 获取 Sa-Token 信息
        SaTokenInfo tokenInfo = StpUtil.getTokenInfo();

        Map<String, Object> result = new HashMap<>();
        result.put("token", tokenInfo.getTokenValue());
        result.put("user", UserDto.from(user));
        return Result.success(result);
    }

    /**
     * 用户登出
     */
    @PostMapping("/logout")
    public Result<Void> logout() {
        StpUtil.logout();
        return Result.success(null);
    }

    /**
     * 获取当前登录用户信息
     */
    @GetMapping("/userInfo")
    public Result<UserDto> getUserInfo() {
        long userId = StpUtil.getLoginIdAsLong();
        User user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return Result.success(UserDto.from(user));
    }

    /**
     * 修改密码
     */
    @PostMapping("/changePassword")
    public Result<Void> changePassword(@RequestBody Map<String, String> body) {
        long userId = StpUtil.getLoginIdAsLong();
        userService.changePassword(userId, body.get("oldPassword"), body.get("newPassword"));
        return Result.success(null);
    }

    /**
     * 更新个人信息（昵称/邮箱）
     */
    @PutMapping("/profile")
    public Result<UserDto> updateProfile(@RequestBody Map<String, String> body) {
        long userId = StpUtil.getLoginIdAsLong();
        User user = userService.getById(userId);
        if (user == null) throw new BusinessException("用户不存在");
        if (body.containsKey("nickname")) user.setNickname(body.get("nickname"));
        if (body.containsKey("email")) user.setEmail(body.get("email"));
        userService.updateById(user);
        return Result.success(UserDto.from(user));
    }
}
