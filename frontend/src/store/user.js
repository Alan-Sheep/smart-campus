import { defineStore } from 'pinia'

/**
 * 用户状态管理
 * 使用 Pinia 管理用户登录状态、用户信息等
 */
export const useUserStore = defineStore('user', {
  // 状态
  state: () => ({
    // 用户信息
    userInfo: null,
    // 登录令牌
    token: uni.getStorageSync('token') || ''
  }),

  // 计算属性
  getters: {
    // 是否已登录
    isLogin: (state) => !!state.token,
    // 获取用户头像
    avatar: (state) => state.userInfo?.avatar || '',
    // 获取用户昵称
    nickname: (state) => state.userInfo?.nickname || '未登录'
  },

  // 方法
  actions: {
    /**
     * 设置登录信息
     * @param {Object} data 登录返回的数据 { token, userInfo }
     */
    setLogin(data) {
      this.token = data.token
      this.userInfo = data.userInfo
      // 保存到本地缓存
      uni.setStorageSync('token', data.token)
      uni.setStorageSync('userInfo', data.userInfo)
    },

    /**
     * 更新用户信息
     * @param {Object} userInfo 用户信息
     */
    setUserInfo(userInfo) {
      this.userInfo = userInfo
      uni.setStorageSync('userInfo', userInfo)
    },

    /**
     * 退出登录
     */
    logout() {
      this.token = ''
      this.userInfo = null
      uni.removeStorageSync('token')
      uni.removeStorageSync('userInfo')
    }
  }
})
