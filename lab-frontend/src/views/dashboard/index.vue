<template>
  <page-shell :title="pageTitle" :desc="pageDesc">
    <div class="dashboard-page">
      <div class="metric-row">
        <el-card v-for="m in metrics" :key="m.key" class="metric-card" shadow="never" body-style="padding:20px">
          <div class="metric-icon" :style="{ background: m.bg, color: m.color }">
            <i :class="m.icon"></i>
          </div>
          <div class="metric-info">
            <div class="metric-label">{{ m.label }}</div>
            <div class="metric-value">{{ m.value }}</div>
            <div class="metric-foot" v-if="m.trend !== undefined">
              <span class="metric-trend" :class="m.trend >= 0 ? 'up' : 'down'">
                <i :class="m.trend >= 0 ? 'el-icon-top' : 'el-icon-bottom'"></i>
                {{ Math.abs(m.trend) }}%
              </span>
              <span class="metric-trend-label">较上周</span>
            </div>
          </div>
        </el-card>
      </div>

      <template v-if="isStudentDashboard">
        <el-row :gutter="16" class="chart-row">
          <el-col :md="16" :sm="24">
            <el-card shadow="never" class="chart-card" body-style="padding:0">
              <div class="card-header">
                <div>
                  <h3 class="card-title-text">我的预约记录</h3>
                  <p class="card-subtitle">最近预约的实验室使用记录</p>
                </div>
              </div>
              <el-table :data="myReservations" size="medium" empty-text="暂无预约记录" style="width:100%">
                <el-table-column prop="reserveNo" label="预约编号" width="180" />
                <el-table-column prop="labName" label="实验室" min-width="160" />
                <el-table-column prop="purpose" label="用途" min-width="180" show-overflow-tooltip />
                <el-table-column label="预约时间" width="280">
                  <template #default="{ row }">
                    <span class="date-text">{{ formatTime(row.startTime) }} ~ {{ formatTime(row.endTime, true) }}</span>
                  </template>
                </el-table-column>
                <el-table-column label="状态" width="120">
                  <template #default="{ row }">
                    <el-tag size="mini" :type="reservationStatusTag(row.status).type" effect="dark">
                      {{ reservationStatusTag(row.status).text }}
                    </el-tag>
                  </template>
                </el-table-column>
              </el-table>
            </el-card>
          </el-col>
          <el-col :md="8" :sm="24">
            <el-card shadow="never" class="chart-card" body-style="padding:0">
              <div class="card-header">
                <div>
                  <h3 class="card-title-text">快捷操作</h3>
                  <p class="card-subtitle">常用功能入口</p>
                </div>
              </div>
              <div class="quick-actions">
                <el-button type="primary" icon="el-icon-date" @click="goTo('/reserve/mine')">查看我的预约</el-button>
                <el-button type="success" icon="el-icon-circle-check" @click="goTo('/reserve/check')">签到记录</el-button>
                <el-button type="warning" icon="el-icon-s-tools" @click="goTo('/lab/device')">设备报修</el-button>
                <el-button type="info" icon="el-icon-office-building" @click="goTo('/lab/room')">实验室档案</el-button>
                <el-button type="danger" icon="el-icon-document" @click="goTo('/lab/repair/mine')">我的报修记录</el-button>
              </div>
            </el-card>
            <el-card shadow="never" class="chart-card" body-style="padding:0" style="margin-top:16px">
              <div class="card-header">
                <div>
                  <h3 class="card-title-text">我的报修记录</h3>
                  <p class="card-subtitle">最近提交的设备报修</p>
                </div>
              </div>
              <el-table :data="myRepairs" size="medium" empty-text="暂无报修记录" style="width:100%">
                <el-table-column prop="deviceName" label="设备" min-width="140" />
                <el-table-column label="状态" width="100">
                  <template #default="{ row }">
                    <el-tag size="mini" :type="repairStatusTag(row.status).type" effect="dark">
                      {{ repairStatusTag(row.status).text }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="报修时间" width="150">
                  <template #default="{ row }">
                    <span class="date-text">{{ formatTime(row.reportTime) }}</span>
                  </template>
                </el-table-column>
              </el-table>
            </el-card>
          </el-col>
        </el-row>
      </template>

      <template v-else-if="isTeacherDashboard">
        <el-row :gutter="16" class="chart-row">
          <el-col :md="12" :sm="24">
            <el-card shadow="never" class="chart-card" body-style="padding:0">
              <div class="card-header">
                <div>
                  <h3 class="card-title-text">待审核预约</h3>
                  <p class="card-subtitle">本学院学生提交的等待审核预约</p>
                </div>
              </div>
              <el-table :data="pendingReservations" size="medium" empty-text="暂无待审核预约" style="width:100%">
                <el-table-column prop="reserveNo" label="预约编号" width="180" />
                <el-table-column prop="applicant" label="申请人" width="120" />
                <el-table-column prop="labName" label="实验室" min-width="160" />
                <el-table-column prop="purpose" label="用途" min-width="180" show-overflow-tooltip />
                <el-table-column label="预约时间" width="280">
                  <template #default="{ row }">
                    <span class="date-text">{{ formatTime(row.startTime) }} ~ {{ formatTime(row.endTime, true) }}</span>
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="140">
                  <template #default="{ row }">
                    <el-button size="mini" type="success" @click="goTo(`/reserve/audit?id=${row.id}`)">审核</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </el-card>
          </el-col>
          <el-col :md="12" :sm="24">
            <el-card shadow="never" class="chart-card" body-style="padding:0">
              <div class="card-header">
                <div>
                  <h3 class="card-title-text">我的预约</h3>
                  <p class="card-subtitle">您自己的实验室预约记录</p>
                </div>
              </div>
              <el-table :data="myReservations" size="medium" empty-text="暂无预约记录" style="width:100%">
                <el-table-column prop="reserveNo" label="预约编号" width="180" />
                <el-table-column prop="labName" label="实验室" min-width="160" />
                <el-table-column prop="purpose" label="用途" min-width="180" show-overflow-tooltip />
                <el-table-column label="预约时间" width="280">
                  <template #default="{ row }">
                    <span class="date-text">{{ formatTime(row.startTime) }} ~ {{ formatTime(row.endTime, true) }}</span>
                  </template>
                </el-table-column>
                <el-table-column label="状态" width="120">
                  <template #default="{ row }">
                    <el-tag size="mini" :type="reservationStatusTag(row.status).type" effect="dark">
                      {{ reservationStatusTag(row.status).text }}
                    </el-tag>
                  </template>
                </el-table-column>
              </el-table>
            </el-card>
          </el-col>
        </el-row>
        <el-row :gutter="16" class="chart-row" style="margin-top:16px">
          <el-col :md="16" :sm="24">
            <el-card shadow="never" class="chart-card" body-style="padding:0">
              <div class="card-header">
                <div>
                  <h3 class="card-title-text">近 7 天预约次数（本学院）</h3>
                  <p class="card-subtitle">按日期统计本学院实验室预约数量变化趋势</p>
                </div>
              </div>
              <div ref="trendChart" class="chart"></div>
            </el-card>
          </el-col>
          <el-col :md="8" :sm="24">
            <el-card shadow="never" class="chart-card" body-style="padding:0">
              <div class="card-header">
                <div>
                  <h3 class="card-title-text">快捷操作</h3>
                  <p class="card-subtitle">常用功能入口</p>
                </div>
              </div>
              <div class="quick-actions">
                <el-button type="primary" icon="el-icon-edit" @click="goTo('/reserve/audit')">预约审核</el-button>
                <el-button type="success" icon="el-icon-date" @click="goTo('/reserve/mine')">我的预约</el-button>
                <el-button type="info" icon="el-icon-circle-check" @click="goTo('/reserve/check')">签到记录</el-button>
                <el-button type="warning" icon="el-icon-s-tools" @click="goTo('/lab/device')">设备台账</el-button>
                <el-button type="danger" icon="el-icon-document" @click="goTo('/lab/repair/mine')">我的报修记录</el-button>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </template>

      <template v-else>
        <el-row :gutter="16" class="chart-row">
          <el-col :md="16" :sm="24">
            <el-card shadow="never" class="chart-card" body-style="padding:0">
              <div class="card-header">
                <div>
                  <h3 class="card-title-text">实验室近 7 天预约次数</h3>
                  <p class="card-subtitle">按日期统计预约提交数量变化趋势</p>
                </div>
              </div>
              <div ref="trendChart" class="chart"></div>
            </el-card>
          </el-col>
          <el-col :md="8" :sm="24">
            <el-card shadow="never" class="chart-card" body-style="padding:0">
              <div class="card-header">
                <div>
                  <h3 class="card-title-text">设备状态占比</h3>
                  <p class="card-subtitle">在用 / 维修 / 报废 数量分布</p>
                </div>
              </div>
              <div ref="devicePieChart" class="chart"></div>
            </el-card>
          </el-col>
        </el-row>

        <el-card shadow="never" body-style="padding:0" class="pending-card">
          <div class="card-header">
            <div>
              <h3 class="card-title-text">待处理事项</h3>
              <p class="card-subtitle">低库存耗材 · 待审核预约 · 未处理报修</p>
            </div>
          </div>
          <el-tabs v-model="pendingTab" type="border-card">
            <el-tab-pane label="低库存耗材" name="stock">
              <el-table :data="lowStockList" size="medium" empty-text="暂无低库存记录" style="width:100%">
                <el-table-column prop="code" label="耗材编号" width="140" />
                <el-table-column prop="name" label="耗材名称" min-width="160" />
                <el-table-column prop="labName" label="所属实验室" min-width="180" />
                <el-table-column label="当前库存" width="120">
                  <template #default="{ row }">
                    <span class="stock-qty warn">{{ row.qty }} {{ row.unit }}</span>
                  </template>
                </el-table-column>
                <el-table-column label="预警阈值" width="120">
                  <template #default="{ row }">
                    <span class="stock-qty">{{ row.warnQty }} {{ row.unit }}</span>
                  </template>
                </el-table-column>
                <el-table-column label="状态" width="120">
                  <template #default>
                    <el-tag size="mini" type="warning" effect="dark">库存不足</el-tag>
                  </template>
                </el-table-column>
              </el-table>
            </el-tab-pane>
            <el-tab-pane label="待审核预约" name="reservation">
              <el-table :data="pendingReservations" size="medium" empty-text="暂无待审核预约" style="width:100%">
                <el-table-column prop="reserveNo" label="预约编号" width="180" />
                <el-table-column prop="applicant" label="申请人" width="120" />
                <el-table-column prop="labName" label="实验室" min-width="180" />
                <el-table-column prop="purpose" label="用途" min-width="200" show-overflow-tooltip />
                <el-table-column label="预约时段" width="280">
                  <template #default="{ row }">
                    <span class="date-text">{{ formatTime(row.startTime) }} ~ {{ formatTime(row.endTime, true) }}</span>
                  </template>
                </el-table-column>
                <el-table-column label="状态" width="120">
                  <template #default>
                    <el-tag size="mini" type="warning" effect="dark">待审核</el-tag>
                  </template>
                </el-table-column>
              </el-table>
            </el-tab-pane>
            <el-tab-pane label="未处理报修" name="repair">
              <el-table :data="pendingRepairs" size="medium" empty-text="暂无待处理报修" style="width:100%">
                <el-table-column prop="deviceName" label="设备名称" min-width="180" />
                <el-table-column prop="reporterName" label="报修人" width="120" />
                <el-table-column prop="faultDesc" label="故障描述" min-width="260" show-overflow-tooltip />
                <el-table-column label="报修时间" width="180">
                  <template #default="{ row }">
                    <span class="date-text">{{ formatTime(row.reportTime) }}</span>
                  </template>
                </el-table-column>
                <el-table-column label="状态" width="120">
                  <template #default>
                    <el-tag size="mini" type="danger" effect="dark">待处理</el-tag>
                  </template>
                </el-table-column>
              </el-table>
            </el-tab-pane>
          </el-tabs>
        </el-card>
      </template>
    </div>
  </page-shell>
</template>

<script>
import PageShell from '@/components/PageShell.vue'
import { statOverview } from '@/api/stat'
import { loadECharts } from '@/utils/echarts'
import * as wsModule from '@/utils/websocket'
import { currentIsStudent, currentIsTeacher, currentCanManageLab, currentIsAdmin } from '@/utils/permission'

const STUDENT_METRICS = [
  { key: 'reservation', label: '我的预约', icon: 'el-icon-date', bg: 'linear-gradient(135deg,#E8F3FF,#D0E6FF)', color: '#2d5a87', trend: undefined },
  { key: 'checkedIn',   label: '已签到',   icon: 'el-icon-circle-check', bg: 'linear-gradient(135deg,#E6F7FF,#C5E6FF)', color: '#1890FF', trend: undefined },
  { key: 'repair',      label: '我的报修', icon: 'el-icon-s-tools', bg: 'linear-gradient(135deg,#FFF7E6,#FFE7BA)', color: '#FA8C16', trend: undefined },
  { key: 'lab',         label: '可用实验室', icon: 'el-icon-office-building', bg: 'linear-gradient(135deg,#F6FFED,#D9F7BE)', color: '#52C41A', trend: undefined }
]

const TEACHER_METRICS = [
  { key: 'pending',     label: '待审核预约', icon: 'el-icon-finished', bg: 'linear-gradient(135deg,#FFF7E6,#FFE7BA)', color: '#FA8C16', trend: undefined },
  { key: 'reservation', label: '我的预约',   icon: 'el-icon-date', bg: 'linear-gradient(135deg,#E8F3FF,#D0E6FF)', color: '#2d5a87', trend: undefined },
  { key: 'checkedIn',   label: '已签到',     icon: 'el-icon-circle-check', bg: 'linear-gradient(135deg,#E6F7FF,#C5E6FF)', color: '#1890FF', trend: undefined },
  { key: 'repair',      label: '我的报修',   icon: 'el-icon-s-tools', bg: 'linear-gradient(135deg,#FFF1F0,#FFCCC7)', color: '#F5222D', trend: undefined }
]

const ADMIN_METRICS = [
  { key: 'lab',     label: '实验室总数', icon: 'el-icon-office-building', bg: 'linear-gradient(135deg,#E8F3FF,#D0E6FF)', color: '#2d5a87', trend: 12 },
  { key: 'device',  label: '设备总数',   icon: 'el-icon-monitor', bg: 'linear-gradient(135deg,#E6F7FF,#C5E6FF)', color: '#1890FF', trend: 8 },
  { key: 'stock',   label: '库存预警',   icon: 'el-icon-warning', bg: 'linear-gradient(135deg,#FFF1F0,#FFCCC7)', color: '#F5222D', trend: -3 },
  { key: 'booking', label: '待处理预约', icon: 'el-icon-date', bg: 'linear-gradient(135deg,#FFF7E6,#FFE7BA)', color: '#FA8C16', trend: 18 }
]

export default {
  name: 'Dashboard',
  components: { PageShell },
  data() {
    return {
      rawMetrics: [],
      pendingTab: 'stock',
      lowStockList: [],
      pendingReservations: [],
      pendingRepairs: [],
      myReservations: [],
      myRepairs: [],
      _echarts: null,
      _resizeBound: false
    }
  },
  computed: {
    isStudentDashboard() { return currentIsStudent() },
    isTeacherDashboard() { return currentIsTeacher() },
    isAdminDashboard() { return currentCanManageLab() || currentIsAdmin() },
    pageTitle() {
      if (this.isStudentDashboard) return '学生工作台'
      if (this.isTeacherDashboard) return '教师工作台'
      return '系统总览'
    },
    pageDesc() {
      if (this.isStudentDashboard) return '查看我的预约和报修记录'
      if (this.isTeacherDashboard) return '审核预约，管理实验室使用'
      return '实时数据统计'
    },
    metrics() {
      return this.rawMetrics
    }
  },
  mounted() {
    this.reload()
    try {
      const token = localStorage.getItem('lab_token')
      if (token) wsModule.ensureConnected(token)
    } catch (e) {}
    this._wsOff = wsModule.on('refresh-overview', () => this.reload())
  },
  activated() {
    this.reload()
  },
  beforeDestroy() {
    if (this._wsOff) this._wsOff()
    if (this.$refs.trendChart && this.$refs.trendChart.__chart) this.$refs.trendChart.__chart.dispose()
    if (this.$refs.devicePieChart && this.$refs.devicePieChart.__chart) this.$refs.devicePieChart.__chart.dispose()
    window.removeEventListener('resize', this.handleResize)
  },
  methods: {
    goTo(path) {
      this.$router.push(path).catch(() => {})
    },
    formatTime(t, timeOnly) {
      if (!t) return ''
      const d = new Date(t)
      const p = n => (n < 10 ? '0' + n : '' + n)
      if (timeOnly) return `${p(d.getHours())}:${p(d.getMinutes())}`
      return `${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
    },
    reservationStatusTag(s) {
      const map = {
        0: { type: 'warning', text: '待审核' },
        1: { type: 'success', text: '已通过' },
        2: { type: 'danger', text: '已驳回' },
        3: { type: 'info', text: '已完成' }
      }
      return map[s] || { type: 'info', text: '-' }
    },
    repairStatusTag(s) {
      const map = {
        0: { type: 'info', text: '待指派' },
        1: { type: 'warning', text: '处理中' },
        2: { type: 'success', text: '已完成' },
        3: { type: 'danger', text: '已驳回' }
      }
      return map[s] || { type: 'info', text: '-' }
    },
    async reload() {
      try {
        const res = await statOverview()
        const data = (res && res.data) || {}

        // 指标卡：按角色从 API 取数
        if (this.isStudentDashboard) {
          this.rawMetrics = STUDENT_METRICS.map(m => {
            const map = { reservation: data.myReservationCount, checkedIn: data.checkInCount, repair: data.myRepairCount, lab: data.labCount }
            return { ...m, value: Number(map[m.key]) || 0 }
          })
        } else if (this.isTeacherDashboard) {
          this.rawMetrics = TEACHER_METRICS.map(m => {
            const map = { pending: data.pendingReservations, reservation: data.myReservationCount, checkedIn: data.checkInCount, repair: data.myRepairCount }
            return { ...m, value: Number(map[m.key]) || 0 }
          })
        } else {
          this.rawMetrics = ADMIN_METRICS.map(m => {
            const map = { lab: data.labCount, device: data.deviceCount, stock: data.stockWarnings, booking: data.pendingReservations }
            return { ...m, value: Number(map[m.key]) || 0 }
          })
        }

        // 列表数据：统一从 API 取，无 fallback
        this.lowStockList = data.stockWarningList || []

        if (this.isStudentDashboard || this.isTeacherDashboard) {
          this.myReservations = data.myReservationList || []
          this.myRepairs = data.myRepairList || []
          const pending = (data.recentReservations || []).filter(r => r.status === 0)
          this.pendingReservations = pending
          const pendingRepairCount = Number(data.myPendingRepairs) || 0
          this.pendingRepairs = pendingRepairCount > 0
            ? [{ deviceName: `待处理设备 (${pendingRepairCount})`, reporterName: '待处理', faultDesc: `当前共有 ${pendingRepairCount} 条未处理报修记录`, reportTime: new Date().toISOString() }]
            : []
        } else {
          this.pendingReservations = (data.recentReservations || []).filter(r => r.status === 0)
          this.myReservations = data.recentReservations || []
          const pendingCount = Number(data.pendingRepairs) || 0
          this.pendingRepairs = pendingCount > 0
            ? [{ deviceName: `待处理设备 (${pendingCount})`, reporterName: '待处理', faultDesc: `当前共有 ${pendingCount} 条未处理报修记录`, reportTime: new Date().toISOString() }]
            : []
          this.myRepairs = data.recentRepairs || []
        }

        // 图表
        await this.$nextTick()
        if (this.isAdminDashboard) {
          await this.initCharts()
          if (data.dailyReservations) this.renderTrend(data.dailyReservations)
          if (data.deviceStatus) this.renderDevicePie(data.deviceStatus)
        } else if (this.isTeacherDashboard) {
          await this.initCharts()
          if (data.dailyReservations) this.renderTrend(data.dailyReservations)
        }
      } catch (e) {
        // API 失败时，给个空状态让用户知道，而不是显示假数据
        this.rawMetrics = []
        this.lowStockList = []
        this.pendingReservations = []
        this.pendingRepairs = []
        this.myReservations = []
        this.myRepairs = []
      }
    },
    async initCharts() {
      this._echarts = this._echarts || await loadECharts()
      if (this.$refs.trendChart && !this.$refs.trendChart.__chart) {
        this.$refs.trendChart.__chart = this._echarts.init(this.$refs.trendChart)
      }
      if (this.$refs.devicePieChart && !this.$refs.devicePieChart.__chart) {
        this.$refs.devicePieChart.__chart = this._echarts.init(this.$refs.devicePieChart)
      }
      if (!this._resizeBound) {
        this._resizeBound = true
        window.addEventListener('resize', this.handleResize)
      }
    },
    handleResize() {
      [this.$refs.trendChart, this.$refs.devicePieChart].forEach(ref => {
        if (ref && ref.__chart) ref.__chart.resize()
      })
    },
    renderTrend(list) {
      const chart = this.$refs.trendChart && this.$refs.trendChart.__chart
      if (!chart) return
      const dates = list.map(x => x.day || x.date || '')
      const values = list.map(x => Number(x.count || x.cnt || 0))
      chart.setOption({
        tooltip: { trigger: 'axis' },
        grid: { left: 50, right: 30, top: 30, bottom: 40 },
        xAxis: {
          type: 'category',
          data: dates,
          axisLine: { lineStyle: { color: '#D8DEE9' } },
          axisLabel: { color: '#606266' }
        },
        yAxis: {
          type: 'value',
          minInterval: 1,
          splitLine: { lineStyle: { type: 'dashed', color: '#ECEFF4' } },
          axisLabel: { color: '#606266' }
        },
        series: [{
          type: 'line',
          smooth: true,
          data: values,
          symbolSize: 8,
          itemStyle: { color: '#409EFF' },
          lineStyle: { width: 3, color: '#409EFF' },
          areaStyle: {
            color: new this._echarts.graphic.LinearGradient(0, 0, 0, 1,
              [{ offset: 0, color: 'rgba(64,158,255,0.35)' }, { offset: 1, color: 'rgba(64,158,255,0.02)' }])
          },
          label: { show: true, color: '#303133', position: 'top' }
        }]
      })
      setTimeout(() => chart.resize(), 50)
    },
    renderDevicePie(list) {
      const chart = this.$refs.devicePieChart && this.$refs.devicePieChart.__chart
      if (!chart) return
      const palette = { 1: '#52C41A', 2: '#FAAD14', 3: '#F5222D' }
      const data = list.map(x => ({
        name: x.name || (x.status === 1 ? '在用' : x.status === 2 ? '维修' : '报废'),
        value: Number(x.count || 0),
        itemStyle: { color: palette[x.status] || '#409EFF' }
      }))
      const total = data.reduce((s, x) => s + x.value, 0)
      chart.setOption({
        tooltip: { trigger: 'item', formatter: '{b}: {c} 台 ({d}%)' },
        legend: { bottom: 6, icon: 'circle', itemWidth: 10 },
        graphic: [{
          type: 'group',
          left: 'center',
          top: '40%',
          children: [
            { type: 'text', style: { text: String(total), fill: '#303133', fontSize: 28, fontWeight: 'bold', textAlign: 'center' } },
            { type: 'text', style: { text: '台', fill: '#909399', fontSize: 12, textAlign: 'center', y: 26 } }
          ]
        }],
        series: [{
          type: 'pie',
          radius: ['52%', '75%'],
          center: ['50%', '46%'],
          avoidLabelOverlap: true,
          itemStyle: { borderColor: '#fff', borderWidth: 3, borderRadius: 6 },
          label: { show: true, formatter: '{b}\n{d}%', color: '#303133' },
          emphasis: { label: { fontWeight: 'bold' } },
          data
        }]
      })
      setTimeout(() => chart.resize(), 50)
    }
  }
}
</script>

<style scoped lang="scss">
.dashboard-page {
  padding: 0;
}

.metric-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

@media (max-width: 1200px) {
  .metric-row { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 600px) {
  .metric-row { grid-template-columns: 1fr; }
}

.metric-card {
  border-radius: 12px;
  border: 1px solid #EBEEF5;
  transition: transform .25s ease, box-shadow .25s ease;
}
.metric-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 10px 24px rgba(30, 58, 95, 0.08);
}

.metric-icon {
  width: 52px;
  height: 52px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  float: right;
  font-size: 26px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.05);
}

.metric-info {
  overflow: hidden;
}
.metric-label {
  color: #909399;
  font-size: 13px;
  margin-bottom: 6px;
}
.metric-value {
  color: #303133;
  font-size: 30px;
  font-weight: 700;
  line-height: 1.1;
  margin-bottom: 8px;
  font-family: 'PingFang SC', 'Microsoft YaHei', monospace;
}
.metric-foot {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #909399;
}
.metric-trend {
  display: inline-flex;
  align-items: center;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 12px;
  font-weight: 600;
  gap: 2px;
}
.metric-trend.up {
  color: #52C41A;
  background: #F6FFED;
}
.metric-trend.down {
  color: #F5222D;
  background: #FFF1F0;
}
.metric-trend-label {
  color: #909399;
}

.chart-row {
  margin-bottom: 20px;
}
.chart-card, .pending-card {
  border-radius: 12px;
  border: 1px solid #EBEEF5;
  overflow: hidden;
}

.card-header {
  padding: 18px 20px 14px;
  border-bottom: 1px solid #F2F6FC;
  margin-bottom: 4px;
}
.card-title-text {
  margin: 0;
  font-size: 16px;
  color: #303133;
  font-weight: 600;
}
.card-subtitle {
  margin: 4px 0 0;
  font-size: 12px;
  color: #909399;
}

.chart {
  height: 340px;
  width: 100%;
}

.pending-card ::v-deep .el-tabs__header {
  margin: 0;
  padding: 0 20px;
}
.pending-card ::v-deep .el-tabs__item {
  height: 48px;
  line-height: 48px;
}
.pending-card ::v-deep .el-table {
  border-radius: 0;
}

.stock-qty {
  font-family: monospace;
  color: #606266;
}
.stock-qty.warn {
  color: #F5222D;
  font-weight: 600;
}

.date-text {
  color: #606266;
  font-size: 12px;
  font-family: monospace;
}

.quick-actions {
  padding: 16px 20px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.quick-actions .el-button {
  width: 100%;
  justify-content: flex-start;
}
</style>
