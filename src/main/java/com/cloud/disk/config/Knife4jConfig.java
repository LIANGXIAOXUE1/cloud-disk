package com.cloud.disk.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j (Swagger3) 配置类
 */
@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("云盘系统 API 文档")
                        .description("基于 SpringBoot3 + Sa-Token + MyBatis-Plus 的私有云盘系统")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("梁晓丽")
                                .email("liangxiaoxue@example.com")
                                .url("https://github.com/LIANGXIAOXUE1/cloud-disk"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")));
    }
}