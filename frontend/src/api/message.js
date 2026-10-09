/**
 * 消息相关接口
 * 消息列表、系统通知、聊天等
 */
import request from '@/utils/request'

// 消息相关接口路径前缀
const BASE = '/api/message'

export const messageApi = {
  /**
   * 获取消息列表（按类型）
   * @param {Object} params - { type, pageNum, pageSize }
   * @returns {Promise<Object>} { records, total, current, size }
   */
  getMessageList: (params) => request.get(BASE + '/list', params),

  /**
   * 获取系统通知列表
   * @param {Object} params - { pageNum, pageSize }
   * @returns {Promise<Object>} { records, total }
   */
  getNoticeList: (params) => request.get(BASE + '/list', { ...params, type: 0 }),

  /**
   * 获取订单消息列表
   * @param {Object} params - { pageNum, pageSize }
   * @returns {Promise<Object>} { records, total }
   */
  getOrderMessageList: (params) => request.get(BASE + '/list', { ...params, type: 1 }),

  /**
   * 获取聊天会话列表
   * @returns {Promise<Array>} 聊天会话列表
   */
  getChatList: () => request.get(BASE + '/chatList'),

  /**
   * 获取聊天记录
   * @param {string} conversationId - 会话ID
   * @param {Object} params - { pageNum, pageSize }
   * @returns {Promise<Object>} { records, total }
   */
  getChatRecords: (conversationId, params) => request.get(BASE + '/chatMessages', { conversationId, ...params }),

  /**
   * 发送聊天消息
   * @param {Object} data - { conversationId, toUserId, content, type }
   * @returns {Promise<Object>} 消息信息
   */
  sendMessage: (data) => request.post(BASE + '/sendChat', data),

  /**
   * 标记消息为已读
   * @param {number} id - 消息ID
   * @returns {Promise}
   */
  markAsRead: (id) => request.post(BASE + '/read', { id }),

  /**
   * 全部标记已读
   * @param {number} type - 消息类型
   * @returns {Promise}
   */
  markAllAsRead: (type) => request.post(BASE + '/read', { type }),

  /**
   * 获取未读消息数量
   * @returns {Promise<Object>} { total, system, order, interact, chat }
   */
  getUnreadCount: () => request.get(BASE + '/unreadCount'),

  /**
   * 获取消息详情
   * @param {number} id - 消息ID
   * @returns {Promise<Object>} 消息详情
   */
  getMessageDetail: (id) => request.get(BASE + '/detail', { id })
}

export default messageApi
