package com.cloud.disk.common.context;

import cn.dev33.satoken.stp.StpUtil;

/**
 * 用户上下文工具类 — 从 Sa-Token 获取当前登录用户信息
 */
public class UserContext {

    /**
     * 获取当前登录用户的 ID
     */
    public static Long getCurrentUserId() {
        return StpUtil.getLoginIdAsLong();
    }

    /**
     * 检查当前是否已登录
     */
    public static boolean isLogin() {
        return StpUtil.isLogin();
    }
}