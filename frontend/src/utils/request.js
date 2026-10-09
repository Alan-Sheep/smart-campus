/**
 * 网络请求封装
 * 基于 uni.request 进行封装，统一处理请求和响应
 * 后端返回格式：{ code, msg, data }
 * 前端直接获取 data 字段（自动解包）
 */

/**
 * 后端接口基础地址
 *
 * - H5 浏览器环境：使用相对路径（配合 Nginx 反向代理，无跨域问题）
 *   访问地址：http://localhost/  （Nginx 统一入口）
 *
 * - 微信小程序等其他环境：使用 localhost
 *   小程序没有跨域限制，直接访问本地接口
 *   上线时换成公网地址（cpolar 或服务器域名）
 */
// #ifdef H5
export const BASE_URL = ''
// #endif
// #ifndef H5
export const BASE_URL = 'http://localhost:8080'
// #endif

/**
 * 请求超时时间（毫秒）
 */
const TIMEOUT = 10000

/**
 * 失败重试次数（仅 GET 请求自动重试）
 */
const RETRY_COUNT = 1

/**
 * 全局 loading 计数器
 *
 * 问题：多个请求并发时，各自调用 uni.showLoading / uni.hideLoading，
 * 先返回的请求会把 loading 提前关掉（其余请求还在飞），造成闪烁。
 * 解决：用计数器保证「最后一个请求结束」才真正 hideLoading。
 */
let loadingCount = 0

function showGlobalLoading() {
  loadingCount++
  if (loadingCount === 1) {
    uni.showLoading({ title: '加载中...', mask: true })
  }
}

function hideGlobalLoading() {
  if (loadingCount > 0) {
    loadingCount--
  }
  if (loadingCount === 0) {
    uni.hideLoading()
  }
}

/**
 * 进行中的 GET 请求表（请求去重）
 *
 * 用途：页面 onMounted 与下拉刷新、快速切换 Tab 等场景可能对
 * 同一 URL + 同一参数发起重复请求，这里复用同一个 Promise，
 * 避免重复网络开销，也能减少后端压力。
 */
const pendingGets = new Map()

/**
 * 生成请求唯一键（Method + URL + 参数）
 */
function buildRequestKey(method, url, data) {
  let params = ''
  if (data && typeof data === 'object') {
    params = Object.keys(data)
      .sort()
      .map(k => k + '=' + data[k])
      .join('&')
  }
  return method + '|' + url + '|' + params
}

/**
 * 发起网络请求
 * @param {Object} options 请求配置
 * @param {string} options.url 请求地址
 * @param {string} options.method 请求方法 GET/POST/PUT/DELETE
 * @param {Object} options.data 请求参数
 * @param {Object} options.header 请求头
 * @param {boolean} options.showLoading 是否显示加载中提示，默认 false
 * @param {boolean} options.showError 是否显示错误提示，默认 true
 * @param {boolean} options.unwrapData 是否自动解包响应 data，默认 true
 * @param {boolean} options.needLogin 是否需要登录（401时自动跳转），默认 false
 * @param {number} options.retryCount 剩余重试次数，内部使用
 * @returns {Promise}
 */
