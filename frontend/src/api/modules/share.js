import request from '../request'

export function createShare(fileId, userId, password, expireType) {
  return request({ url: '/share/create', method: 'post', params: { fileId, userId, password, expireType } })
}

export function getShareInfo(shareId) {
  return request({ url: '/share/info', method: 'get', params: { shareId } })
}

export function verifySharePassword(shareId, password) {
  return request({ url: '/share/verify', method: 'post', params: { shareId, password } })
}

export function cancelShare(shareId, userId) {
  return request({ url: '/share/cancel', method: 'post', params: { shareId, userId } })
}

export function getShareList(userId) {
  return request({ url: '/share/list', method: 'get', params: { userId } })
}