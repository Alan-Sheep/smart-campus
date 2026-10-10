/**
 * 文件上传相关接口
 * 图片上传、文件上传等
 */
import { BASE_URL } from '@/utils/request'

// 文件上传相关接口路径前缀
const BASE = '/api/file'

export const fileApi = {
  /**
   * 上传图片
   * @param {string} filePath - 本地文件路径
   * @param {string} name - 文件字段名，默认 file
   * @returns {Promise<Object>} { url, name, size }
   */
  uploadImage: (filePath, name = 'file') => {
    // #ifdef H5
    // 手机浏览器上 chooseImage 返回 blob: URL，uni.uploadFile 兼容性差，
    // 改用 fetch+FormData 直传（修复手机上传图片后商品图片空白的问题）
    if (/^blob:|^data:/.test(filePath) || (typeof location !== 'undefined' && location.protocol.startsWith('http'))) {
      return new Promise(async (resolve, reject) => {
        uni.showLoading({ title: '上传中...', mask: true })
        try {
          const blob = await (await fetch(filePath)).blob()
          const fd = new FormData()
          fd.append('file', blob, 'image_' + Date.now() + '.png')
          const token = uni.getStorageSync('token') || ''
          const cur = uni.getStorageSync('currentSchool')
          const xhr = new XMLHttpRequest()
          xhr.open('POST', (typeof BASE_URL !== 'undefined' ? BASE_URL : '') + BASE + '/upload')
          if (token) xhr.setRequestHeader('Authorization', 'Bearer ' + token)
          if (cur && cur.id) xhr.setRequestHeader('School-Id', cur.id)
          xhr.onload = () => {
            uni.hideLoading()
            try {
              const data = JSON.parse(xhr.responseText)
              if (data.code === 200) { resolve(data.data) }
              else { uni.showToast({ title: data.msg || '上传失败', icon: 'none' }); reject(data) }
            } catch (e) { uni.showToast({ title: '上传失败', icon: 'none' }); reject(e) }
          }
          xhr.onerror = () => { uni.hideLoading(); uni.showToast({ title: '上传失败', icon: 'none' }); reject(new Error('network')) }
          xhr.send(fd)
        } catch (e) { uni.hideLoading(); reject(e) }
      })
    }
    // #endif
    return new Promise((resolve, reject) => {
      // 从本地缓存获取 token
      const token = uni.getStorageSync('token') || ''
      // 获取当前学校ID
      const currentSchool = uni.getStorageSync('currentSchool')
      const schoolId = currentSchool ? (currentSchool.id || 1) : 1

      uni.showLoading({ title: '上传中...', mask: true })

      uni.uploadFile({
        url: BASE_URL + BASE + '/upload',
        filePath: filePath,
        name: name,
        header: {
          'Authorization': token ? 'Bearer ' + token : '',
          'School-Id': schoolId
        },
        success: (res) => {
          uni.hideLoading()
          try {
            const data = JSON.parse(res.data)
            if (data.code === 200) {
              // 自动解包返回 data
              resolve(data.data)
            } else {
              uni.showToast({
                title: data.msg || '上传失败',
                icon: 'none'
              })
              reject(data)
            }
          } catch (e) {
            uni.showToast({
              title: '上传失败',
              icon: 'none'
            })
            reject(e)
          }
        },
        fail: (err) => {
          uni.hideLoading()
          console.error('文件上传失败:', err)
          uni.showToast({
            title: '网络异常，上传失败',
            icon: 'none'
          })
          reject(err)
        }
      })
    })
  },

  /**
   * 上传多张图片
   * @param {Array<string>} filePaths - 本地文件路径数组
   * @returns {Promise<Array>} 上传结果数组
   */
  uploadImages: async (filePaths) => {
    const results = []
    for (const path of filePaths) {
      const result = await fileApi.uploadImage(path)
      results.push(result)
    }
    return results
  },

  /**
   * 上传文件（通用）
   * @param {string} filePath - 本地文件路径
   * @param {string} name - 文件字段名
   * @returns {Promise<Object>} { url, name, size }
   */
  uploadFile: (filePath, name = 'file') => {
    return fileApi.uploadImage(filePath, name)
  },

  /**
   * 删除文件
   * @param {string} url - 文件URL
   * @returns {Promise}
   */
  deleteFile: (url) => {
    return new Promise((resolve, reject) => {
      // 这里可以调用删除文件的接口
      // request.delete(BASE + '/delete', { url })
      resolve()
    })
  }
}

// 兼容旧的 uploadApi 命名
export const uploadApi = fileApi

export default fileApi
