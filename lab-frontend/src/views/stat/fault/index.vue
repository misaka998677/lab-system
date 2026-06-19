<template>
  <div class="page-shell">
    <div class="page-header">
      <div>
        <h2 class="page-title">设备故障</h2>
        <p class="page-desc">统计设备状态分布与维修工单处理情况，辅助设备维护与库存管理。</p>
      </div>
      <div>
        <el-button icon="el-icon-download" @click="doExport" :loading="exporting" type="success">导出数据</el-button>
        <el-button icon="el-icon-refresh" :loading="loading" @click="reload">刷新数据</el-button>
      </div>
    </div>

    <div class="metric-grid">
      <el-card class="metric-card gradient gradient-red" shadow="never">
        <div class="metric-label">待处理维修</div>
        <div class="metric-value">{{ pendingRepairCount }}</div>
        <div class="metric-foot">待指派 + 处理中</div>
      </el-card>
      <el-card class="metric-card gradient gradient-orange" shadow="never">
        <div class="metric-label">设备总数</div>
        <div class="metric-value">{{ totalDeviceCount }}</div>
        <div class="metric-foot">在册实验设备</div>
      </el-card>
    </div>

    <el-row :gutter="16" class="chart-row">
      <el-col :md="12" :sm="24">
        <el-card shadow="never" class="chart-card" body-style="padding:0">
          <div class="card-header">
            <div>
              <h3 class="card-title-text">设备状态分布</h3>
              <p class="card-subtitle">在用 / 维修 / 报废</p>
            </div>
          </div>
          <div ref="devicePie" class="chart"></div>
        </el-card>
      </el-col>
      <el-col :md="12" :sm="24">
        <el-card shadow="never" class="chart-card" body-style="padding:0">
          <div class="card-header">
            <div>
              <h3 class="card-title-text">维修单状态分布</h3>
              <p class="card-subtitle">待指派 / 处理中 / 已完成 / 已驳回</p>
            </div>
          </div>
          <div ref="repairPie" class="chart"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="table-card" body-style="padding:0">
      <div class="card-header">
        <div>
          <h3 class="card-title-text">待处理维修</h3>
          <p class="card-subtitle">状态：待指派 / 处理中</p>
        </div>
      </div>
      <el-table
        :data="pendingList"
        :border="false"
        empty-text="没有待处理的维修工单"
        style="width:100%"
        :header-cell-style="headerStyle"
        :row-style="rowStyle"
      >
        <el-table-column label="序号" width="70" align="center">
          <template #default="{ $index }">{{ $index + 1 }}</template>
        </el-table-column>
        <el-table-column prop="assetNo" label="设备编号" width="140" />
        <el-table-column prop="deviceName" label="设备名称" min-width="160" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag size="small" :type="repairTag(row.status)">{{ repairLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="faultDesc" label="故障描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="reportTime" label="报修时间" width="180" />
      </el-table>
    </el-card>
  </div>
</template>

<script>
import { statFault } from '@/api/stat'
import { loadECharts } from '@/utils/echarts'
import { init as initWebSocket, on as onWebSocket, close as closeWebSocket } from '@/utils/websocket'
import { getToken } from '@/utils/auth'
import request from '@/utils/request'

export default {
  name: 'StatFault',
  data() {
    return {
      loading: false,
      exporting: false,
      deviceStatus: [],
      repairStatus: [],
      pendingList: [],
      charts: [],
      _echarts: null,
      _wsUnsubscribe: null,
      headerStyle: { background: '#FAFBFC', color: '#606266', fontWeight: 500 }
    }
  },
  computed: {
    pendingRepairCount() {
      const fromStatus = this.repairStatus
        .filter(r => Number(r.status) === 0 || Number(r.status) === 1)
        .reduce((s, r) => s + Number(r.count || 0), 0)
      return fromStatus || this.pendingList.length
    },
    totalDeviceCount() {
      return this.deviceStatus.reduce((s, r) => s + Number(r.count || 0), 0)
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
    repairTag(s) {
      const map = { 0: 'danger', 1: 'warning', 2: 'success', 3: 'info' }
      return map[Number(s)] || 'info'
    },
    async doExport() {
      if (this.exporting) return
      this.exporting = true
      try {
        const resp = await request.get('/stat/device-fault/export', { responseType: 'blob' })
        const blob = new Blob([resp], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
        const url = URL.createObjectURL(blob)
        const a = document.createElement('a')
        a.href = url
        a.download = `设备故障分析_${new Date().toLocaleDateString()}.xlsx`
        a.click()
        URL.revokeObjectURL(url)
        this.$message.success('导出成功')
      } catch (e) {
        this.$message.error('导出失败')
      } finally {
        this.exporting = false
      }
    },
    repairLabel(s) {
      return ['待指派', '处理中', '已完成', '已驳回'][Number(s)] || '未知'
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
      this.devicePie = echarts.init(this.$refs.devicePie)
      this.repairPie = echarts.init(this.$refs.repairPie)
      this.charts = [this.devicePie, this.repairPie]
      return echarts
    },
    handleResize() {
      this.charts.forEach(c => { try { c.resize() } catch (e) {} })
    },
    async reload() {
      this.loading = true
      try {
        const [a] = await Promise.allSettled([statFault(), loadECharts()])
        const d = a.status === 'fulfilled' ? (a.value.data || {}) : {}
        this.deviceStatus = d.deviceStatus || []
        this.repairStatus = d.repairStatus || []
        this.pendingList = d.pendingList || []
        this.$nextTick(async () => {
          await this.initCharts()
          this.renderDevicePie()
          this.renderRepairPie()
          this.handleResize()
        })
      } catch (e) {}
      finally { this.loading = false }
    },
    renderDevicePie() {
      const palette = { 1: '#67C23A', 2: '#E6A23C', 3: '#F56C6C' }
      const nameMap = { 1: '在用', 2: '维修', 3: '报废' }
      const data = this.deviceStatus.map(x => ({
        name: nameMap[x.status] || ('状态' + x.status),
        value: Number(x.count || 0),
        itemStyle: { color: palette[x.status] || '#409EFF' }
      }))
      const total = data.reduce((s, x) => s + x.value, 0)
      if (data.length === 0 || total === 0) {
        this.devicePie.setOption({
          title: { text: '暂无设备数据', left: 'center', top: 'center', textStyle: { color: '#909399', fontSize: 14, fontWeight: 400 } }
        })
        return
      }
      this.devicePie.setOption({
        tooltip: { trigger: 'item', formatter: '{b}: {c} 台 ({d}%)' },
        legend: { bottom: 8, icon: 'circle', itemWidth: 10 },
        graphic: [{ type: 'group', left: 'center', top: '38%', children: [
          { type: 'text', style: { text: String(total), fill: '#303133', fontSize: 26, fontWeight: 'bold', textAlign: 'center' } },
          { type: 'text', style: { text: '台', fill: '#909399', fontSize: 12, textAlign: 'center', y: 24 } }
        ]}],
        series: [{
          type: 'pie',
          radius: ['50%', '75%'],
          center: ['50%', '45%'],
          avoidLabelOverlap: true,
          data,
          itemStyle: { borderColor: '#fff', borderWidth: 3, borderRadius: 6 },
          label: { show: true, formatter: '{b}\n{c}台 ({d}%)', color: '#303133' },
          emphasis: { label: { fontWeight: 'bold' } }
        }]
      })
    },
    renderRepairPie() {
      const palette = { 0: '#F56C6C', 1: '#E6A23C', 2: '#67C23A', 3: '#909399' }
      const nameMap = { 0: '待指派', 1: '处理中', 2: '已完成', 3: '已驳回' }
      const data = this.repairStatus.map(x => ({
        name: nameMap[x.status] || ('状态' + x.status),
        value: Number(x.count || 0),
        itemStyle: { color: palette[x.status] || '#409EFF' }
      }))
      const total = data.reduce((s, x) => s + x.value, 0)
      if (data.length === 0 || total === 0) {
        this.repairPie.setOption({
          title: { text: '暂无维修数据', left: 'center', top: 'center', textStyle: { color: '#909399', fontSize: 14, fontWeight: 400 } }
        })
        return
      }
      this.repairPie.setOption({
        tooltip: { trigger: 'item', formatter: '{b}: {c} 单 ({d}%)' },
        legend: { bottom: 8, icon: 'circle', itemWidth: 10 },
        graphic: [{ type: 'group', left: 'center', top: '38%', children: [
          { type: 'text', style: { text: String(total), fill: '#303133', fontSize: 26, fontWeight: 'bold', textAlign: 'center' } },
          { type: 'text', style: { text: '单', fill: '#909399', fontSize: 12, textAlign: 'center', y: 24 } }
        ]}],
        series: [{
          type: 'pie',
          radius: ['50%', '75%'],
          center: ['50%', '45%'],
          avoidLabelOverlap: true,
          data,
          itemStyle: { borderColor: '#fff', borderWidth: 3, borderRadius: 6 },
          label: { show: true, formatter: '{b}\n{c}单 ({d}%)', color: '#303133' }
        }]
      })
    },
    renderTopUsage() {
      const top = (this.topUsage || []).slice(0, 10)
      const names = top.map(x => x.itemName || x.itemCode || '未知').reverse()
      const counts = top.map(x => Number(x.qty || 0)).reverse()
      if (names.length === 0) {
        this.topUsageChart.setOption({
          title: { text: '暂无出库数据', left: 'center', top: 'center', textStyle: { color: '#909399', fontSize: 14, fontWeight: 400 } }
        })
        return
      }
      this.topUsageChart.setOption({
        tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
        grid: { left: 120, right: 60, top: 20, bottom: 30 },
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
          type: 'bar', data: counts, barWidth: 14,
          itemStyle: {
            color: new this._echarts.graphic.LinearGradient(0, 0, 1, 0,
              [{ offset: 0, color: '#FFA07A' }, { offset: 1, color: '#F56C6C' }]),
            borderRadius: [0, 6, 6, 0]
          },
          label: { show: true, position: 'right', color: '#303133', fontWeight: 'bold' }
        }]
      })
    },
    renderWarningBar() {
      const list = (this.warningList || []).slice(0, 10)
      const names = list.map(x => x.name || x.code || '未知').reverse()
      const qty = list.map(x => Number(x.qty || 0)).reverse()
      const warn = list.map(x => Number(x.warnQty || 0)).reverse()
      if (names.length === 0) {
        this.warningBar.setOption({
          title: { text: '所有耗材库存充足', left: 'center', top: 'center', textStyle: { color: '#909399', fontSize: 14, fontWeight: 400 } }
        })
        return
      }
      this.warningBar.setOption({
        tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
        legend: { bottom: 2, data: ['当前库存', '预警阈值'], icon: 'circle', itemWidth: 10 },
        grid: { left: 120, right: 60, top: 20, bottom: 40 },
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
        series: [
          {
            name: '当前库存', type: 'bar', data: qty, barWidth: 10,
            itemStyle: { color: '#F56C6C', borderRadius: [0, 4, 4, 0] },
            label: { show: true, position: 'right', color: '#303133', fontWeight: 'bold' }
          },
          {
            name: '预警阈值', type: 'bar', data: warn, barWidth: 10,
            itemStyle: { color: '#909399', borderRadius: [0, 4, 4, 0] }
          }
        ]
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
.metric-card { border-radius: 10px; overflow: hidden; }
.gradient { position: relative; color: #fff; border: none; }
.gradient .metric-label { color: rgba(255, 255, 255, 0.9); font-size: 13px; margin-bottom: 6px; }
.gradient .metric-value { color: #fff; font-size: 30px; font-weight: 700; margin-bottom: 4px; text-shadow: 0 2px 4px rgba(0, 0, 0, 0.15); }
.gradient .metric-foot { color: rgba(255, 255, 255, 0.85); font-size: 12px; }
.gradient-blue { background: linear-gradient(135deg, #4D9EFF, #1D6FEC); }
.gradient-orange { background: linear-gradient(135deg, #FFB07A, #F08F3D); }
.gradient-green { background: linear-gradient(135deg, #8FE38F, #3EB063); }
.gradient-red { background: linear-gradient(135deg, #FF8A8A, #E84545); }
.chart-row { margin-bottom: 16px; }
.chart-card { border-radius: 10px; border: 1px solid #EBEEF5; overflow: hidden; }
.chart { height: 320px; width: 100%; }
.card-header { padding: 18px 20px; border-bottom: 1px solid #F2F6FC; }
.card-title-text { margin: 0; font-size: 16px; color: #303133; font-weight: 600; }
.card-subtitle { margin: 4px 0 0; font-size: 12px; color: #909399; }
</style>
