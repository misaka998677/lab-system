<template>
  <div class="page-shell">
    <div class="page-header">
      <div>
        <h2 class="page-title">库存预警</h2>
        <p class="page-desc">展示耗材库存预警、高频领用和出入库汇总，辅助采购决策。</p>
      </div>
      <div>
        <el-button icon="el-icon-download" @click="doExport" :loading="exporting" type="success">导出数据</el-button>
        <el-button icon="el-icon-refresh" :loading="loading" @click="reload">刷新数据</el-button>
      </div>
    </div>

    <el-row :gutter="16" class="chart-row">
      <el-col :md="12" :sm="24">
        <el-card shadow="never" class="chart-card" body-style="padding:0">
          <div class="card-header">
            <h3 class="card-title-text">高频领用耗材 TOP 10</h3>
            <p class="card-subtitle">累计出库数量</p>
          </div>
          <div ref="topUsageChart" class="chart"></div>
        </el-card>
      </el-col>
      <el-col :md="12" :sm="24">
        <el-card shadow="never" class="chart-card" body-style="padding:0">
          <div class="card-header">
            <h3 class="card-title-text">库存低于阈值</h3>
            <p class="card-subtitle">需要优先补充的耗材</p>
          </div>
          <div ref="warningBar" class="chart"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="table-card" body-style="padding:0">
      <div class="card-header">
        <h3 class="card-title-text">预警明细</h3>
        <p class="card-subtitle">库存量 ≤ 预警阈值，共 {{ warningList.length }} 条预警</p>
      </div>
      <el-table :data="warningList" empty-text="所有耗材库存充足" style="width:100%"
        :header-cell-style="{ background: '#FAFBFC', color: '#606266', fontWeight: 500 }">
        <el-table-column label="序号" width="70" align="center">
          <template #default="{ $index }">{{ $index + 1 }}</template>
        </el-table-column>
        <el-table-column prop="code" label="耗材编号" width="140" />
        <el-table-column prop="name" label="耗材名称" min-width="160" />
        <el-table-column prop="labName" label="存放实验室" min-width="160">
          <template #default="{ row }"><span>{{ row.labName || '—' }}</span></template>
        </el-table-column>
        <el-table-column label="库存 / 预警" width="180">
          <template #default="{ row }">
            <span style="color:#F56C6C;font-weight:600">{{ row.qty }}</span>
            <span style="color:#909399"> / {{ row.warnQty }} {{ row.unit || '' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="缺口" width="120" align="right">
          <template #default="{ row }">
            <el-tag type="danger" size="small">
              {{ Math.max(0, Number(row.warnQty || 0) - Number(row.qty || 0)) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default><el-tag type="danger" size="small">库存不足</el-tag></template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script>
import { statStockWarning } from '@/api/stat'
import { loadECharts } from '@/utils/echarts'
import request from '@/utils/request'

export default {
  name: 'StatStockWarning',
  data() {
    return {
      loading: false,
      exporting: false,
      warningList: [],
      topUsage: [],
      charts: [],
      _echarts: null
    }
  },
  mounted() {
    this.reload()
    window.addEventListener('resize', this.handleResize)
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.handleResize)
    this.charts.forEach(c => { try { c.dispose() } catch (e) {} })
  },
  methods: {
    async doExport() {
      if (this.exporting) return
      this.exporting = true
      try {
        const resp = await request.get('/stat/stock/export', { responseType: 'blob' })
        const blob = new Blob([resp], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
        const url = URL.createObjectURL(blob)
        const a = document.createElement('a')
        a.href = url
        a.download = `库存预警数据_${new Date().toLocaleDateString()}.xlsx`
        a.click()
        URL.revokeObjectURL(url)
        this.$message.success('导出成功')
      } catch (e) {
        this.$message.error('导出失败')
      } finally {
        this.exporting = false
      }
    },
    async reload() {
      this.loading = true
      try {
        const res = await statStockWarning()
        const d = res.data || {}
        this.warningList = d.warningList || []
        this.topUsage = d.topUsage || []
        this.$nextTick(async () => {
          await this.initCharts()
          this.renderTopUsage()
          this.renderWarningBar()
          this.handleResize()
        })
      } catch (e) { /* 接口无权限时静默 */ }
      finally { this.loading = false }
    },
    async initCharts() {
      // 先销毁已有实例，避免重复初始化警告
      this.charts.forEach(c => { try { c.dispose() } catch (e) {} })
      this.charts = []

      const echarts = this._echarts || (this._echarts = await loadECharts())
      this.topUsageChart = echarts.init(this.$refs.topUsageChart)
      this.warningBar = echarts.init(this.$refs.warningBar)
      this.charts = [this.topUsageChart, this.warningBar]
      return echarts
    },
    handleResize() {
      this.charts.forEach(c => { try { c.resize() } catch (e) {} })
    },
    renderTopUsage() {
      const top = (this.topUsage || []).slice(0, 10)
      const names = top.map(x => x.itemName || x.itemCode || '未知').reverse()
      const counts = top.map(x => Number(x.qty || 0)).reverse()
      if (names.length === 0) {
        this.topUsageChart.setOption({ title: { text: '暂无出库数据', left: 'center', top: 'center', textStyle: { color: '#909399', fontSize: 14 } } })
        return
      }
      this.topUsageChart.setOption({
        tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
        grid: { left: 120, right: 60, top: 20, bottom: 30 },
        xAxis: { type: 'value', axisLine: { lineStyle: { color: '#D8DEE9' } }, axisLabel: { color: '#606266' }, splitLine: { lineStyle: { type: 'dashed', color: '#ECEFF4' } } },
        yAxis: { type: 'category', data: names, axisLine: { lineStyle: { color: '#D8DEE9' } }, axisLabel: { color: '#303133' } },
        series: [{
          type: 'bar', data: counts, barWidth: 14,
          itemStyle: { color: new this._echarts.graphic.LinearGradient(0, 0, 1, 0, [{ offset: 0, color: '#FFA07A' }, { offset: 1, color: '#F56C6C' }]), borderRadius: [0, 6, 6, 0] },
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
        this.warningBar.setOption({ title: { text: '所有耗材库存充足', left: 'center', top: 'center', textStyle: { color: '#909399', fontSize: 14 } } })
        return
      }
      this.warningBar.setOption({
        tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
        legend: { bottom: 2, data: ['当前库存', '预警阈值'], icon: 'circle', itemWidth: 10 },
        grid: { left: 120, right: 60, top: 20, bottom: 40 },
        xAxis: { type: 'value', axisLine: { lineStyle: { color: '#D8DEE9' } }, axisLabel: { color: '#606266' }, splitLine: { lineStyle: { type: 'dashed', color: '#ECEFF4' } } },
        yAxis: { type: 'category', data: names, axisLine: { lineStyle: { color: '#D8DEE9' } }, axisLabel: { color: '#303133' } },
        series: [
          { name: '当前库存', type: 'bar', data: qty, barWidth: 10, itemStyle: { color: '#F56C6C', borderRadius: [0, 4, 4, 0] }, label: { show: true, position: 'right', color: '#303133', fontWeight: 'bold' } },
          { name: '预警阈值', type: 'bar', data: warn, barWidth: 10, itemStyle: { color: '#909399', borderRadius: [0, 4, 4, 0] } }
        ]
      })
    }
  }
}
</script>

<style scoped lang="scss">
.chart-row { margin-bottom: 16px; }
.chart-card { border-radius: 10px; border: 1px solid #EBEEF5; overflow: hidden; }
.chart { height: 320px; width: 100%; }
.card-header { padding: 18px 20px; border-bottom: 1px solid #F2F6FC; }
.card-title-text { margin: 0; font-size: 16px; color: #303133; font-weight: 600; }
.card-subtitle { margin: 4px 0 0; font-size: 12px; color: #909399; }
</style>
