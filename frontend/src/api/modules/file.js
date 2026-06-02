import request from '../request'

// ==================== 文件 CRUD ====================

export function getFileList(userId, parentId) {
  return request({ url: '/file/list', method: 'get', params: { userId, parentId } })
}

export function searchFiles(userId, fileName) {
  return request({ url: '/file/search', method: 'get', params: { userId, fileName } })
}
// 注意：后端返回 Result<List<FileInfo>>，axios 拦截器已解包 Result.data，
// 所以本函数返回值即为 FileInfo 数组，调用方应直接使用 res，而非 res.list。

export function createFolder(userId, folderName, parentId) {
  return request({ url: '/file/createFolder', method: 'post', params: { userId, folderName, parentId } })
}

export function renameFile(fileId, newName) {
  return request({ url: '/file/rename', method: 'post', params: { fileId, newName } })
}

export function moveFile(fileId, newParentId) {
  return request({ url: '/file/move', method: 'post', params: { fileId, newParentId } })
}

export function deleteFile(fileId) {
  return request({ url: '/file/delete', method: 'post', params: { fileId } })
}

export function getDownloadUrl(fileId) {
  return request({ url: '/file/downloadUrl', method: 'get', params: { fileId } })
}

// ==================== 文件上传 ====================

/**
 * 普通小文件上传（< 100MB），multipart/form-data
 * @param {File} file 文件对象
 * @param {Number|null} parentId 父目录 ID
 * @param {Function} onProgress 上传进度回调 ({loaded, total})
 * @param {AbortSignal} signal 取消信号
 */
export function uploadFile(file, parentId, onProgress, signal) {
  const formData = new FormData()
  formData.append('file', file)
  if (parentId != null) formData.append('parentId', String(parentId))
  return request({
    url: '/file/upload',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 600000,
    onUploadProgress: onProgress,
    signal
  })
}

/**
 * 初始化分片上传，返回 uploadId
 * @param {String} fileName 文件名
 * @param {Number} fileSize 文件大小（字节）
 * @param {Number} totalChunks 总分片数
 * @param {Number|null} parentId 父目录 ID
 * @returns {Promise<{data: {uploadId: string}}>}
 */
export function initChunkUpload(fileName, fileSize, totalChunks, parentId) {
  return request({
    url: '/file/chunk/init',
    method: 'post',
    data: { fileName, fileSize, totalChunks, parentId }
  })
}

/**
 * 上传单个分片
 * @param {String} uploadId 上传任务 ID
 * @param {Number} chunkIndex 分片索引（从 0 开始）
 * @param {Blob} chunkBlob 分片数据
 * @param {AbortSignal} signal 取消信号
 */
export function uploadChunk(uploadId, chunkIndex, chunkBlob, signal) {
  const formData = new FormData()
  formData.append('uploadId', uploadId)
  formData.append('chunkIndex', String(chunkIndex))
  formData.append('file', chunkBlob, 'chunk_' + chunkIndex)
  return request({
    url: '/file/chunk/upload',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 600000,
    signal
  })
}

/**
 * 查询分片上传进度，获取已完成分片列表（用于断点续传）
 * @param {String} uploadId 上传任务 ID
 * @returns {Promise<{data: {uploadId: string, completedChunks: number[]}}>}
 */
export function getChunkProgress(uploadId) {
  return request({
    url: '/file/chunk/progress',
    method: 'get',
    params: { uploadId }
  })
}

/**
 * 合并分片，生成最终文件
 * @param {String} uploadId 上传任务 ID
 */
export function mergeChunks(uploadId) {
  return request({
    url: '/file/chunk/merge',
    method: 'post',
    data: { uploadId }
  })
}

