/**
 * 用户相关接口
 * 登录、注册、用户信息等
 */
import request from '@/utils/request'

// 用户相关接口路径前缀
const BASE = '/api/user'

export const userApi = {
  /**
   * 手机号验证码登录
   * @param {Object} data - { phone, code }
   * @returns {Promise<Object>} 返回 { token, userInfo }
   */
  login: (data) => request.post(BASE + '/login', data),

  /**
   * 微信登录
   * 后端同一个 /login 接口，传 wxCode 参数就是微信登录
   * @param {Object} data - { wxCode }
   * @returns {Promise<Object>} 返回 { token, userInfo }
   */
  wxLogin: (data) => request.post(BASE + '/login', data),

  /**
   * 发送手机验证码
   * @param {string} phone - 手机号
   * @returns {Promise<string>} 返回验证码（测试用，真实项目不返回）
   */
  sendCode: (phone) => request.get(BASE + '/sendCode', { phone }),

  /**
   * 获取当前登录用户信息
   * @returns {Promise<Object>} 用户信息对象
   */
  getUserInfo: () => request.get(BASE + '/info'),

  /**
   * 更新用户信息
   * @param {Object} data - 用户信息 { nickname, avatar, ... }
   * @returns {Promise<Object>} 更新后的用户信息
   */
  updateUserInfo: (data) => request.put(BASE + '/info', data),

  /**
   * 修改密码
   * @param {Object} data - { oldPassword, newPassword }
   * @returns {Promise}
   */
  changePassword: (data) => request.put(BASE + '/password', data),

  /**
   * 退出登录
   * @returns {Promise}
   */
  logout: () => request.post(BASE + '/logout')
}

export default userApi
