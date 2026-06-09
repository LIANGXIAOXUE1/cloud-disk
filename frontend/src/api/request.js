import axios from 'axios'
import { ElMessage } from 'element-plus'

const service = axios.create({
  baseURL: '/api',
  timeout: 15000
})

service.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      // Sa-Token 配置的 token-name 为 cloud_disk_token
      config.headers['cloud_disk_token'] = token
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

service.interceptors.response.use(
  response => {
    const data = response.data
    if (data.code !== 200) {
      if (data.code === 401) {
        ElMessage.error('登录已过期，请重新登录')
        localStorage.removeItem('token')
        localStorage.removeItem('username')
        window.location.href = '/#/login'
        return Promise.reject(new Error('Unauthorized'))
      }
      ElMessage.error(data.message || 'Request failed')
      return Promise.reject(new Error(data.message || 'Request failed'))
    }
    return data
  },
  error => {
    if (error.response) {
      const { status } = error.response
      if (status === 401) {
        ElMessage.error('Login expired, please login again')
        localStorage.removeItem('token')
        localStorage.removeItem('user')
        window.location.href = '/#/login'
      } else if (status === 403) {
        ElMessage.error('Access denied')
      } else if (status === 500) {
        ElMessage.error('Server error')
      } else {
        ElMessage.error(error.response.data?.message || 'Network error')
      }
    } else {
      ElMessage.error('Network connection failed')
    }
    return Promise.reject(error)
  }
)

export default service