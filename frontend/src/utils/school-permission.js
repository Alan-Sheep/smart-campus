/**
 * 跨校访问权限工具
 *
 * 规则（与用户确认的三点）：
 * 1. 身份判断：以「用户档案绑定的学校」(userInfo.schoolId) 为准；
 *    当前浏览的学校(School-Id/本地缓存的 currentSchool) ≠ 绑定学校 → 访客身份
 * 2. 访客权限：全部业务只读，唯独「校园频道」可发帖/评论/点赞/收藏
 * 3. 提示形式：进入非本校业务页面时 toast 提示一次「你不是该校用户」，
 *    发布/接单/报名等写操作按钮点击时再次拦截提示
 *
 * 使用方式（在各业务页面的 <script setup> 中）：
 *   import { crossSchool, guardWrite, showCrossHintOnce } from '@/utils/school-permission'
 *   const isGuest = computed(() => crossSchool.isGuest())
 *   // 写操作入口第一行：
 *   if (guardWrite()) return
 */

import { checkLogin } from '@/utils/request'

const KEY_BIND = 'bindHintShown'

/**
 * 获取用户绑定的学校ID
 * @returns {number|null} 未登录或未绑定时返回 null
 */
function getBoundSchoolId() {
  try {
    const info = uni.getStorageSync('userInfo')
    if (info && info.schoolId) return Number(info.schoolId)
  } catch (e) { /* storage 异常按未绑定处理 */ }
  return null
}

/**
 * 获取当前浏览的学校ID（本地缓存的 currentSchool）
 */
function getCurrentSchoolId() {
  try {
    const cur = uni.getStorageSync('currentSchool')
    if (cur && cur.id) return Number(cur.id)
  } catch (e) { /* ignore */ }
  return null
}

/**
 * 是否跨校访客：
 * 已登录 且 已绑定学校 且 绑定学校 ≠ 当前浏览学校
 */
function isGuest() {
  if (!checkLogin(false)) return false          // 未登录不按跨校处理（登录拦截另有逻辑）
  const bound = getBoundSchoolId()
  if (!bound) return false                       // 未绑定学校的不受限（引导选校流程负责）
  const cur = getCurrentSchoolId()
  if (!cur) return false
  return bound !== cur
}

/**
 * 提示文案
 */
const GUEST_TIP = '你不是该校用户，仅可浏览'

/**
 * 进页面提示一次（每个页面每次会话只提示一次，用内存标记）
 */
function showCrossHintOnce() {
  if (!isGuest()) return
  // #ifdef H5
  if (uni.__crossHintShown) return
  uni.__crossHintShown = true
  // #endif
  uni.showToast({ title: GUEST_TIP, icon: 'none', duration: 2000 })
}

/**
 * 写操作守卫：跨校访客时拦截并提示
 * @returns {boolean} true = 已拦截（调用方应立即 return）
 */
function guardWrite() {
  if (!isGuest()) return false
  uni.showToast({ title: GUEST_TIP, icon: 'none' })
  return true
}

/**
 * 页面 onShow 时调用：检测「当前学校」与进页时是否变化，
 * 变了就重新提示一次（同一页面切学校往返的场景）
 */
function onSchoolChanged() {
  try {
    const cur = getCurrentSchoolId()
    if (cur !== uni.__lastSchoolOnPage) {
      uni.__crossHintShown = false
      uni.__lastSchoolOnPage = cur
    }
  } catch (e) { /* ignore */ }
  showCrossHintOnce()
}

export const crossSchool = { isGuest, getBoundSchoolId, getCurrentSchoolId }
export { GUEST_TIP, showCrossHintOnce, guardWrite, onSchoolChanged }
export default { isGuest, showCrossHintOnce, guardWrite, onSchoolChanged, GUEST_TIP }
