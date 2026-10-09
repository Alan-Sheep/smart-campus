/**
 * 校园跑腿相关接口
 * 订单列表、详情、发布、接单等
 */
import request from '@/utils/request'

// 校园跑腿相关接口路径前缀
const BASE = '/api/errand'

/**
 * 后端字段 → 前端字段适配
 * 后端状态：0=已取消, 1=待接单, 2=进行中(已接单), 3=已完成
 * 前端状态：0=待接单, 1=进行中, 2=已完成, 3=已取消
 */
function adaptErrand(item) {
  if (!item) return item
  const statusMap = { 0: 3, 1: 0, 2: 1, 3: 2 }
  return {
    ...item,
    type: String(item.errandType ?? ''),
    title: item.title || '',
    description: item.description || '',
    pickupLocation: item.pickupAddress || '',
    deliveryLocation: item.deliveryAddress || '',
    reward: item.serviceFee != null ? Number(item.serviceFee) : 0,
    estimateTime: item.estimatedTime ? item.estimatedTime.substring(5, 16) : '尽快',
    status: statusMap[item.status] ?? 0
  }
}

export const errandApi = {
  /**
   * 获取跑腿订单列表
   * @param {Object} params - { category, type: 'hall'|'my', pageNum, pageSize }
   */
  getErrandList: (params) => {
    // 前端 tab 值 → 后端 type 值映射
    // 前端: hall(大厅) / my(我的发布) / accept(我接单的)
    // 后端: all(大厅) / publish(我发布的) / accept(我接单的)
    let backendType = params.type
    if (params.type === 'hall') backendType = 'all'
    else if (params.type === 'my') backendType = 'publish'

    const query = {
      errandType: params.category && params.category !== 'all' ? params.category : undefined,
      type: backendType || 'all',
      keyword: params.keyword,
      pageNum: params.pageNum || 1,
      pageSize: params.pageSize || 10
    }
    return request.get(BASE + '/orderList', query).then(data => {
      const list = data.records || data.list || data || []
      const adapted = list.map(adaptErrand)
      if (data.records !== undefined) {
        return { ...data, records: adapted }
      }
      return adapted
    })
  },

  /**
   * 获取跑腿订单详情
   */
  getErrandDetail: (id) => request.get(BASE + '/orderDetail', { id }).then(adaptErrand),

  /**
   * 发布跑腿需求
   */
  publishErrand: (data) => request.post(BASE + '/createOrder', data),

  /**
   * 接单
   */
  acceptErrand: (id) => request.post(BASE + '/acceptOrder', { id }),

  /**
   * 完成跑腿订单
   */
  finishErrand: (id) => request.post(BASE + '/completeOrder', { id }),

  /**
   * 取消跑腿订单
   */
  cancelErrand: (id) => request.post(BASE + '/cancelOrder', { id }),

  /**
   * 获取跑腿分类列表
   */
  getCategories: () => request.get(BASE + '/categories')
}

export default errandApi
