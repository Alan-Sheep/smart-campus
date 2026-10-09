/**
 * 管理端接口统一管理
 * 按后端模块划分，与 81 个管理端端点一一对应
 */
import request from '@/utils/request'

/* ============ 登录授权 ============ */
export const authApi = {
  login: (data) => request.post('/admin/auth/login', data),
  info: () => request.get('/admin/auth/info'),
  logout: () => request.post('/admin/auth/logout'),
  updatePassword: (data) => request.put('/admin/auth/updatePassword', data)
}

/* ============ 数据概览 ============ */
export const statsApi = {
  overview: (schoolId) => request.get('/admin/stats/overview', { params: { schoolId } }),
  userGrowth: (params) => request.get('/admin/stats/userGrowth', { params }),
  orderStats: (params) => request.get('/admin/stats/orderStats', { params }),
  moduleRatio: (schoolId) => request.get('/admin/stats/moduleRatio', { params: { schoolId } }),
  messageTrend: (params) => request.get('/admin/stats/messageTrend', { params }),
  messageTypeRatio: (schoolId) => request.get('/admin/stats/messageTypeRatio', { params: { schoolId } })
}

/* ============ 用户管理 ============ */
export const userApi = {
  list: (params) => request.get('/admin/user/list', { params }),
  detail: (id) => request.get(`/admin/user/detail/${id}`),
  update: (data) => request.put('/admin/user/update', data),
  updateStatus: (id, status) => request.put(`/admin/user/updateStatus/${id}`, null, { params: { status } })
}

/* ============ 学校管理 ============ */
export const schoolApi = {
  list: (params) => request.get('/admin/school/list', { params }),
  all: () => request.get('/admin/school/all'),
  add: (data) => request.post('/admin/school/add', data),
  update: (data) => request.put('/admin/school/update', data),
  remove: (id) => request.delete(`/admin/school/delete/${id}`)
}

/* ============ 轮播图管理 ============ */
export const bannerApi = {
  list: (params) => request.get('/admin/banner/list', { params }),
  add: (data) => request.post('/admin/banner/add', data),
  update: (data) => request.put('/admin/banner/update', data),
  remove: (id) => request.delete(`/admin/banner/delete/${id}`),
  updateStatus: (id, status) => request.put(`/admin/banner/updateStatus/${id}`, null, { params: { status } }),
  sort: (ids) => request.put('/admin/banner/sort', ids)
}

/* ============ 公告管理 ============ */
export const noticeApi = {
  list: (params) => request.get('/admin/notice/list', { params }),
  add: (data) => request.post('/admin/notice/add', data),
  update: (data) => request.put('/admin/notice/update', data),
  remove: (id) => request.delete(`/admin/notice/delete/${id}`),
  top: (id, isTop) => request.put(`/admin/notice/top/${id}`, null, { params: { isTop } })
}

/* ============ 内容审核 ============ */
// 注意：后端 *Remove / *Delete 接口均只接收路径参数，不接收请求体
export const auditApi = {
  // 商品
  goodsList: (params) => request.get('/admin/audit/goodsList', { params }),
  goodsDetail: (id) => request.get(`/admin/audit/goodsDetail/${id}`),
  goodsAudit: (id, data) => request.put(`/admin/audit/goodsAudit/${id}`, data),
  goodsRemove: (id) => request.put(`/admin/audit/goodsRemove/${id}`),
  // 帖子
  postList: (params) => request.get('/admin/audit/postList', { params }),
  postDetail: (id) => request.get(`/admin/audit/postDetail/${id}`),
  postAudit: (id, data) => request.put(`/admin/audit/postAudit/${id}`, data),
  postDelete: (id) => request.delete(`/admin/audit/postDelete/${id}`),
  postTop: (id, isTop) => request.put(`/admin/audit/postTop/${id}`, null, { params: { isTop } }),
  commentDelete: (id) => request.delete(`/admin/audit/commentDelete/${id}`),
  // 活动
  activityList: (params) => request.get('/admin/audit/activityList', { params }),
  activityDetail: (id) => request.get(`/admin/audit/activityDetail/${id}`),
  activityAudit: (id, data) => request.put(`/admin/audit/activityAudit/${id}`, data),
  activityRemove: (id) => request.put(`/admin/audit/activityRemove/${id}`),
  // 失物招领（lostfoundList 支持 auditStatus + type + schoolId 服务端筛选）
  lostfoundList: (params) => request.get('/admin/audit/lostfoundList', { params }),
  lostfoundDetail: (id) => request.get(`/admin/audit/lostfoundDetail/${id}`),
  lostfoundAudit: (id, data) => request.put(`/admin/audit/lostfoundAudit/${id}`, data),
  lostfoundRemove: (id) => request.put(`/admin/audit/lostfoundRemove/${id}`)
}

/* ============ 订单管理 ============ */
function orderApiOf(module) {
  return {
    list: (params) => request.get(`/admin/order/${module}List`, { params }),
    detail: (id) => request.get(`/admin/order/${module}Detail/${id}`),
    accept: (id) => request.put(`/admin/order/${module}Accept/${id}`),
    complete: (id) => request.put(`/admin/order/${module}Complete/${id}`),
    cancel: (id, reason) => request.put(`/admin/order/${module}Cancel/${id}`, null, { params: { reason } }),
    refund: (id) => request.put(`/admin/order/${module}Refund/${id}`)
  }
}
export const orderApi = {
  express: orderApiOf('express'),
  print: orderApiOf('print'),
  errand: orderApiOf('errand'),
  substitute: orderApiOf('substitute')
}

/* ============ 意见反馈 ============ */
export const feedbackApi = {
  list: (params) => request.get('/admin/feedback/list', { params }),
  detail: (id) => request.get(`/admin/feedback/detail/${id}`),
  handle: (id, status) => request.put(`/admin/feedback/handle/${id}`, null, { params: { status } }),
  reply: (id, data) => request.put(`/admin/feedback/reply/${id}`, data)
}

/* ============ 管理员管理 ============ */
export const manageApi = {
  list: (params) => request.get('/admin/manage/list', { params }),
  add: (data) => request.post('/admin/manage/add', data),
  update: (data) => request.put('/admin/manage/update', data),
  remove: (id) => request.delete(`/admin/manage/delete/${id}`),
  resetPassword: (id, data) => request.put(`/admin/manage/resetPassword/${id}`, data || {})
}

export default {
  authApi, statsApi, userApi, schoolApi, bannerApi, noticeApi,
  auditApi, orderApi, feedbackApi, manageApi
}
