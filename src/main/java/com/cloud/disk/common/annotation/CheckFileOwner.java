package com.cloud.disk.common.annotation;

import java.lang.annotation.*;

/**
 * 文件归属权限校验注解。
 * 标注在 Controller 方法上，自动校验传入的文件 ID 是否属于当前登录用户。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CheckFileOwner {

    /**
     * 方法参数中承载文件 ID 的参数名，默认 "fileId"。
     * 支持 Long（单文件）或 List&lt;Long&gt;（批量）。
     */
    String paramName() default "fileId";
}