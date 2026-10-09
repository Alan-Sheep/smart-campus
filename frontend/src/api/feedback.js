import request from '@/utils/request'

/**
 * 意见反馈接口
 */
export const feedbackApi = {
  /**
   * 提交反馈
   * @param {Object} data - 反馈内容
   * @param {string} data.type - 反馈类型
   * @param {string} data.title - 标题
   * @param {string} data.content - 内容
   * @param {string} data.images - 图片（逗号分隔）
   * @param {string} data.contact - 联系方式
   */
  submitFeedback(data) {
    return request({
      url: '/api/feedback/submit',
      method: 'POST',
      data
    })
  },

  /**
   * 我的反馈列表
   */
  getMyFeedback(params) {
    return request({
      url: '/api/feedback/myList',
      method: 'GET',
      data: params
    })
  }
}

export default feedbackApi
