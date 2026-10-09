/**
 * 二手市场相关接口
 * 商品列表、详情、发布、收藏等
 */
import request from '@/utils/request'

// 二手市场相关接口路径前缀
const BASE = '/api/goods'

/**
 * 后端字段 → 前端字段适配
 */
function adaptGoods(item) {
  if (!item) return item
  return {
    ...item,
    images: item.images ? item.images.split(',').filter(Boolean) : [],
    sellerName: item.sellerName || item.userName || '校园用户',
    createTime: item.createTime || ''
  }
}

export const goodsApi = {
  /**
   * 获取商品列表
   */
  getGoodsList: (params) => {
    const mapped = {
      categoryId: params.category,
      keyword: params.keyword,
      sort: params.sortBy,
      pageNum: params.pageNum,
      pageSize: params.pageSize
    }
    return request.get(BASE + '/list', mapped).then(data => {
      const list = data.records || data.list || data || []
      const adapted = list.map(adaptGoods)
      if (data.records !== undefined) {
        return { ...data, records: adapted }
      }
      return adapted
    })
  },

  /**
   * 获取商品详情
   */
  getGoodsDetail: (id) => request.get(BASE + '/detail', { id }).then(adaptGoods),

  /**
   * 发布商品
   * @param {Object} data - 商品信息
   * @returns {Promise<Object>} 发布后的商品信息
   */
  publishGoods: (data) => request.post(BASE + '/create', data),

  /**
   * 更新商品信息
   * @param {number} id - 商品ID
   * @param {Object} data - 更新的商品信息
   * @returns {Promise<Object>} 更新后的商品信息
   * @note 后端暂无此接口，保留供前端扩展
   */
  updateGoods: (id, data) => request.put(BASE + '/' + id, data),

  /**
   * 获取商品分类列表
   * @returns {Promise<Array>} 分类列表
   * @note 后端暂无此接口，保留供前端扩展
   */
  getGoodsCategory: () => request.get(BASE + '/categories'),

  /**
   * 获取我发布的商品
   * @param {Object} params - { status, pageNum, pageSize }
   * @returns {Promise<Object>} { records, total }
   */
  getMyGoods: (params) => request.get(BASE + '/myList', params),

  /**
   * 收藏/取消收藏商品
   * @param {number} id - 商品ID
   * @returns {Promise<Object>} { isCollected }
   * @note 后端暂无此接口，保留供前端扩展
   */
  toggleCollect: (id) => request.post(BASE + '/collect/' + id),

  /**
   * 获取收藏的商品列表
   * @param {Object} params - { pageNum, pageSize }
   * @returns {Promise<Object>} { records, total }
   * @note 后端暂无此接口，保留供前端扩展
   */
  getCollectList: (params) => request.get(BASE + '/collect/list', params),

  /**
   * 删除商品
   * @param {number} id - 商品ID
   * @returns {Promise}
   * @note 后端暂无此接口，可用 updateStatus 下架替代，保留供前端扩展
   */
  deleteGoods: (id) => request.delete(BASE + '/' + id),

  /**
   * 更新商品状态（上架/下架等）
   * @param {number} id - 商品ID
   * @param {number} status - 状态值
   * @returns {Promise}
   */
  updateStatus: (id, status) => request.post(BASE + '/updateStatus', { id, status })
}

export default goodsApi
