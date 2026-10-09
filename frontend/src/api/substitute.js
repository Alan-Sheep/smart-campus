/**
 * 代课服务相关接口
 * 订单列表、详情、发布、接单等
 */
import request from '@/utils/request'

// 代课服务相关接口路径前缀
const BASE = '/api/substitute'

/**
 * 后端字段 → 前端字段适配
 * 后端状态：0=已取消, 1=待接单, 2=进行中(已接单), 3=已完成
 * 前端状态：0=待接单, 1=进行中, 2=已完成, 3=已取消
 */
function adaptSubstitute(item) {
  if (!item) return item

  // 解析 courseTime，如 "周二第3-4节" → day="周二", section="3-4节"
  let day = ''
  let section = ''
  const timeStr = item.courseTime || ''
  const match = timeStr.match(/(周[一二三四五六日天])第(.+节)/)
  if (match) {
    day = match[1]
    section = match[2]
  } else {
    day = timeStr
    section = timeStr
  }

  const statusMap = { 0: 3, 1: 0, 2: 1, 3: 2 }

  return {
    ...item,
    day,
    section,
    date: item.courseDate || '',
    location: item.classroom || '',
    reward: item.reward != null ? Number(item.reward) : 0,
    requirement: item.requirement || '',
    contact: item.contactWechat || item.contactQq || '',
    status: statusMap[item.status] ?? 0
  }
}

export const substituteApi = {
  /**
   * 获取代课订单列表
   */
  getSubstituteList: (params) => {
    // 前端 tab 值 → 后端 type 值映射
    // 前端: hall(大厅) / my(我的发布)
    // 后端: all(大厅) / publish(我发布的) / accept(我接单的)
    let backendType = params.type
    if (params.type === 'hall') backendType = 'all'
    else if (params.type === 'my') backendType = 'publish'

    const query = {
      type: backendType || 'all',
      pageNum: params.pageNum || 1,
      pageSize: params.pageSize || 10
    }
    return request.get(BASE + '/orderList', query).then(data => {
      const list = data.records || data.list || data || []
      const adapted = list.map(adaptSubstitute)
      if (data.records !== undefined) {
        return { ...data, records: adapted }
      }
      return adapted
    })
  },

  /**
   * 获取代课订单详情
   */
  getSubstituteDetail: (id) => request.get(BASE + '/orderDetail', { id }).then(adaptSubstitute),

  /**
   * 发布代课需求
   */
  publishSubstitute: (data) => request.post(BASE + '/createOrder', data),

  /**
   * 接代课
   */
  acceptSubstitute: (id) => request.post(BASE + '/acceptOrder', { id }),

  /**
   * 完成代课订单
   */
  finishSubstitute: (id) => request.post(BASE + '/completeOrder', { id }),

  /**
   * 取消代课订单
   */
  cancelSubstitute: (id) => request.post(BASE + '/cancelOrder', { id })
}

export default substituteApi
