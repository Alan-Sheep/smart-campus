/**
 * 首页相关接口
 * 轮播图、公告、功能模块、活动推荐等
 */
import request from '@/utils/request'

// 首页相关接口路径前缀
const BASE = '/api/home'

export const homeApi = {
  /**
   * 获取首页综合数据（轮播图+公告+功能模块）
   * @returns {Promise<Object>} { bannerList, noticeList, modules }
   */
  getIndex: () => request.get(BASE + '/index'),

  /**
   * 获取轮播图列表
   * @returns {Promise<Array>} 轮播图列表
   */
  getBannerList: () => request.get(BASE + '/bannerList'),

  /**
   * 获取公告列表
   * @returns {Promise<Array>} 公告列表
   */
  getNoticeList: () => request.get(BASE + '/noticeList')
}

export default homeApi
