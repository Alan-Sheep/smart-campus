/**
 * 游戏搭子相关接口
 * 列表、详情、发布等
 */
import request from '@/utils/request'

// 游戏搭子相关接口路径前缀
const BASE = '/api/game'

/**
 * 后端字段 → 前端字段适配
 */
function adaptGame(item) {
  if (!item) return item
  return {
    ...item,
    content: item.description || '',
    level: item.gameLevel || '不限',
    gameType: item.playMode || item.gameName || '',
    publisher: '校园玩家',
    contact: item.contactWay || ''
  }
}

function adaptPage(data) {
  if (!data || !data.records) return data
  return {
    ...data,
    records: data.records.map(adaptGame)
  }
}

export const gameApi = {
  /**
   * 获取游戏搭子列表
   * @param {Object} params - { gameType, keyword, pageNum, pageSize }
   * @returns {Promise<Object>} { records, total, current, size }
   */
  getGameList: async (params) => {
    const mapped = {
      gameName: params.gameType,
      keyword: params.keyword,
      pageNum: params.pageNum,
      pageSize: params.pageSize
    }
    const data = await request.get(BASE + '/list', mapped)
    return adaptPage(data)
  },

  /**
   * 获取游戏搭子详情
   * @param {number} id - 帖子ID
   * @returns {Promise<Object>} 详情信息
   */
  getGameDetail: async (id) => {
    const data = await request.get(BASE + '/detail', { id })
    return adaptGame(data)
  },

  /**
   * 发布找搭子帖子
   * @param {Object} data - 帖子信息
   * @returns {Promise<Object>} 发布后的帖子信息
   */
  publishGame: (data) => request.post(BASE + '/create', data),

  /**
   * 获取游戏分类列表（后端返回游戏名去重后的字符串数组）
   */
  getGameTypes: () => request.get(BASE + '/gameTypes'),

  /**
   * 获取我发布的游戏搭子
   * @param {Object} params - { pageNum, pageSize }
   * @returns {Promise<Object>} { records, total }
   */
  getMyPosts: async (params) => {
    const data = await request.get(BASE + '/myList', params)
    return adaptPage(data)
  },

  /**
   * 删除帖子
   * @param {number} id - 帖子ID
   * @returns {Promise}
   * @note 后端暂无此接口，保留供前端扩展
   */
  deletePost: (id) => request.delete(BASE + '/' + id)
}

export default gameApi
