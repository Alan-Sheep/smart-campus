/**
 * 页面滚动归位工具
 *
 * 背景：uni-app H5 下页面切换后滚动位置被保留（keep-alive），
 * 用户从详情页返回或再次进入业务页时，视角停在离开时的位置，
 * 看不到页面顶部的返回按钮/标题。
 *
 * 用法：在页面 onShow 里调用 scrollToTop()（放在 onShow 回调第一行）
 */

/**
 * 滚动到页面顶部（覆盖两种场景）
 * @param {string} scrollViewSelector 可选：scroll-view 的选择器（.xxx），
 *   传入时同时处理 scroll-view 内部滚动
 */
export function scrollToTop(scrollViewSelector = '') {
  // 场景1：页面级滚动（window）
  try {
    uni.pageScrollTo({ scrollTop: 0, duration: 0 })
  } catch (e) { /* ignore */ }

  // #ifdef H5
  // H5 下 pageScrollTo 有时不生效于嵌套 scroll-view，额外处理 DOM
  try {
    window.scrollTo(0, 0)
    if (scrollViewSelector) {
      const el = document.querySelector(scrollViewSelector)
      if (el) {
        // uni-app H5 的 scroll-view 内部是 .uni-scroll-view 容器
        const inner = el.querySelector('.uni-scroll-view')
        if (inner) inner.scrollTop = 0
        el.scrollTop = 0
      }
    }
  } catch (e) { /* ignore */ }
  // #endif
}

/**
 * 校验是否需要回顶：配合 onShow 使用
 * 仅在「重新进入」时回顶，首次加载不干扰
 */
let lastShowTime = 0
export function shouldScrollTopOnShow() {
  const now = Date.now()
  const need = now - lastShowTime > 800   // 800ms 内的连续触发视为同一次进入
  lastShowTime = now
  return need
}