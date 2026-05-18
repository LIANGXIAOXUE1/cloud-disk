package com.cloud.disk.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.disk.repository.entity.User;
import com.cloud.disk.repository.mapper.UserMapper;
import com.cloud.disk.service.api.IUserService;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Override
    public User getByUsername(String username) {
        return this.getOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
                .eq(User::getStatus, 1));
    }

    @Override
    public boolean register(String username, String password, String nickname, String email) {
        if (getByUsername(username) != null) return false;
        User user = new User();
        user.setUsername(username);
        user.setPassword(encryptPassword(password));
        user.setNickname(nickname);
        user.setEmail(email);
        user.setStatus(1);
        user.setTotalSpace(10737418240L);
        user.setUsedSpace(0L);
        return this.save(user);
    }

    @Override
    public User login(String username, String password) {
        User user = getByUsername(username);
        if (user != null && verifyPassword(password, user.getPassword())) return user;
        return null;
    }

    @Override
    public boolean changePassword(Long userId, String oldPassword, String newPassword) {
        User user = this.getById(userId);
        if (user == null || !verifyPassword(oldPassword, user.getPassword())) return false;
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

    private String encryptPassword(String password) {
        return DigestUtils.md5DigestAsHex(("cloud-disk:" + password).getBytes());
    }

    private boolean verifyPassword(String rawPassword, String encodedPassword) {
        return encryptPassword(rawPassword).equals(encodedPassword);
    }
}