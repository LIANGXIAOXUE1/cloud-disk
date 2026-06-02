import request from '../request'

export function getRecycleList() {
  return request({ url: '/recycle/list', method: 'get' })
}

export function restoreFile(recycleId) {
  return request({ url: '/recycle/restore', method: 'post', params: { recycleId } })
}

export function batchRestore(recycleIds) {
  return request({ url: '/recycle/batchRestore', method: 'post', data: recycleIds })
}

export function permanentDelete(recycleId) {
  return request({ url: '/recycle/permanentDelete', method: 'post', params: { recycleId } })
}

export function clearRecycle() {
  return request({ url: '/recycle/clear', method: 'post' })
}