function request(options) {
  const method = (options.method || 'GET').toUpperCase()

  // GET 请求：过滤掉值为 undefined / null / 空字符串 的参数
  let requestData = options.data
  if (method === 'GET' && requestData && typeof requestData === 'object') {
    const cleaned = {}
    Object.keys(requestData).forEach(key => {
      const val = requestData[key]
      if (val !== undefined && val !== null && val !== '') {
        cleaned[key] = val
      }
    })
    requestData = cleaned
  }

  // GET 请求去重：相同 URL + 参数在飞行中时复用同一 Promise
  const dedupe = method === 'GET' && options.dedupe !== false && !options.retryCount
  const requestKey = dedupe ? buildRequestKey(method, options.url, requestData) : null
  if (dedupe && pendingGets.has(requestKey)) {
    return pendingGets.get(requestKey)
  }

  // 显示加载中（计数器，保证并发请求不会提前关闭）
  if (options.showLoading) {
    showGlobalLoading()
  }

  const promise = new Promise((resolve, reject) => {
    // 从本地缓存获取 token
    const token = uni.getStorageSync('token') || ''
    // 获取当前学校ID
    const currentSchool = uni.getStorageSync('currentSchool')
    const schoolId = currentSchool ? (currentSchool.id || 1) : 1

    uni.request({
      url: BASE_URL + options.url,
      method: method,
      data: requestData || {},
      timeout: TIMEOUT,
      header: {
        'Content-Type': 'application/json',
        'Authorization': token ? 'Bearer ' + token : '',
        'School-Id': schoolId,
        ...options.header
      },
      success: (res) => {
        if (options.showLoading) {
          hideGlobalLoading()
        }

        const statusCode = res.statusCode

        // HTTP 层错误
        if (statusCode < 200 || statusCode >= 300) {
          console.error('HTTP 错误:', options.url, statusCode)
          handleError(statusCode, options, reject, res.data)
          return
        }

        const data = res.data

        // 返回不是对象，直接返回
        if (typeof data !== 'object' || data === null) {
          resolve(data)
          return
        }

        // 业务层处理
        if (data.code === 200) {
          const result = options.unwrapData !== false ? data.data : data
          resolve(result)
        } else if (data.code === 401) {
          // 未登录或 token 过期
          handleAuthError(options, data, reject)
        } else {
          // 其他业务错误
          if (options.showError !== false) {
            uni.showToast({
              title: data.msg || '请求失败',
              icon: 'none',
              duration: 2000
            })
          }
          reject(data)
        }
      },
      fail: (err) => {
        if (options.showLoading) {
          hideGlobalLoading()
        }

        console.error('请求失败:', options.url, err)

        // GET 请求失败，有剩余重试次数的话自动重试
        if (method === 'GET') {
          const remainRetry = options.retryCount !== undefined ? options.retryCount : RETRY_COUNT
          if (remainRetry > 0) {
            console.log(`重试请求 (${RETRY_COUNT - remainRetry + 1}/${RETRY_COUNT}):`, options.url)
            setTimeout(() => {
              request({ ...options, retryCount: remainRetry - 1 })
                .then(resolve)
                .catch(reject)
            }, 500)
            return
          }
        }

        // 没有重试次数了，提示用户
        if (options.showError !== false) {
          uni.showToast({
            title: '网络连接失败，请检查网络',
            icon: 'none',
            duration: 2000
          })
        }
        reject(err)
      }
    })
  })

  // 注册去重表；请求结束（无论成功失败）后清理，避免后续请求拿到旧结果
  if (dedupe) {
    pendingGets.set(requestKey, promise)
    const cleanup = () => pendingGets.delete(requestKey)
    promise.then(cleanup, cleanup)
  }

  return promise
}

/**
 * 处理 HTTP 错误
 */
function handleError(statusCode, options, reject, data) {
  let msg = ''
  switch (statusCode) {
    case 400:
      msg = '请求参数错误'
      break
    case 401:
      msg = '登录已过期，请重新登录'
      break
    case 403:
      msg = '没有访问权限'
      break
    case 404:
      msg = '请求的资源不存在'
      break
    case 500:
    case 502:
    case 503:
    case 504:
      msg = '服务器开小差了，请稍后再试'
      break
    default:
      msg = '服务异常 (' + statusCode + ')'
  }

  if (statusCode === 401) {
    handleAuthError(options, { code: 401, msg }, reject)
    return
  }

  if (options.showError !== false) {
    uni.showToast({ title: msg, icon: 'none', duration: 2000 })
  }
  reject({ code: statusCode, msg, data })
}

/**
 * 处理 401 未授权错误
 */
function handleAuthError(options, data, reject) {
  // 清除本地登录态
  uni.removeStorageSync('token')
  uni.removeStorageSync('userInfo')

  if (options.showError !== false) {
    uni.showToast({
      title: data.msg || '请先登录',
      icon: 'none',
      duration: 1500
    })
  }

  // 如果标记了需要登录，自动跳转登录页
  if (options.needLogin) {
    setTimeout(() => {
      uni.navigateTo({ url: '/pages/login/login' })
    }, 1500)
  }

  reject(data)
}

// 导出常用的请求方法（自动解包 data）
export default {
  get: (url, data, options = {}) => request({ url, method: 'GET', data, ...options }),
  post: (url, data, options = {}) => request({ url, method: 'POST', data, ...options }),
  put: (url, data, options = {}) => request({ url, method: 'PUT', data, ...options }),
  delete: (url, data, options = {}) => request({ url, method: 'DELETE', data, ...options })
}

/**
 * 检查是否已登录
 * @param {boolean} showTip 是否显示提示，默认 true
 * @returns {boolean} 是否已登录
 */
export function checkLogin(showTip = true) {
  const token = uni.getStorageSync('token')
  if (!token) {
    if (showTip) {
      uni.showToast({
        title: '请先登录',
        icon: 'none'
      })
    }
    return false
  }
  return true
}
