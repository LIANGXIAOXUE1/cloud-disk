package com.cloud.disk.service.storage;

import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;

/**
 * 本地存储服务接口
 * 封装所有本地磁盘读写操作，供 FileServiceImpl 调用
 */
public interface ILocalStorageService {

    /**
     * 保存普通上传的小文件
     *
     * @param userId   当前用户 ID
     * @param file     上传的文件
     * @param fileName 原始文件名（含扩展名）
     * @return 保存到磁盘后的绝对路径，如 D:/cloud-disk-files/upload/1/abc123.pdf
     */
    String saveFile(Long userId, MultipartFile file, String fileName) throws Exception;

    /**
     * 保存文件夹占位记录（文件夹不需要实际文件，仅返回逻辑路径）
     *
     * @param userId     当前用户 ID
     * @param folderName 文件夹名称
     * @param parentId   父目录 ID（根目录时为 null）
     * @return 逻辑路径字符串，如 /upload/1/folder_xxx
     */
    String saveFolder(Long userId, String folderName, Long parentId);

    /**
     * 初始化分片上传任务
     *
     * @param uploadId    唯一上传 ID（UUID）
     * @param fileName    原始文件名
     * @param totalChunks 总分片数
     * @param parentId    目标父目录 ID（可为 null）
     */
    void initChunkUpload(String uploadId, String fileName, int totalChunks, Long parentId);

    /**
     * 保存单个分片到临时目录
     *
     * @param uploadId   初始化时返回的 uploadId
     * @param chunkIndex 当前分片索引（从 0 开始）
     * @param chunkFile  分片文件数据
     */
    void saveChunk(String uploadId, int chunkIndex, MultipartFile chunkFile) throws Exception;

    /**
     * 查询已成功上传的分片索引列表
     *
     * @param uploadId 上传任务 ID
     * @return 已完成的分片索引列表（升序）
     */
    List<Integer> getUploadedChunks(String uploadId);

    /**
     * 合并所有分片为完整文件
     *
     * @param uploadId 上传任务 ID
     * @param userId   当前用户 ID
     * @param fileName 原始文件名（用于确定扩展名和最终文件名）
     * @return 合并后文件的磁盘绝对路径
     */
    String mergeChunks(String uploadId, Long userId, String fileName) throws Exception;

    /**
     * 清理分片上传的临时目录
     *
     * @param uploadId 上传任务 ID
     */
    void cleanTemp(String uploadId);

    /**
     * 处理同名冲突：若目标路径已存在，自动加 (1)、(2) 后缀
     *
     * @param targetPath 预期保存路径
     * @return 实际可用的路径（若冲突则已加后缀）
     */
    String resolveDuplicate(String targetPath);

    /**
     * 根据文件 ID 和磁盘路径构造 HTTP 下载/预览 URL
     *
     * @param fileId   文件 ID
     * @param filePath 磁盘绝对路径
     * @return URL 路径，如 /api/file/download/123
     */
    String buildAccessUrl(Long fileId, String filePath);

    /**
     * 从分片上传的元数据文件中读取原始文件名
     *
     * @param uploadId 上传任务 ID
     * @return 原始文件名，读取失败返回 null
     */
    String getOriginalFileName(String uploadId);

    /**
     * 从分片上传的元数据文件中读取目标父目录 ID
     *
     * @param uploadId 上传任务 ID
     * @return 父目录 ID，未记录或读取失败返回 null
     */
    Long getParentId(String uploadId);
}
