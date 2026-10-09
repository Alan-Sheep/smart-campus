<script>
// 应用根组件
// 这里可以做一些全局的初始化操作
export default {
  onLaunch: function() {
    console.log('智慧校园小程序启动')
    // 从本地缓存读取学校信息
    const school = uni.getStorageSync('currentSchool')
    if (!school || !school.id) {
      // 没有选择学校时默认第一个（重庆大学）
      uni.setStorageSync('currentSchool', {
        id: 1,
        name: '重庆大学'
      })
    } else if (school.name === '示例大学') {
      // 兼容旧缓存：历史版本默认写的是「示例大学」，库里已改名为重庆高校
      // 若缓存里的名称与当前学校列表对不上会导致切回时找不到，这里纠正为重庆大学
      uni.setStorageSync('currentSchool', {
        id: 1,
        name: '重庆大学'
      })
    }
    // 从本地缓存读取token
    const token = uni.getStorageSync('token')
    if (token) {
      console.log('已登录')
    }
  },
  onShow: function() {
    console.log('App Show')
  },
  onHide: function() {
    console.log('App Hide')
  }
}
</script>

<style>
/* 全局样式 */
page {
  background-color: #f5f5f7;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}

/* 通用容器 */
.container {
  padding: 20rpx;
  box-sizing: border-box;
}

/* 卡片样式 */
.card {
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.05);
}

/* 文本省略 */
.ellipsis-1 {
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.ellipsis-2 {
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
}

/* 主色调 */
.primary-color {
  color: #6c4ee6;
}

.primary-bg {
  background-color: #6c4ee6;
}
</style>
