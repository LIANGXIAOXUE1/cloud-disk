import { defineStore } from 'pinia'
import { getUserInfo } from '@/api/modules/auth'

/**
 * 用户状态管理
 */
export const useUserStore = defineStore('user', {
  state: () => ({
    id: null,
    username: '',
    nickname: '',
    avatar: '',
    email: '',
    storageUsed: 0,
    storageTotal: 0,
    createdAt: ''
  }),

  getters: {
    isLoggedIn: (state) => !!state.id,
    storagePercent: (state) => {
      if (!state.storageTotal) return 0
      return Math.round((state.storageUsed / state.storageTotal) * 100)
    }
  },

  actions: {
    /**
     * 从后端获取用户信息并写入状态
     */
    async fetchUserInfo() {
      const res = await getUserInfo()
      if (res.code === 200 && res.data) {
        this.id = res.data.id
        this.username = res.data.username
        this.nickname = res.data.nickname || res.data.username
        this.avatar = res.data.avatar || ''
        this.email = res.data.email || ''
        this.storageUsed = res.data.storageUsed || 0
        this.storageTotal = res.data.storageTotal || 0
        this.createdAt = res.data.createdAt || ''
      }
    },

    /**
     * 清空用户状态（退出登录时调用）
     */
    clearUser() {
      this.id = null
      this.username = ''
      this.nickname = ''
      this.avatar = ''
      this.email = ''
      this.storageUsed = 0
      this.storageTotal = 0
      this.createdAt = ''
    }
  }
})