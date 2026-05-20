import request from '../request'

export function getFileList(userId, parentId) {
  return request({ url: '/file/list', method: 'get', params: { userId, parentId } })
}

export function searchFiles(userId, fileName) {
  return request({ url: '/file/search', method: 'get', params: { userId, fileName } })
}

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