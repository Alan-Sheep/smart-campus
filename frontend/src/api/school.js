/**
 * 学校相关接口
 * 学校列表、当前学校、切换学校等
 */
import request from '@/utils/request'

// 学校相关接口路径前缀
const BASE = '/api/school'

export const schoolApi = {
  /**
   * 获取学校列表
   * @returns {Promise<Array>} 学校列表 [{ id, name, ... }]
   */
  getSchoolList: () => request.get(BASE + '/list'),

  /**
   * 获取当前学校信息
   * @returns {Promise<Object>} 当前学校信息
   */
  getCurrentSchool: () => request.get(BASE + '/current'),

  /**
   * 切换学校
   * @param {number} schoolId - 学校ID
   * @returns {Promise<Object>} 切换后的学校信息
   */
  switchSchool: (schoolId) => request.post(BASE + '/switch', { schoolId }),

  /**
   * 搜索学校
   * @param {string} keyword - 搜索关键词
   * @returns {Promise<Array>} 学校列表
   */
  searchSchool: (keyword) => request.get(BASE + '/search', { keyword })
}

export default schoolApi
