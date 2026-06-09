package com.cloud.disk.service.api;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cloud.disk.common.result.PageResult;
import com.cloud.disk.repository.entity.FileInfo;

import java.util.List;

/**
 * 文件服务接口
 */
public interface IFileService extends IService<FileInfo> {

    /**
     * 按父目录分页列出文件
     */
    PageResult<FileInfo> listByParentId(Long userId, Long parentId, int page, int pageSize);

    /**
     * 按文件名模糊搜索（当前用户）
     */
    List<FileInfo> searchByName(Long userId, String fileName);

    /**
     * 创建文件夹
     */
    boolean createFolder(Long userId, String folderName, Long parentId);

    /**
     * 重命名文件/文件夹
     */
    boolean rename(Long fileId, String newName);

    /**
     * 移动文件/文件夹到新父目录
     */
    boolean move(Long fileId, Long newParentId);

    /**
     * 复制文件/文件夹到目标目录
     */
    boolean copy(Long fileId, Long targetParentId);

    /**
     * 删除文件到回收站（同时写入 recycle_bin 表）
     */
    boolean deleteToRecycle(Long fileId);

    /**
     * 批量删除文件到回收站
     */
    boolean batchDeleteToRecycle(List<Long> fileIds);

    /**
     * 获取文件下载 URL
     */
    String getDownloadUrl(Long fileId);

    /**
     * 获取文件预览 URL
     */
    String getPreviewUrl(Long fileId);

    /**
     * 获取用户已用空间（字节）
     */
    Long getUsedSpace(Long userId);

    /**
     * Get storage statistics (total/used space + file type breakdown)
     */
    java.util.Map<String, Object> getStorageStats(Long userId);

    /**
     * 检查指定目录下是否已存在同名文件/文件夹
     * @param excludeFileId 排除的文件 ID（重命名时传入原 ID，新建时传 null）
     */
    boolean existsSameName(Long userId, Long parentId, String fileName, Long excludeFileId);

    // ========== 文件上传（本地存储版）==========

    /**
     * 普通小文件上传
     * @param userId   当前用户 ID
     * @param file     上传的文件
     * @param parentId 目标父目录 ID（根目录传 null）
     * @return 保存后的 FileInfo 记录
     */
    com.cloud.disk.repository.entity.FileInfo uploadFile(Long userId, org.springframework.web.multipart.MultipartFile file, Long parentId) throws Exception;

    /**
     * 初始化分片上传任务
     * @param userId       当前用户 ID
     * @param fileName     原始文件名
     * @param fileSize     文件总大小（字节）
     * @param totalChunks  总分片数
     * @param parentId     目标父目录 ID（根目录传 null）
     * @return uploadId 上传任务 ID
     */
    String initChunkUpload(Long userId, String fileName, Long fileSize, Integer totalChunks, Long parentId);

    /**
     * 上传单个分片
     * @param uploadId   上传任务 ID
     * @param chunkIndex 分片索引（从 0 开始）
     * @param chunkFile  分片文件数据
     */
    void uploadChunk(String uploadId, int chunkIndex, org.springframework.web.multipart.MultipartFile chunkFile) throws Exception;

    /**
     * 查询分片上传进度
     * @param uploadId 上传任务 ID
     * @return 已完成的分片索引列表
     */
    java.util.List<Integer> getChunkProgress(String uploadId);

    /**
     * 合并所有分片，写入 file_info 表
     * @param userId   当前用户 ID
     * @param uploadId 上传任务 ID
     * @return 合并后的 FileInfo 记录
     */
    com.cloud.disk.repository.entity.FileInfo mergeChunks(Long userId, String uploadId) throws Exception;
}