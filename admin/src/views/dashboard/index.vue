<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 class="page-title">数据概览</h2>
        <p class="page-desc">实时掌握平台运营数据</p>
      </div>
      <div class="header-actions">
        <el-select
          v-model="schoolId"
          placeholder="全部学校"
          clearable
          style="width: 170px"
          @change="loadAll"
        >
          <el-option
            v-for="s in schools"
            :key="s.id"
            :label="s.name"
            :value="s.id"
          />
        </el-select>
        <el-button :icon="Refresh" :loading="loading" @click="loadAll">刷新</el-button>
      </div>
    </div>

    <!-- ============ 核心指标卡 ============ -->
    <el-row :gutter="16" class="stat-row">
      <el-col
        v-for="card in statCards"
        :key="card.key"
        :xs="12"
        :sm="12"
        :md="6"
      >
        <div class="stat-card" :class="card.theme">
          <div class="stat-head">
            <span class="stat-label">{{ card.label }}</span>
            <div class="stat-icon">
              <el-icon :size="20"><component :is="card.icon" /></el-icon>
            </div>
          </div>
          <div class="stat-value">
            <span class="num">{{ card.value }}</span>
            <span v-if="card.unit" class="unit">{{ card.unit }}</span>
          </div>
          <div class="stat-foot">{{ card.foot }}</div>
        </div>
      </el-col>
    </el-row>

    <!-- ============ 趋势图 ============ -->
    <el-row :gutter="16">
      <el-col :xs="24" :lg="16">
        <div class="sc-card">
          <div class="card-head">
            <h3>用户增长趋势</h3>
            <el-radio-group v-model="growthRange" size="small" @change="loadAll">
              <el-radio-button value="day">近7天</el-radio-button>
              <el-radio-button value="month">近30天</el-radio-button>
            </el-radio-group>
          </div>
          <BaseChart :option="userGrowthOption" height="300px" />
        </div>
      </el-col>

      <el-col :xs="24" :lg="8">
        <div class="sc-card">
          <div class="card-head"><h3>业务模块占比</h3></div>
          <BaseChart :option="moduleRatioOption" height="300px" />
        </div>
      </el-col>
    </el-row>

    <!-- ============ 订单 & 消息 ============ -->
    <el-row :gutter="16">
      <el-col :xs="24" :lg="16">
        <div class="sc-card">
          <div class="card-head">
            <h3>订单量趋势</h3>
            <span class="card-sub">单位：单</span>
          </div>
          <BaseChart :option="orderStatsOption" height="300px" />
        </div>
      </el-col>

      <el-col :xs="24" :lg="8">
        <div class="sc-card">
          <div class="card-head"><h3>消息类型分布</h3></div>
          <BaseChart :option="messageRatioOption" height="300px" />
        </div>
      </el-col>
    </el-row>

    <!-- ============ 消息趋势 ============ -->
    <el-row :gutter="16">
      <el-col :span="24">
        <div class="sc-card">
          <div class="card-head"><h3>消息发送趋势</h3></div>
          <BaseChart :option="messageTrendOption" height="260px" />
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import BaseChart from '@/components/BaseChart.vue'
import { statsApi, schoolApi } from '@/api'

const loading = ref(false)
const schoolId = ref(null)
const schools = ref([])
const growthRange = ref('day')

const overview = ref({})
const userGrowth = ref({ dates: [], userCounts: [] })
const orderStatsRaw = ref({ dates: [] })
const moduleRatio = ref([])
const messageTrendRaw = ref({ dates: [], counts: [] })
const messageRatio = ref([])

/* ---------- 指标卡 ---------- */
const statCards = computed(() => {
  const o = overview.value
  return [
    {
      key: 'user',
      label: '用户总数',
      value: o.userTotal ?? 0,
      unit: '人',
      foot: `今日新增 ${o.todayUserCount ?? 0} 人`,
      icon: 'User',
      theme: 'theme-purple'
    },
    {
      key: 'order',
      label: '订单总数',
      value: o.orderTotal ?? 0,
      unit: '单',
      foot: `今日新增 ${o.todayOrderCount ?? 0} 单`,
      icon: 'List',
      theme: 'theme-blue'
    },
    {
      key: 'amount',
      label: '交易总额',
      value: `¥${Number(o.totalAmount ?? 0).toFixed(2)}`,
      unit: '',
      foot: `今日 ¥${Number(o.todayAmount ?? 0).toFixed(2)}`,
      icon: 'Money',
      theme: 'theme-green'
    },
    {
      key: 'message',
      label: '消息总数',
      value: o.messageTotal ?? 0,
      unit: '条',
      foot: `未读 ${o.unreadMessageCount ?? 0} 条`,
      icon: 'ChatDotRound',
      theme: 'theme-orange'
    }
  ]
})

