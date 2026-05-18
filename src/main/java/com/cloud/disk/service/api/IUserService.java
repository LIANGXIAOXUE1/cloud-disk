package com.cloud.disk.service.api;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cloud.disk.repository.entity.User;

public interface IUserService extends IService<User> {
    User getByUsername(String username);
    boolean register(String username, String password, String nickname, String email);
    User login(String username, String password);
    boolean changePassword(Long userId, String oldPassword, String newPassword);
    boolean updateUsedSpace(Long userId, Long size);
}