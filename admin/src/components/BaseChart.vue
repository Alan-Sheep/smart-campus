<template>
  <div ref="el" class="chart" :style="{ height }"></div>
</template>

<script setup>
/**
 * ECharts 通用封装
 * - 自适应容器尺寸（ResizeObserver）
 * - option 变化自动更新
 * - 组件卸载时销毁实例，避免内存泄漏
 */
import { ref, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  option: { type: Object, required: true },
  height: { type: String, default: '300px' }
})

const el = ref(null)
let chart = null
let observer = null

function render() {
  if (!el.value) return
  if (!chart) {
    chart = echarts.init(el.value)
  }
  // notMerge=true：避免旧配置残留
  chart.setOption(props.option, true)
}

function resize() {
  chart && chart.resize()
}

onMounted(async () => {
  await nextTick()
  render()
  window.addEventListener('resize', resize)
  // 侧边栏折叠/展开会改变容器宽度，用 ResizeObserver 感知
  if (window.ResizeObserver && el.value) {
    observer = new ResizeObserver(() => resize())
    observer.observe(el.value)
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  if (observer) {
    observer.disconnect()
    observer = null
  }
  if (chart) {
    chart.dispose()
    chart = null
  }
})

watch(
  () => props.option,
  () => render(),
  { deep: true }
)
</script>

<style scoped>
.chart {
  width: 100%;
}
</style>
