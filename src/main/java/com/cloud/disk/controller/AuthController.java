package com.cloud.disk.controller;

import com.cloud.disk.service.api.IUserService;
import com.cloud.disk.repository.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private IUserService userService;

    @PostMapping("/register")
    public String register(@RequestParam String username,
                           @RequestParam String password,
                           @RequestParam String nickname,
                           @RequestParam(required = false) String email) {
        boolean ok = userService.register(username, password, nickname, email);
        return ok ? "OK" : "USERNAME_EXISTS";
    }

    @PostMapping("/login")
    public User login(@RequestParam String username, @RequestParam String password) {
        return userService.login(username, password);
    }

    @PostMapping("/changePassword")
    public String changePassword(@RequestParam Long userId,
                                 @RequestParam String oldPassword,
                                 @RequestParam String newPassword) {
        boolean ok = userService.changePassword(userId, oldPassword, newPassword);
        return ok ? "OK" : "FAILED";
    }

    @GetMapping("/userInfo")
    public User getUserInfo(@RequestParam Long userId) {
        User user = userService.getById(userId);
        if (user != null) user.setPassword(null);
        return user;
    }
}