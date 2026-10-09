/**
 * 快递代取相关接口
 * 订单列表、详情、发布、接单等
 */
import request from '@/utils/request'

// 快递代取相关接口路径前缀
const BASE = '/api/express'

/**
 * 后端字段 → 前端字段适配
 * 后端状态：0=已取消, 1=待接单, 2=进行中(已接单), 3=已完成
 * 前端状态：0=待接单, 1=进行中, 2=已完成, 3=已取消
 */
function adaptExpress(item) {
  if (!item) return item
  const statusMap = { 0: 3, 1: 0, 2: 1, 3: 2 }
  return {
    ...item,
    reward: item.serviceFee != null ? Number(item.serviceFee) : 0,
    status: statusMap[item.status] ?? 0
  }
}

function adaptPage(data) {
  if (!data || !data.records) return data
  return {
    ...data,
    records: data.records.map(adaptExpress)
  }
}

export const expressApi = {
  /**
   * 获取快递大厅列表（所有待接单，无需登录）
   */
  getHallList: async (params) => {
    const data = await request.get(BASE + '/hallList', params)
    return adaptPage(data)
  },

  /**
   * 获取我的快递订单列表
   */
  getExpressList: async (params) => {
    const data = await request.get(BASE + '/orderList', params)
    return adaptPage(data)
  },

  /**
   * 获取快递订单详情
   */
  getExpressDetail: async (id) => {
    const data = await request.get(BASE + '/orderDetail', { id })
    return adaptExpress(data)
  },

  /**
   * 发布快递代取需求
   * @param {Object} data - 订单信息
   * @returns {Promise<Object>} 发布后的订单信息
   */
  publishExpress: (data) => request.post(BASE + '/createOrder', data),

  /**
   * 接单（跑腿人员接单）
   * @param {number} id - 订单ID
   * @returns {Promise<Object>} 接单结果
   */
  acceptOrder: (id) => request.post(BASE + '/pickOrder', { id }),

  /**
   * 完成订单
   * @param {number} id - 订单ID
   * @returns {Promise<Object>} 完成结果
   */
  finishOrder: (id) => request.post(BASE + '/completeOrder', { id }),

  /**
   * 取消订单
   * @param {number} id - 订单ID
   * @returns {Promise}
   */
  cancelOrder: (id) => request.post(BASE + '/cancelOrder', { id }),

  /**
   * 获取我的快递订单列表
   * @param {Object} params - { type, status, pageNum, pageSize }
   *   type: publish=我发布的 / pick=我接单的 / 不传=全部
   * @returns {Promise<Object>} { records, total }
   */
  getMyOrders: async (params) => {
    // 关键修复：原先这里写死了 type:'publish'，导致「我接单的」永远查不到数据。
    // 现在把调用方传入的 type 透传（由前端把 accept 映射为后端约定的 pick）。
    const data = await request.get(BASE + '/orderList', params)
    return adaptPage(data)
  }
}

export default expressApi
