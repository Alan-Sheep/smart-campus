import { defineStore } from 'pinia'

/**
 * 学校状态管理
 * 管理当前选择的学校信息
 */
export const useSchoolStore = defineStore('school', {
  state: () => ({
    // 当前学校（默认第一个：重庆大学）
    currentSchool: uni.getStorageSync('currentSchool') || { id: 1, name: '重庆大学' },
    // 学校列表
    schoolList: []
  }),

  getters: {
    schoolName: (state) => state.currentSchool?.name || '请选择学校',
    schoolId: (state) => state.currentSchool?.id || ''
  },

  actions: {
    /**
     * 切换学校
     * @param {Object} school 学校对象 { id, name }
     */
    switchSchool(school) {
      this.currentSchool = school
      uni.setStorageSync('currentSchool', school)
      // 切换学校后，可以刷新相关数据
      uni.showToast({
        title: '已切换到' + school.name,
        icon: 'success'
      })
    },

    /**
     * 设置学校列表
     * 同时校正缓存的当前学校：
     * 若缓存里的学校在最新列表中已不存在（例如学校被改名/删除），
     * 就回退到列表第一个，避免「切走之后切不回来」。
     * @param {Array} list 学校列表
     */
    setSchoolList(list) {
      this.schoolList = list
      const cur = this.currentSchool
      if (cur && cur.id && list && list.length) {
        const match = list.find(s => s.id === cur.id)
        if (match) {
          // 用列表里的最新名称同步，避免缓存名称过期
          if (cur.name !== match.name) {
            this.currentSchool = { id: match.id, name: match.name }
            uni.setStorageSync('currentSchool', this.currentSchool)
          }
        } else {
          // 当前缓存学校已不存在 → 回退到列表第一个
          this.currentSchool = { id: list[0].id, name: list[0].name }
          uni.setStorageSync('currentSchool', this.currentSchool)
        }
      }
    }
  }
})
