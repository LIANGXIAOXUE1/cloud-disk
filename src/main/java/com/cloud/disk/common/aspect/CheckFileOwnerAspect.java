package com.cloud.disk.common.aspect;

import com.cloud.disk.common.annotation.CheckFileOwner;
import com.cloud.disk.common.context.UserContext;
import com.cloud.disk.common.exception.BusinessException;
import com.cloud.disk.repository.entity.FileInfo;
import com.cloud.disk.service.api.IFileService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * {@link CheckFileOwner} 注解的 AOP 切面实现。
 * 在方法执行前校验文件归属：文件必须存在且属于当前登录用户。
 */
@Aspect
@Component
public class CheckFileOwnerAspect {

    @Autowired
    private IFileService fileService;

    @Around("@annotation(checkFileOwner)")
    public Object checkOwner(ProceedingJoinPoint joinPoint, CheckFileOwner checkFileOwner) throws Throwable {
        Long userId = UserContext.getCurrentUserId();
        String paramName = checkFileOwner.paramName();

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        for (int i = 0; i < paramNames.length; i++) {
            if (paramNames[i].equals(paramName)) {
                Object arg = args[i];
                if (arg instanceof Long fileId) {
                    checkSingle(fileId, userId);
                } else if (arg instanceof List<?> list) {
                    for (Object item : list) {
                        if (item instanceof Long fid) {
                            checkSingle(fid, userId);
                        }
                    }
                }
                break;
            }
        }

        return joinPoint.proceed();
    }

    private void checkSingle(Long fileId, Long userId) {
        FileInfo file = fileService.getById(fileId);
        if (file == null) {
            throw new BusinessException(404, "文件不存在: id=" + fileId);
        }
        if (!file.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权操作该文件: id=" + fileId);
        }
    }
}