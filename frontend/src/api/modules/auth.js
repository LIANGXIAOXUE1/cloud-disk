import request from '../request'

export function login(data) {
  return request({ url: '/auth/login', method: 'post', params: data })
}

export function register(data) {
  return request({ url: '/auth/register', method: 'post', params: data })
}

export function changePassword(data) {
  return request({ url: '/auth/changePassword', method: 'post', params: data })
}

export function getUserInfo(userId) {
  return request({ url: '/auth/userInfo', method: 'get', params: { userId } })
}