import request from '../request'

export function createTransferTask(userId, sourceUrl, extractCode, targetPath) {
  return request({ url: '/transfer/create', method: 'post', params: { userId, sourceUrl, extractCode, targetPath } })
}

export function getTransferList(userId) {
  return request({ url: '/transfer/list', method: 'get', params: { userId } })
}

export function getTransferDetail(taskNo) {
  return request({ url: '/transfer/detail', method: 'get', params: { taskNo } })
}

export function cancelTransferTask(taskNo, userId) {
  return request({ url: '/transfer/cancel', method: 'post', params: { taskNo, userId } })
}

export function retryTransferTask(taskNo, userId) {
  return request({ url: '/transfer/retry', method: 'post', params: { taskNo, userId } })
}