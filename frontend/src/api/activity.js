/**
 * 校园活动相关接口
 * 活动列表、详情、发布、报名等
 */
import request from '@/utils/request'

// 校园活动相关接口路径前缀
const BASE = '/api/activity'

/**
 * 后端字段 → 前端字段适配
 * 后端字段名和前端期望的不一样，这里做统一转换
 */
function adaptActivity(item) {
  if (!item) return item
  // 取第一张图作为封面
  const cover = item.images ? item.images.split(',')[0] : ''
  // 拼接时间：日期 + 开始时间 ~ 结束时间
  const time = item.activityDate
    ? `${item.activityDate} ${item.startTime || ''}${item.endTime ? ' ~ ' + item.endTime : ''}`
    : ''
  return {
    ...item,
    cover,
    time,
    date: item.activityDate,
    startTime: item.startTime,
    endTime: item.endTime,
    joinCount: item.currentParticipants || 0,
    maxCount: item.maxParticipants || 0,
    initiator: '校园用户',
    isJoined: false,
    type: item.activityType
  }
}

function adaptPage(data) {
  if (!data || !data.records) return data
  return {
    ...data,
    records: data.records.map(adaptActivity)
  }
}

export const activityApi = {
  /**
   * 获取活动列表
   * @param {Object} params - { type, keyword, pageNum, pageSize }
   * @returns {Promise<Object>} { records, total, current, size }
   */
  getActivityList: async (params) => {
    const data = await request.get(BASE + '/list', params)
    return adaptPage(data)
  },

  /**
   * 获取活动详情
   * @param {number} id - 活动ID
   * @returns {Promise<Object>} 活动详情
   */
  getActivityDetail: async (id) => {
    const data = await request.get(BASE + '/detail', { id })
    return adaptActivity(data)
  },

  /**
   * 发布活动
   * @param {Object} data - 活动信息
   * @returns {Promise<Object>} 发布后的活动信息
   */
  publishActivity: (data) => request.post(BASE + '/create', data),

  /**
   * 报名活动
   * @param {number} activityId - 活动ID
   * @returns {Promise<Object>} 报名结果
   */
  joinActivity: (activityId) => request.post(BASE + '/signup', { activityId }),

  /**
   * 取消报名
   * @param {number} activityId - 活动ID
   * @returns {Promise<Object>} 取消结果
   */
  cancelJoin: (activityId) => request.post(BASE + '/cancelSignup', { activityId }),

  /**
   * 获取我报名的活动列表
   * @param {Object} params - { pageNum, pageSize }
   * @returns {Promise<Object>} { records, total }
   */
  getMyActivities: async (params) => {
    const data = await request.get(BASE + '/mySignup', params)
    return adaptPage(data)
  },

  /**
   * 获取活动报名人员列表
   * @param {number} id - 活动ID
   * @returns {Promise<Array>} 报名人员列表
   * @note 后端暂无此接口，保留供前端扩展
   */
  getJoiners: (id) => request.get(BASE + '/joiners/' + id)
}

export default activityApi
