package com.cloud.disk.config;

import cn.dev33.satoken.stp.StpInterface;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.List;

/**
 * Sa-Token 权限认证配置
 */
@Configuration
public class SaTokenConfig implements StpInterface {

    /**
     * 返回指定 loginId 的权限码集合
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        // 默认不分配权限码，业务需要时按用户角色动态返回
        return Collections.emptyList();
    }

    /**
     * 返回指定 loginId 的角色标识集合
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        // 默认无角色
        return Collections.emptyList();
    }
}
