package com.cloud.disk.common.dto;

import com.cloud.disk.repository.entity.User;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.time.LocalDateTime;

@Data
public class UserDto {

    private Long id;
    private String username;
    private String nickname;
    private String email;
    private String phone;
    private String avatar;
    private Long totalSpace;
    private Long usedSpace;
    private Integer status;
    private LocalDateTime lastLoginTime;
    private LocalDateTime createdAt;

    public static UserDto from(User user) {
        if (user == null) return null;
        UserDto dto = new UserDto();
        BeanUtils.copyProperties(user, dto);
        return dto;
    }
}
