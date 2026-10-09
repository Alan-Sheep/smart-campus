import axios from 'axios'
import { ElMessage } from 'element-plus'

/**
 * 统一请求封装
 *
 * 后端返回结构：{ code, msg, data }
 * - code === 200 视为成功，自动解包返回 data
 * - code === 401 视为未登录/过期，清除登录态并跳转登录页
 * - 其他 code 弹出业务错误提示
 */
const service = axios.create({
  baseURL: '/api',
  timeout: 15000
})

// 请求拦截：注入管理员 token
service.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('admin_token')
    if (token) {
      config.headers['Authorization'] = 'Bearer ' + token
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截：统一处理业务码
service.interceptors.response.use(
  (response) => {
    const res = response.data

    // 非标准结构（如文件流）直接返回
    if (res === null || typeof res !== 'object' || res.code === undefined) {
      return res
    }

    if (res.code === 200) {
      return res.data
    }

    if (res.code === 401) {
      handleUnauthorized(res.msg)
      return Promise.reject(new Error(res.msg || '登录已过期'))
    }

    ElMessage.error(res.msg || '操作失败')
    return Promise.reject(new Error(res.msg || '操作失败'))
  },
  (error) => {
    const status = error.response?.status
    const data = error.response?.data

    if (status === 401) {
      handleUnauthorized(data?.msg)
      return Promise.reject(error)
    }

    const msgMap = {
      400: '请求参数错误',
      403: '没有访问权限',
      404: '接口不存在',
      500: '服务器内部错误'
    }
    ElMessage.error(data?.msg || msgMap[status] || `请求失败 (${status || '网络异常'})`)
    return Promise.reject(error)
  }
)

// 防止 401 时重复弹窗/跳转
let redirecting = false
function handleUnauthorized(msg) {
  if (redirecting) return
  redirecting = true
  ElMessage.warning(msg || '登录已过期，请重新登录')
  localStorage.removeItem('admin_token')
  localStorage.removeItem('admin_info')
  setTimeout(() => {
    window.location.href = '/login'
    redirecting = false
  }, 800)
}

export default service
