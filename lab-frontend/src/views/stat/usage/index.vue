<template>
  <div class="page-shell">
    <div class="page-header">
      <div>
        <h2 class="page-title">实验室使用率分析</h2>
        <p class="page-desc">按预约次数和使用时长统计各实验室运行情况，辅助安排实验教学资源。</p>
      </div>
      <div>
        <el-button icon="el-icon-download" @click="doExport" :loading="exporting" type="success">导出数据</el-button>
        <el-button icon="el-icon-refresh" :loading="loading" @click="reload">刷新数据</el-button>
      </div>
    </div>

    <div class="metric-grid">
      <el-card class="metric-card" shadow="never">
        <div class="metric-label">本月预约</div>
        <div class="metric-value">{{ monthTotal || 0 }}</div>
        <div class="metric-foot">单位：次</div>
      </el-card>
      <el-card class="metric-card" shadow="never">
        <div class="metric-label">实验室数量</div>
        <div class="metric-value">{{ ranking.length }}</div>
        <div class="metric-foot">参与统计的实验室</div>
      </el-card>
      <el-card class="metric-card" shadow="never">
        <div class="metric-label">统计周期</div>
        <div class="metric-value">近 7 天</div>
        <div class="metric-foot">{{ trendRange }}</div>
      </el-card>
      <el-card class="metric-card" shadow="never">
        <div class="metric-label">日均预约</div>
        <div class="metric-value">{{ avgPerDay }}</div>
        <div class="metric-foot">平均值</div>
      </el-card>
    </div>

    <el-row :gutter="16" class="chart-row">
      <el-col :md="12" :sm="24">
        <el-card shadow="never" class="chart-card" body-style="padding:0">
          <div class="card-header">
            <div>
              <h3 class="card-title-text">实验室预约 TOP 10</h3>
              <p class="card-subtitle">横向柱状图显示预约次数</p>
            </div>
          </div>
          <div ref="barChart" class="chart"></div>
        </el-card>
      </el-col>
      <el-col :md="12" :sm="24">
        <el-card shadow="never" class="chart-card" body-style="padding:0">
          <div class="card-header">
            <div>
              <h3 class="card-title-text">近 7 天预约趋势</h3>
              <p class="card-subtitle">按日期统计预约数量变化</p>
            </div>
          </div>
          <div ref="lineChart" class="chart"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="table-card" body-style="padding:0">
      <div class="card-header">
        <div>
          <h3 class="card-title-text">实验室预约排行</h3>
          <p class="card-subtitle">按预约次数和使用时长排序</p>
        </div>
      </div>
      <el-table :data="ranking" :border="false" empty-text="暂无预约记录" style="width:100%" :header-cell-style="headerStyle" :row-style="rowStyle">
        <el-table-column label="排名" width="80" align="center">
          <template #default="{ $index }">
            <el-tag v-if="$index < 3" size="mini" :type="rankTag($index)">{{ $index + 1 }}</el-tag>
            <span v-else style="color:#909399">{{ $index + 1 }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="labName" label="实验室名称" min-width="180" />
        <el-table-column prop="reserveCount" label="预约次数" width="120" align="right" />
        <el-table-column prop="useHours" label="使用时长(小时)" width="160" align="right">
          <template #default="{ row }">
            <span style="color:#303133">{{ Number(row.useHours || 0).toFixed(1) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="占比" min-width="220">
          <template #default="{ row }">
            <el-progress
              :percentage="ratio(row.reserveCount, maxReserve)"
              :show-text="false"
              :stroke-width="8"
              :color="progressColor(ratio(row.reserveCount, maxReserve))"
            />
            <div class="muted small" style="margin-top: 4px">{{ row.reserveCount || 0 }} / {{ maxReserve || 0 }}</div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script>
import { statUsage } from '@/api/stat'
import { loadECharts } from '@/utils/echarts'
import { init as initWebSocket, on as onWebSocket, close as closeWebSocket } from '@/utils/websocket'
import { getToken } from '@/utils/auth'
import request from '@/utils/request'

export default {
  name: 'StatUsage',
  data() {
    return {
      loading: false,
      exporting: false,
      ranking: [],
      trend: [],
      monthTotal: 0,
      charts: [],
      _echarts: null,
      _wsUnsubscribe: null,
      headerStyle: { background: '#FAFBFC', color: '#606266', fontWeight: 500 }
    }
  },
  computed: {
    maxReserve() {
      return this.ranking.reduce((m, r) => Math.max(m, Number(r.reserveCount || 0)), 0)
    },
    avgPerDay() {
      const days = this.trend.length || 7
      const total = this.trend.reduce((s, r) => s + Number(r.count || 0), 0)
      return days > 0 ? (total / days).toFixed(1) : '0'
    },
    trendRange() {
      if (!this.trend.length) return '—'
      const first = this.trend[0].day
      const last = this.trend[this.trend.length - 1].day
      return `${first} ~ ${last}`
    }
  },
  mounted() {
    this.reload()
    window.addEventListener('resize', this.handleResize)
    const token = getToken()
    if (token) {
      initWebSocket(token)
      this._wsUnsubscribe = onWebSocket('refresh-overview', this.handleWebSocketMessage)
    }
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.handleResize)
    this.charts.forEach(c => { try { c.dispose() } catch (e) {} })
    if (this._wsUnsubscribe) this._wsUnsubscribe()
    closeWebSocket()
  },
  methods: {
    rankTag(i) { return ['danger', 'warning', 'primary'][i] || 'info' },
    async doExport() {
      if (this.exporting) return
      this.exporting = true
      try {
        const resp = await request.get('/stat/usage/export', { responseType: 'blob' })
        const blob = new Blob([resp], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
        const url = URL.createObjectURL(blob)
        const a = document.createElement('a')
        a.href = url
        a.download = `实验室使用率_${new Date().toLocaleDateString()}.xlsx`
        a.click()
        URL.revokeObjectURL(url)
        this.$message.success('导出成功')
      } catch (e) {
        this.$message.error('导出失败')
      } finally {
        this.exporting = false
      }
    },
    progressColor(p) {
      if (p >= 80) return '#409EFF'
      if (p >= 50) return '#67B0FF'
      if (p >= 20) return '#A0CFFF'
      return '#C0D8FF'
    },
    rowStyle({ rowIndex }) {
      return rowIndex % 2 === 1 ? { background: '#FAFBFC' } : {}
    },
    handleWebSocketMessage(msg) {
      this.reload()
    },
    async initCharts() {
      // 先销毁已有实例，避免重复初始化警告
      this.charts.forEach(c => { try { c.dispose() } catch (e) {} })
      this.charts = []

      const echarts = this._echarts || (this._echarts = await loadECharts())
      this.barChart = echarts.init(this.$refs.barChart)
      this.lineChart = echarts.init(this.$refs.lineChart)
      this.charts = [this.barChart, this.lineChart]
      return echarts
    },
    handleResize() {
      this.charts.forEach(c => { try { c.resize() } catch (e) {} })
    },
    ratio(v, max) {
      const value = Number(v || 0)
      if (!max) return 0
      const r = Math.round((value / max) * 100)
      return Math.max(0, Math.min(100, r))
    },
    async reload() {
      this.loading = true
      try {
        // async-parallel：同时拉数据与加载图表库
        const [r] = await Promise.all([statUsage(), loadECharts()])
        const data = r.data || {}
        this.ranking = data.ranking || []
        this.trend = data.trend || []
        this.monthTotal = data.monthTotal || 0
        this.$nextTick(async () => {
          await this.initCharts()
          this.renderBar(); this.renderLine(); this.handleResize()
        })
      } catch (e) {}
      finally { this.loading = false }
    },
    renderBar() {
      const top = (this.ranking || []).slice(0, 10)
      const names = top.map(x => x.labName || '未知').reverse()
      const counts = top.map(x => Number(x.reserveCount || 0)).reverse()
      if (names.length === 0) {
        this.barChart.setOption({
          title: { text: '暂无预约数据', left: 'center', top: 'center', textStyle: { color: '#909399', fontSize: 14, fontWeight: 400 } }
        })
        return
      }
      this.barChart.setOption({
        tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
        grid: { left: 120, right: 60, top: 30, bottom: 30 },
        xAxis: {
          type: 'value',
          axisLine: { lineStyle: { color: '#D8DEE9' } },
          axisLabel: { color: '#606266' },
          splitLine: { lineStyle: { type: 'dashed', color: '#ECEFF4' } }
        },
        yAxis: {
          type: 'category',
          data: names,
          axisLine: { lineStyle: { color: '#D8DEE9' } },
          axisLabel: { color: '#303133' }
        },
        series: [{
          type: 'bar', data: counts, barWidth: 18,
          itemStyle: {
            color: new this._echarts.graphic.LinearGradient(0, 0, 1, 0,
              [{ offset: 0, color: '#67B0FF' }, { offset: 1, color: '#3D7FFF' }]),
            borderRadius: [0, 6, 6, 0]
          },
          label: { show: true, position: 'right', color: '#303133', fontWeight: 'bold' }
        }]
      })
    },
    renderLine() {
      const dates = (this.trend || []).slice(-7).map(x => x.day || '')
      const values = this.trend.map(x => Number(x.count || 0)).slice(-7)
      this.lineChart.setOption({
        tooltip: { trigger: 'axis' },
        grid: { left: 50, right: 30, top: 30, bottom: 30 },
        xAxis: {
          type: 'category',
          data: dates,
          axisLine: { lineStyle: { color: '#D8DEE9' } },
          axisLabel: { color: '#606266' }
        },
        yAxis: {
          type: 'value',
          minInterval: 1,
          axisLine: { lineStyle: { color: '#D8DEE9' } },
          axisLabel: { color: '#606266' },
          splitLine: { lineStyle: { type: 'dashed', color: '#ECEFF4' } }
        },
        series: [{
          type: 'line', smooth: true, data: values, symbolSize: 8,
          itemStyle: { color: '#67C23A' },
          lineStyle: { width: 3, color: '#67C23A' },
          areaStyle: {
            color: new this._echarts.graphic.LinearGradient(0, 0, 0, 1,
              [{ offset: 0, color: 'rgba(103,194,58,0.30)' }, { offset: 1, color: 'rgba(103,194,58,0.02)' }])
          },
          label: { show: true, position: 'top', color: '#303133' }
        }]
      })
    }
  }
}
</script>

<style scoped lang="scss">
.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 16px;
}
@media (max-width: 1200px) { .metric-grid { grid-template-columns: repeat(2, 1fr); } }
.metric-card { border-radius: 10px; border: 1px solid #EBEEF5; }
.metric-label { color: #909399; font-size: 13px; margin-bottom: 6px; }
.metric-value { font-size: 30px; font-weight: 700; color: #409EFF; margin-bottom: 4px; }
.metric-foot { color: #B1B3B8; font-size: 12px; }
.chart-row { margin-bottom: 16px; }
.chart-card { border-radius: 10px; border: 1px solid #EBEEF5; overflow: hidden; }
.chart { height: 320px; width: 100%; }
.card-header { padding: 18px 20px; border-bottom: 1px solid #F2F6FC; }
.card-title-text { margin: 0; font-size: 16px; color: #303133; font-weight: 600; }
.card-subtitle { margin: 4px 0 0; font-size: 12px; color: #909399; }
</style>
