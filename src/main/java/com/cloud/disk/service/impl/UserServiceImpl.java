package com.cloud.disk.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.disk.common.exception.BusinessException;
import com.cloud.disk.repository.entity.User;
import com.cloud.disk.repository.mapper.UserMapper;
import com.cloud.disk.service.api.IUserService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.time.LocalDateTime;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Override
    public User getByUsername(String username) {
        return this.getOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
                .eq(User::getStatus, 1));
    }

    @Override
    public boolean register(String username, String password, String nickname, String email) {
        if (getByUsername(username) != null) {
            return false;
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(encryptPassword(password));
        user.setNickname(nickname);
        user.setEmail(email);
        user.setStatus(1);
        user.setTotalSpace(10737418240L);  // 10GB
        user.setUsedSpace(0L);
        return this.save(user);
    }

    @Override
    public User login(String username, String password) {
        User user = getByUsername(username);
        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }
        if (!verifyPassword(password, user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }

        // 自动升级旧 MD5 密码为 BCrypt
        if (!user.getPassword().startsWith("$2")) {
            user.setPassword(encryptPassword(password));
        }

        // Sa-Token 登录
        StpUtil.login(user.getId());

        // 更新最后登录信息
        user.setLastLoginTime(LocalDateTime.now());
        this.updateById(user);

        return user;
    }

    @Override
    public boolean changePassword(Long userId, String oldPassword, String newPassword) {
        User user = this.getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (!verifyPassword(oldPassword, user.getPassword())) {
            throw new BusinessException("原密码错误");
        }
        user.setPassword(encryptPassword(newPassword));
        return this.updateById(user);
    }

    @Override
    public boolean updateUsedSpace(Long userId, Long size) {
        User user = this.getById(userId);
        if (user == null) return false;
        user.setUsedSpace(user.getUsedSpace() + size);
        return this.updateById(user);
    }

    // ==================== 加密策略 ====================

    private String encryptPassword(String password) {
        return encoder.encode(password);
    }

    /**
     * 密码校验：BCrypt 优先，兼容旧 MD5（自动升级）
     */
    private boolean verifyPassword(String rawPassword, String encodedPassword) {
        // BCrypt 密码以 $2a$/$2b$/$2y$ 开头
        if (encodedPassword.startsWith("$2")) {
            return encoder.matches(rawPassword, encodedPassword);
        }
        // 旧版 MD5 兼容
        String md5Hash = DigestUtils.md5DigestAsHex(("cloud-disk:" + rawPassword).getBytes());
        return md5Hash.equals(encodedPassword);
    }
}