/* ---------- 图表配置 ---------- */
const AXIS_STYLE = {
  axisLine: { lineStyle: { color: '#e5e8f0' } },
  axisLabel: { color: '#8b93a5', fontSize: 12 },
  axisTick: { show: false }
}

const userGrowthOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 10, right: 20, top: 30, bottom: 10, containLabel: true },
  xAxis: { type: 'category', boundaryGap: false, data: userGrowth.value.dates || [], ...AXIS_STYLE },
  yAxis: { type: 'value', splitLine: { lineStyle: { color: '#f2f4f9' } }, ...AXIS_STYLE },
  series: [
    {
      name: '新增用户',
      type: 'line',
      smooth: true,
      symbolSize: 7,
      data: userGrowth.value.userCounts || [],
      itemStyle: { color: '#6c4ee6' },
      lineStyle: { width: 3 },
      areaStyle: {
        color: {
          type: 'linear',
          x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: 'rgba(108,78,230,0.32)' },
            { offset: 1, color: 'rgba(108,78,230,0.02)' }
          ]
        }
      }
    }
  ]
}))

const moduleRatioOption = computed(() => ({
  tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
  legend: {
    orient: 'vertical',
    right: 0,
    top: 'center',
    itemWidth: 8,
    itemHeight: 8,
    textStyle: { color: '#4b5563', fontSize: 12 }
  },
  color: ['#6c4ee6', '#4f8cff', '#18a058', '#f0a020', '#d03050', '#13c2c2', '#eb2f96'],
  series: [
    {
      type: 'pie',
      radius: ['48%', '72%'],
      center: ['36%', '50%'],
      avoidLabelOverlap: true,
      itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
      label: { show: false },
      data: (moduleRatio.value || []).map((i) => ({ name: i.name, value: i.value }))
    }
  ]
}))

const orderStatsOption = computed(() => {
  const o = orderStatsRaw.value || {}
  const series = [
    { name: '快递代取', key: 'expressCounts', color: '#6c4ee6' },
    { name: '打印服务', key: 'printCounts', color: '#4f8cff' },
    { name: '校园跑腿', key: 'errandCounts', color: '#18a058' },
    { name: '代课服务', key: 'substituteCounts', color: '#f0a020' }
  ]
  return {
    tooltip: { trigger: 'axis' },
    legend: { top: 0, itemWidth: 10, itemHeight: 10, textStyle: { color: '#4b5563' } },
    grid: { left: 10, right: 20, top: 44, bottom: 10, containLabel: true },
    xAxis: { type: 'category', data: o.dates || [], ...AXIS_STYLE },
    yAxis: { type: 'value', splitLine: { lineStyle: { color: '#f2f4f9' } }, ...AXIS_STYLE },
    series: series.map((s) => ({
      name: s.name,
      type: 'bar',
      stack: 'total',
      barMaxWidth: 26,
      itemStyle: { color: s.color, borderRadius: [0, 0, 0, 0] },
      data: o[s.key] || []
    }))
  }
})

const messageTrendOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 10, right: 20, top: 30, bottom: 10, containLabel: true },
  xAxis: { type: 'category', boundaryGap: false, data: messageTrendRaw.value.dates || [], ...AXIS_STYLE },
  yAxis: { type: 'value', splitLine: { lineStyle: { color: '#f2f4f9' } }, ...AXIS_STYLE },
  series: [
    {
      name: '消息数',
      type: 'line',
      smooth: true,
      symbolSize: 6,
      data: messageTrendRaw.value.counts || [],
      itemStyle: { color: '#4f8cff' },
      lineStyle: { width: 3 },
      areaStyle: {
        color: {
          type: 'linear',
          x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: 'rgba(79,140,255,0.3)' },
            { offset: 1, color: 'rgba(79,140,255,0.02)' }
          ]
        }
      }
    }
  ]
}))

const messageRatioOption = computed(() => ({
  tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
  legend: { bottom: 0, itemWidth: 8, itemHeight: 8, textStyle: { color: '#4b5563', fontSize: 12 } },
  color: ['#6c4ee6', '#4f8cff', '#18a058'],
  series: [
    {
      type: 'pie',
      radius: ['42%', '66%'],
      center: ['50%', '44%'],
      itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
      label: { show: true, formatter: '{d}%', color: '#4b5563', fontSize: 12 },
      data: (messageRatio.value || []).map((i) => ({ name: i.name, value: i.value }))
    }
  ]
}))

