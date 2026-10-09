/**
 * 校园频道（帖子）相关接口
 * 帖子列表、详情、发布、点赞、评论等
 */
import request from '@/utils/request'

// 校园频道相关接口路径前缀
const BASE = '/api/channel'

export const channelApi = {
  /**
   * 获取帖子列表
   * @param {Object} params - { keyword, category, pageNum, pageSize }
   *   category: 推荐(等价全部) / 吐槽 / 交友 / 分享
   * @returns {Promise<Object>} { records, total, current, size }
   */
  getPostList: (params) => request.get(BASE + '/postList', params),

  /**
   * 获取帖子详情
   * @param {number} id - 帖子ID
   * @returns {Promise<Object>} 帖子详情
   */
  getPostDetail: (id) => request.get(BASE + '/postDetail', { id }),

  /**
   * 发布帖子
   * @param {Object} data - 帖子信息 { title, content, images, category }
   * @returns {Promise<Object>} 发布后的帖子信息
   */
  publishPost: (data) => request.post(BASE + '/createPost', data),

  /**
   * 点赞/取消点赞
   * @param {number} postId - 帖子ID
   * @returns {Promise<boolean>} 操作后的点赞状态（true=已点赞）
   */
  toggleLike: (postId) => request.post(BASE + '/likePost', { postId }),

  /**
   * 收藏/取消收藏帖子
   * @param {number} postId - 帖子ID
   * @returns {Promise<boolean>} 操作后的收藏状态（true=已收藏）
   */
  toggleCollect: (postId) => request.post(BASE + '/collectPost', { postId }),

  /**
   * 我的收藏列表
   * @param {Object} params - { targetType }  0商品 1帖子 2活动，不传为全部
   * @returns {Promise<Array>} 收藏列表
   */
  getMyCollects: (params) => request.get(BASE + '/myCollects', params),

  /**
   * 获取评论列表
   * @param {number} postId - 帖子ID
   * @param {Object} params - { pageNum, pageSize }
   * @returns {Promise<Object>} { records, total }
   */
  getCommentList: (postId, params) => request.get(BASE + '/commentList', { postId, ...params }),

  /**
   * 发表评论
   * @param {Object} data - { postId, content, replyTo }
   * @returns {Promise<Object>} 评论信息
   */
  addComment: (data) => request.post(BASE + '/createComment', data),

  /**
   * 删除帖子
   * @param {number} id - 帖子ID
   * @returns {Promise}
   * @note 后端暂无此接口，保留供前端扩展
   */
  deletePost: (id) => request.delete(BASE + '/' + id),

  /**
   * 删除评论
   * @param {number} id - 评论ID
   * @returns {Promise}
   * @note 后端暂无此接口，保留供前端扩展
   */
  deleteComment: (id) => request.delete(BASE + '/comment/' + id),

  /**
   * 获取我发布的帖子
   * @param {Object} params - { pageNum, pageSize }
   * @returns {Promise<Object>} { records, total }
   */
  getMyPosts: (params) => request.get(BASE + '/myPosts', params)
}

// 兼容旧的 postApi 命名
export const postApi = channelApi

export default channelApi
