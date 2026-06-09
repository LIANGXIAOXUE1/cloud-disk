import request from '../request'

/**
 * 用户登录
 */
export function login(data) {
  return request({ url: '/auth/login', method: 'post', data })
}

/**
 * 用户注册
 */
export function register(data) {
  return request({ url: '/auth/register', method: 'post', data })
}

/**
 * 修改密码
 */
export function changePassword(data) {
  return request({ url: '/auth/changePassword', method: 'post', data })
}

/**
 * 获取当前登录用户信息（无需传参，后端从Token中获取）
 */
export function getUserInfo() {
  return request({ url: '/auth/userInfo', method: 'get' })
}