/* ---------- 数据加载 ---------- */
async function loadSchools() {
  try {
    schools.value = await schoolApi.all()
  } catch {
    schools.value = []
  }
}

async function loadOverview() {
  overview.value = (await statsApi.overview(schoolId.value)) || {}
}

async function loadUserGrowth() {
  userGrowth.value =
    (await statsApi.userGrowth({ type: growthRange.value, schoolId: schoolId.value })) || {}
}

async function loadOrderStats() {
  orderStatsRaw.value =
    (await statsApi.orderStats({ type: growthRange.value, schoolId: schoolId.value })) || {}
}

async function loadModuleRatio() {
  moduleRatio.value = (await statsApi.moduleRatio(schoolId.value)) || []
}

async function loadMessageTrend() {
  messageTrendRaw.value =
    (await statsApi.messageTrend({ type: growthRange.value, schoolId: schoolId.value })) || {}
}

async function loadMessageRatio() {
  messageRatio.value = (await statsApi.messageTypeRatio(schoolId.value)) || []
}

async function loadAll() {
  loading.value = true
  // 各图表接口互相独立，并行请求以缩短等待
  const tasks = [
    loadOverview(),
    loadUserGrowth(),
    loadOrderStats(),
    loadModuleRatio(),
    loadMessageTrend(),
    loadMessageRatio()
  ]
  await Promise.allSettled(tasks)
  loading.value = false
}

onMounted(() => {
  loadSchools()
  loadAll()
})
</script>

<style lang="scss" scoped>
.header-actions {
  display: flex;
  gap: 10px;
  align-items: center;
}

.stat-row {
  margin-bottom: 4px;
}

/* ============ 指标卡 ============ */
.stat-card {
  background: #fff;
  border-radius: var(--sc-radius);
  padding: 20px;
  margin-bottom: 16px;
  position: relative;
  overflow: hidden;
  box-shadow: var(--sc-shadow);
  border: 1px solid rgba(235, 238, 245, 0.9);
  transition: transform 0.25s ease, box-shadow 0.25s ease;

  &:hover {
    transform: translateY(-3px);
    box-shadow: var(--sc-shadow-hover);
  }

  /* 顶部渐变条 */
  &::after {
    content: '';
    position: absolute;
    left: 0;
    top: 0;
    width: 100%;
    height: 3px;
  }

  &.theme-purple::after {
    background: linear-gradient(90deg, #6c4ee6, #a78bfa);
  }
  &.theme-blue::after {
    background: linear-gradient(90deg, #2080f0, #60a5fa);
  }
  &.theme-green::after {
    background: linear-gradient(90deg, #18a058, #4ade80);
  }
  &.theme-orange::after {
    background: linear-gradient(90deg, #f0a020, #fbbf24);
  }
}

.stat-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.stat-label {
  font-size: 13px;
  color: var(--sc-text-secondary);
}

.stat-icon {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.theme-purple .stat-icon {
  background: linear-gradient(135deg, #6c4ee6, #a78bfa);
  box-shadow: 0 6px 14px rgba(108, 78, 230, 0.28);
}
.theme-blue .stat-icon {
  background: linear-gradient(135deg, #2080f0, #60a5fa);
  box-shadow: 0 6px 14px rgba(32, 128, 240, 0.28);
}
.theme-green .stat-icon {
  background: linear-gradient(135deg, #18a058, #4ade80);
  box-shadow: 0 6px 14px rgba(24, 160, 88, 0.28);
}
.theme-orange .stat-icon {
  background: linear-gradient(135deg, #f0a020, #fbbf24);
  box-shadow: 0 6px 14px rgba(240, 160, 32, 0.28);
}

.stat-value {
  display: flex;
  align-items: baseline;
  gap: 5px;
  margin-bottom: 8px;

  .num {
    font-size: 27px;
    font-weight: 700;
    color: var(--sc-text);
    line-height: 1.1;
  }

  .unit {
    font-size: 13px;
    color: var(--sc-text-secondary);
  }
}

.stat-foot {
  font-size: 12px;
  color: var(--sc-text-secondary);
}

/* ============ 图表卡 ============ */
.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;

  h3 {
    font-size: 15px;
    font-weight: 600;
    margin: 0;
    color: var(--sc-text);
  }
}

.card-sub {
  font-size: 12px;
  color: var(--sc-text-secondary);
}
</style>
