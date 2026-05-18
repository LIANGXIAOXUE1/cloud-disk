package com.cloud.disk;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 云盘系统 - 启动类
 */
@SpringBootApplication
@MapperScan("com.cloud.disk.repository")
public class CloudDiskApplication {

    public static void main(String[] args) {
        SpringApplication.run(CloudDiskApplication.class, args);
    }
}