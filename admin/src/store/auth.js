import { defineStore } from 'pinia'
import { authApi } from '@/api'

/**
 * 管理员登录态
 * token / 管理员信息持久化到 localStorage
 */
export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('admin_token') || '',
    adminInfo: JSON.parse(localStorage.getItem('admin_info') || 'null')
  }),

  getters: {
    isLogin: (state) => !!state.token,
    nickname: (state) => state.adminInfo?.nickname || state.adminInfo?.username || '管理员',
    avatar: (state) => state.adminInfo?.avatar || '',
    role: (state) => state.adminInfo?.role
  },

  actions: {
    /** 登录 */
    async login(payload) {
      const data = await authApi.login(payload)
      this.token = data.token
      this.adminInfo = data.adminInfo
      localStorage.setItem('admin_token', data.token)
      localStorage.setItem('admin_info', JSON.stringify(data.adminInfo))
      return data
    },

    /** 拉取最新管理员信息 */
    async fetchInfo() {
      const info = await authApi.info()
      this.adminInfo = info
      localStorage.setItem('admin_info', JSON.stringify(info))
      return info
    },

    /** 退出登录 */
    async logout() {
      try {
        await authApi.logout()
      } catch (e) {
        // 忽略退出接口异常，本地状态必须清干净
      }
      this.clear()
    },

    /** 清除本地登录态 */
    clear() {
      this.token = ''
      this.adminInfo = null
      localStorage.removeItem('admin_token')
      localStorage.removeItem('admin_info')
    }
  }
})
