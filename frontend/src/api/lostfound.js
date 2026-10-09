/**
 * 失物招领相关接口
 * 列表、详情、发布、AI匹配等
 */
import request from '@/utils/request'

// 失物招领相关接口路径前缀
const BASE = '/api/lostfound'

/**
 * 后端字段 → 前端字段适配
 * 后端：type(0/1), item_name, images(逗号分隔字符串), status(0/1)
 * 前端：type(lost/found), images(数组)
 */
function adaptLostFound(item) {
  if (!item) return item
  // type 映射：0=丢失(lost), 1=招领(found)
  let type = item.type
  if (type === '0' || type === 0) type = 'lost'
  else if (type === '1' || type === 1) type = 'found'

  // 图片：逗号分隔字符串 → 数组
  let images = item.images
  if (typeof images === 'string' && images) {
    images = images.split(',').filter(img => img.trim())
  } else if (!images) {
    images = []
  }

  return {
    ...item,
    type,
    images
  }
}

export const lostFoundApi = {
  /**
   * 获取失物招领列表
   * @param {Object} params - { type: 'lost'|'found', keyword, pageNum, pageSize }
   * @returns {Promise<Object>} Page 对象，列表在 records 字段
   */
  getLostFoundList: (params) => request.get(BASE + '/list', params).then(data => {
    const list = data.records || data.list || data || []
    const adapted = list.map(adaptLostFound)
    if (data.records !== undefined) {
      return { ...data, records: adapted }
    }
    return adapted
  }),

  /**
   * 获取失物招领详情
   * @param {number} id - 记录ID
   * @returns {Promise<Object>} 详情信息
   */
  getLostFoundDetail: (id) => request.get(BASE + '/detail', { id }).then(adaptLostFound),

  /**
   * 发布失物招领信息
   * @param {Object} data - 发布信息
   * @returns {Promise<Object>} 发布后的信息
   */
  publishLostFound: (data) => request.post(BASE + '/create', data),

  /**
   * AI 智能匹配相似物品
   * @param {number} id - 丢失物品ID
   * @returns {Promise<Array>} 匹配到的拾到物品列表
   * @note 后端暂无此接口，保留供前端扩展
   */
  matchLostItem: (id) => request.get(BASE + '/match/' + id),

  /**
   * 搜索失物招领
   * @param {string} keyword - 搜索关键词
   * @returns {Promise<Array>} 搜索结果
   * @note 后端暂无独立搜索接口，已复用 list 的 keyword 参数
   */
  search: (keyword) => request.get(BASE + '/list', { keyword }),

  /**
   * 获取我发布的失物招领
   * @param {Object} params - { type, pageNum, pageSize }
   * @returns {Promise<Object>} { records, total }
   */
  getMyPosts: (params) => request.get(BASE + '/myList', params),

  /**
   * 标记为已找回/已认领
   * @param {number} id - 记录ID
   * @returns {Promise}
   */
  markResolved: (id) => request.post(BASE + '/updateStatus', { id, status: 1 }),

  /**
   * 删除发布
   * @param {number} id - 记录ID
   * @returns {Promise}
   * @note 后端暂无此接口，保留供前端扩展
   */
  deletePost: (id) => request.delete(BASE + '/' + id)
}

export default lostFoundApi
