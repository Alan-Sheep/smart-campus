/**
 * 打印服务相关接口
 * 订单列表、详情、提交打印订单等
 */
import request from '@/utils/request'

// 打印服务相关接口路径前缀
const BASE = '/api/print'

export const printApi = {
  /**
   * 获取打印订单列表
   * @param {Object} params - { status, pageNum, pageSize }
   * @returns {Promise<Object>} { records, total, current, size }
   */
  getPrintList: (params) => request.get(BASE + '/orderList', params),

  /**
   * 获取打印订单详情
   * @param {number} id - 订单ID
   * @returns {Promise<Object>} 订单详情
   */
  getPrintDetail: (id) => request.get(BASE + '/orderDetail', { id }),

  /**
   * 提交打印订单
   * @param {Object} data - 打印订单信息
   * @returns {Promise<Object>} 提交后的订单信息
   */
  submitPrint: (data) => request.post(BASE + '/createOrder', data),

  /**
   * 获取打印价格配置
   * @returns {Promise<Object>} 价格配置
   * @note 后端暂无此接口，保留供前端扩展
   */
  getPriceConfig: () => request.get(BASE + '/price-config'),

  /**
   * 获取我的打印订单
   * @param {Object} params - { status, pageNum, pageSize }
   * @returns {Promise<Object>} { records, total }
   */
  getMyOrders: (params) => request.get(BASE + '/orderList', params),

  /**
   * 取消打印订单
   * @param {number} id - 订单ID
   * @returns {Promise}
   */
  cancelOrder: (id) => request.post(BASE + '/cancelOrder', { id })
}

export default printApi
