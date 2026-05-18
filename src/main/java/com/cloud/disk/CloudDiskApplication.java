package com.cloud.disk;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.cloud.disk.repository.mapper")
public class CloudDiskApplication {
    public static void main(String[] args) {
        SpringApplication.run(CloudDiskApplication.class, args);
    }
}