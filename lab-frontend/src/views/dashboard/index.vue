<template>
  <div class="app-container page-shell dashboard-page">
    <div class="hero-header">
      <div class="hero-left">
        <div class="hero-badge">{{ heroBadge }}</div>
        <h2 class="hero-title">欢迎回来，{{ user.realName || user.username || '用户' }}</h2>
        <p class="hero-subtitle">{{ heroSubtitle }}</p>
        <div class="hero-role-row">
          <el-tag size="small" v-for="r in $store.state.user.roles" :key="r" effect="light" type="primary">{{ formatRole(r) }}</el-tag>
          <span class="hero-date">{{ todayText }}</span>
        </div>
      </div>
      <div class="hero-right">
        <el-button v-if="isAdminView || isLabAdminView" type="warning" icon="el-icon-download" size="small" :loading="exporting" @click="doExport">导出数据</el-button>
        <div class="hero-avatar">
          <span>{{ avatarInitial }}</span>
        </div>
      </div>
    </div>

    <div class="metric-row">
      <template v-if="isAdminView || isLabAdminView">
        <el-card class="metric-card gradient gradient-blue" shadow="never" body-style="padding:20px">
          <div class="metric-label">今日预约</div>
          <div class="metric-value">{{ overview.todayReservations || 0 }}</div>
          <div class="metric-foot">今日开始的实验预约</div>
        </el-card>
        <el-card class="metric-card gradient gradient-orange" shadow="never" body-style="padding:20px">
          <div class="metric-label">待审核</div>
          <div class="metric-value">{{ overview.pendingReservations || 0 }}</div>
          <div class="metric-foot">等待审批的预约单</div>
        </el-card>
        <el-card class="metric-card gradient gradient-green" shadow="never" body-style="padding:20px">
          <div class="metric-label">实验室</div>
          <div class="metric-value">{{ overview.labCount || 0 }}</div>
          <div class="metric-foot">在用实验室总数</div>
        </el-card>
        <el-card class="metric-card gradient gradient-purple" shadow="never" body-style="padding:20px">
          <div class="metric-label">设备总数</div>
          <div class="metric-value">{{ overview.deviceCount || 0 }}</div>
          <div class="metric-foot">登记在册的实验设备</div>
        </el-card>
        <el-card class="metric-card gradient gradient-pink" shadow="never" body-style="padding:20px" v-if="isLabAdminView">
          <div class="metric-label">维修中</div>
          <div class="metric-value">{{ overview.pendingRepairs || 0 }}</div>
          <div class="metric-foot">待指派或处理中的维修单</div>
        </el-card>
        <el-card class="metric-card gradient gradient-red" shadow="never" body-style="padding:20px" v-if="isLabAdminView">
          <div class="metric-label">库存预警</div>
          <div class="metric-value warning-value">{{ overview.stockWarnings || 0 }}</div>
          <div class="metric-foot">低于预警阈值的耗材</div>
        </el-card>
        <el-card class="metric-card gradient gradient-pink" shadow="never" body-style="padding:20px" v-if="isAdminView">
          <div class="metric-label">用户总数</div>
          <div class="metric-value">{{ overview.userCount || 0 }}</div>
          <div class="metric-foot">系统注册用户</div>
        </el-card>
        <el-card class="metric-card gradient gradient-red" shadow="never" body-style="padding:20px" v-if="isAdminView">
          <div class="metric-label">今日新增</div>
          <div class="metric-value warning-value">{{ overview.todayNewUsers || 0 }}</div>
          <div class="metric-foot">今日新注册的账号</div>
        </el-card>
        <el-card class="metric-card gradient gradient-yellow pending-teacher-card" shadow="never" body-style="padding:20px" v-if="isAdminView" @click="goPendingTeachers">
          <div class="metric-label">待审核教师</div>
          <div class="metric-value warning-value">{{ overview.pendingTeacherCount || 0 }}</div>
          <div class="metric-foot">点击前往用户管理审核</div>
        </el-card>
      </template>
      <template v-else>
        <el-card class="metric-card gradient gradient-blue" shadow="never" body-style="padding:20px">
          <div class="metric-label">我的预约</div>
          <div class="metric-value">{{ overview.todayReservations || 0 }}</div>
          <div class="metric-foot">你发起的预约总数</div>
        </el-card>
        <el-card class="metric-card gradient gradient-orange" shadow="never" body-style="padding:20px">
          <div class="metric-label">待审核</div>
          <div class="metric-value">{{ overview.pendingReservations || 0 }}</div>
          <div class="metric-foot">等待审批的预约</div>
        </el-card>
        <el-card class="metric-card gradient gradient-green" shadow="never" body-style="padding:20px">
          <div class="metric-label">已通过</div>
          <div class="metric-value">{{ overview.labCount || 0 }}</div>
          <div class="metric-foot">已批准的实验预约</div>
        </el-card>
        <el-card class="metric-card gradient gradient-purple" shadow="never" body-style="padding:20px">
          <div class="metric-label">已签到</div>
          <div class="metric-value">{{ overview.deviceCount || 0 }}</div>
          <div class="metric-foot">已完成签到的实验</div>
        </el-card>
      </template>
    </div>

    <el-row :gutter="16" class="chart-row" v-if="isAdminView || isLabAdminView">
      <el-col :md="14" :sm="24">
        <el-card shadow="never" class="chart-card" body-style="padding:0">
          <div class="card-header">
            <div>
              <h3 class="card-title-text">近 7 天预约趋势</h3>
              <p class="card-subtitle">按日期统计各实验室预约数量变化</p>
            </div>
          </div>
          <div ref="trendChart" class="chart"></div>
        </el-card>
      </el-col>
      <el-col :md="10" :sm="24">
        <el-card shadow="never" class="chart-card" body-style="padding:0">
          <div class="card-header">
            <div>
              <h3 class="card-title-text">设备状态分布</h3>
              <p class="card-subtitle">在用 / 维修 / 报废占比</p>
            </div>
          </div>
          <div ref="devicePieChart" class="chart"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="chart-row" v-if="isAdminView || isLabAdminView">
      <el-col :md="14" :sm="24">
        <el-card shadow="never" class="chart-card" body-style="padding:0">
          <div class="card-header">
            <div>
              <h3 class="card-title-text">实验室预约 TOP 5</h3>
              <p class="card-subtitle">近期预约次数最多的实验室</p>
            </div>
          </div>
          <div ref="labBarChart" class="chart"></div>
        </el-card>
      </el-col>
      <el-col :md="10" :sm="24" v-if="isLabAdminView">
        <el-card shadow="never" class="chart-card" body-style="padding:0">
          <div class="card-header">
            <div>
              <h3 class="card-title-text">维修单处理状态</h3>
              <p class="card-subtitle">待指派 / 处理中 / 已完成 / 已驳回</p>
            </div>
          </div>
          <div ref="repairChart" class="chart"></div>
        </el-card>
      </el-col>
      <el-col :md="10" :sm="24" v-else-if="isAdminView">
        <el-card shadow="never" class="chart-card" body-style="padding:0">
          <div class="card-header">
            <div>
              <h3 class="card-title-text">角色分布</h3>
              <p class="card-subtitle">系统注册用户角色占比</p>
            </div>
          </div>
          <div ref="rolePieChart" class="chart"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="chart-row">
      <el-col :md="14" :sm="24">
        <el-card shadow="never" body-style="padding:0">
          <div class="card-header">
            <div>
              <h3 class="card-title-text">{{ isTeacherView || isStudentView ? '我的预约' : '近期预约' }}</h3>
              <p class="card-subtitle">{{ isTeacherView || isStudentView ? '你最新的 5 条预约记录' : '最新 5 条预约记录' }}</p>
            </div>
            <div v-if="isTeacherView || isStudentView" class="card-action">
              <el-button size="small" type="primary" icon="el-icon-plus" @click="$router.push('/reserve/mine')">查看全部</el-button>
            </div>
          </div>
          <el-table :data="recentReservations" style="width:100%" empty-text="暂无预约记录" >
            <el-table-column prop="reserveNo" label="预约号" width="150" />
            <el-table-column prop="applicant" label="申请人" width="100" v-if="isAdminView || isLabAdminView" />
            <el-table-column prop="labName" label="实验室" min-width="140" />
            <el-table-column prop="purpose" label="用途" min-width="160" show-overflow-tooltip />
            <el-table-column label="时间" width="220">
              <template #default="{ row }">
                <span class="date-text">{{ formatTime(row.startTime) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag size="mini" :type="statusTag(row.status)" effect="dark">{{ statusLabel(row.status) }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :md="10" :sm="24" v-if="isLabAdminView || isAdminView">
        <el-card shadow="never" body-style="padding:0">
          <div class="card-header">
            <div>
              <h3 class="card-title-text">库存提醒</h3>
              <p class="card-subtitle">低库存耗材 TOP 5</p>
            </div>
          </div>
          <div v-if="stockWarningList.length === 0" class="empty-state">
            <i class="el-icon-circle-check empty-icon success"></i>
            <p>当前没有低库存耗材</p>
          </div>
          <ul v-else class="warning-list">
            <li v-for="row in stockWarningList" :key="row.id" class="warning-line">
              <div class="warning-line-main">
                <span class="warning-line-name">{{ row.name }}</span>
                <span class="warning-line-code">{{ row.code }}</span>
              </div>
              <div class="warning-line-meta">
                <span class="warning-line-lab">{{ row.labName || '未指定实验室' }}</span>
                <span class="qty-tag">{{ row.qty }} <span class="qty-sep">/</span> {{ row.warnQty }} {{ row.unit || '' }}</span>
              </div>
            </li>
          </ul>
        </el-card>
      </el-col>
      <el-col :md="10" :sm="24" v-else>
        <el-card shadow="never" body-style="padding:0">
          <div class="card-header">
            <div>
              <h3 class="card-title-text">快捷操作</h3>
              <p class="card-subtitle">{{ isTeacherView ? '快速发起预约或查看历史记录' : '浏览可用实验室并发起预约' }}</p>
            </div>
          </div>
          <div class="quick-action-wrap">
            <div class="quick-action-item" @click="$router.push('/reserve/mine')">
              <i class="el-icon-document"></i>
              <span>{{ isTeacherView ? '我的预约管理' : '我的预约' }}</span>
            </div>
            <div class="quick-action-item" @click="$router.push('/reserve/mine')">
              <i class="el-icon-date"></i>
              <span>{{ isTeacherView ? '发起新预约' : '浏览可预约实验室' }}</span>
            </div>
            <div class="quick-action-item" @click="$router.push('/reserve/mine')">
              <i class="el-icon-tickets"></i>
              <span>预约历史记录</span>
            </div>
            <div class="quick-action-item" @click="$router.push('/dashboard')">
              <i class="el-icon-user"></i>
              <span>个人中心</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { statOverview } from '@/api/stat'
import { ROLE, ROLE_TEXT, primaryRole } from '@/utils/role-map'
import { loadECharts } from '@/utils/echarts'
import { connectWebSocket, disconnectWebSocket, addMessageListener } from '@/utils/websocket'
import { getToken } from '@/utils/auth'
import request from '@/utils/request'

export default {
  name: 'Dashboard',
  data() {
    return {
      loading: false,
      overview: {},
      recentReservations: [],
      stockWarningList: [],
      charts: [],
      _echarts: null,
      _wsUnsubscribe: null,
      exporting: false
    }
  },
  computed: {
    user() { return this.$store.state.user.user || {} },
    _roles() { return this.$store.state.user.roles || [] },
    primaryRoleKey() { return primaryRole(this._roles) },
    isAdminView() { return this.primaryRoleKey === ROLE.ADMIN },
    isLabAdminView() { return this.primaryRoleKey === ROLE.LABADMIN },
    isTeacherView() { return this.primaryRoleKey === ROLE.TEACHER },
    isStudentView() { return this.primaryRoleKey === ROLE.STUDENT },
    heroBadge() {
      if (this.isAdminView) return '系统管理工作台'
      if (this.isLabAdminView) return '实验室运营工作台'
      if (this.isTeacherView) return '教师预约工作台'
      if (this.isStudentView) return '学生预约工作台'
      return '实验室系统工作台'
    },
    heroSubtitle() {
      if (this.isAdminView) return '汇总用户、角色与实验室全局状态，协助管理员掌握系统运行情况。'
      if (this.isLabAdminView) return '汇总实验室使用率、设备状态与耗材库存，协助实验室管理员掌握运行情况。'
      if (this.isTeacherView) return '显示你发起的预约状态、签到记录，方便快速管理教学实验安排。'
      if (this.isStudentView) return '查看你的预约记录、即将开始的实验，方便及时了解实验安排。'
      return '欢迎使用实验室管理系统。'
    },
    todayText() {
      const d = new Date()
      return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日`
    },
    avatarInitial() {
      const n = this.user.realName || this.user.username || 'U'
      return (n + '').charAt(0)
    }
  },
  mounted() {
    this.$nextTick(() => {
      this.reload()
      window.addEventListener('resize', this.handleResize)
      // WebSocket 连接
      const token = getToken()
      if (token) {
        connectWebSocket(token)
        this._wsUnsubscribe = addMessageListener(this.handleWebSocketMessage)
      }
    })
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.handleResize)
    this.charts.forEach(c => { try { c.dispose() } catch (e) {} })
    if (this._wsUnsubscribe) this._wsUnsubscribe()
    disconnectWebSocket()
  },
  methods: {
    goPendingTeachers() { this.$router.push({ path: '/system/user', query: { status: 0 } }) },
    formatRole(code) {
      const m = { ROLE_ADMIN: '系统管理员', ROLE_LABADMIN: '实验室管理员', ROLE_TEACHER: '教师', ROLE_STUDENT: '学生' }
      return m[code] || code
    },
    async doExport() {
      if (this.exporting) return
      this.exporting = true
      try {
        const resp = await request.get('/stat/overview/export', { responseType: 'blob' })
        const blob = new Blob([resp], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
        const url = URL.createObjectURL(blob)
        const a = document.createElement('a')
        a.href = url
        a.download = `总览数据_${new Date().toLocaleDateString()}.xlsx`
        a.click()
        URL.revokeObjectURL(url)
        this.$message.success('导出成功')
      } catch (e) {
        this.$message.error('导出失败')
      } finally {
        this.exporting = false
      }
    },
    statusLabel(s) {
      return ['待审核', '已通过', '已驳回', '已签到', '已签退', '已取消'][Number(s)] || '未知'
    },
    statusTag(s) {
      return ['info', 'success', 'danger', 'warning', 'primary', ''][Number(s)] || ''
    },
    formatTime(t) {
      if (!t) return ''
      const d = new Date(t)
      const p = n => (n < 10 ? '0' + n : '' + n)
      return `${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
    },
    handleWebSocketMessage(msg) {
      if (msg.type === 'overview') {
        const d = msg.data || {}
        this.overview = d
        this.recentReservations = d.recentReservations || []
        this.stockWarningList = d.stockWarningList || []
        this.$nextTick(async () => {
          await this.initCharts()
          if (this.trendChart) this.renderTrend(d.dailyReservations || [])
          if (this.devicePieChart) this.renderDevicePie(d.deviceStatus || [])
          if (this.labBarChart) this.renderLabBar(d.labRanking || [])
          if (this.repairChart) this.renderRepairChart(d.repairStatus || [])
          if (this.isAdminView && this.rolePieChart) this.renderRolePieChart(d.roleDistribution || [])
          this.handleResize()
        })
      }
    },
    async initCharts() {
      // bundle-dynamic-imports：延迟加载 echarts，避免 main bundle 膨胀
      const echarts = this._echarts || (this._echarts = await loadECharts())
      const charts = []
      const tryInit = (key, refName) => {
        const el = this.$refs[refName]
        if (!el) return null
        try {
          const c = echarts.init(el)
          this[key] = c
          charts.push(c)
          return c
        } catch (e) {
          return null
        }
      }
      if (this.isAdminView || this.isLabAdminView) {
        tryInit('trendChart', 'trendChart')
        tryInit('devicePieChart', 'devicePieChart')
        tryInit('labBarChart', 'labBarChart')
        if (this.isLabAdminView) tryInit('repairChart', 'repairChart')
      }
      if (this.isAdminView) tryInit('rolePieChart', 'rolePieChart')
      this.charts = charts
      return charts
    },
    handleResize() {
      this.charts.forEach(c => { try { c.resize() } catch (e) {} })
    },
    async reload() {
      this.loading = true
      try {
        // async-parallel：同时发起数据请求与图表库懒加载
        const [r] = await Promise.all([
          statOverview(),
          this.isAdminView || this.isLabAdminView ? loadECharts() : Promise.resolve()
        ])
        const d = r.data || {}
        this.overview = d
        this.recentReservations = d.recentReservations || []
        this.stockWarningList = d.stockWarningList || []
        this.$nextTick(async () => {
          await this.initCharts()
          if (this.trendChart) this.renderTrend(d.dailyReservations || [])
          if (this.devicePieChart) this.renderDevicePie(d.deviceStatus || [])
          if (this.labBarChart) this.renderLabBar(d.labRanking || [])
          if (this.repairChart) this.renderRepairChart(d.repairStatus || [])
          if (this.isAdminView && this.rolePieChart) this.renderRolePieChart(d.roleDistribution || [])
          this.handleResize()
        })
      } catch (e) {}
      finally {
        this.loading = false
      }
    },
    renderTrend(list) {
      const dates = list.map(x => (x.day || x.date || '').slice(5))
      const values = list.map(x => Number(x.count || x.cnt || 0))
      this.trendChart.setOption({
        tooltip: { trigger: 'axis' },
        grid: { left: 50, right: 30, top: 30, bottom: 40 },
        xAxis: { type: 'category', data: dates, axisLine: { lineStyle: { color: '#D8DEE9' } }, axisLabel: { color: '#606266' } },
        yAxis: { type: 'value', minInterval: 1, splitLine: { lineStyle: { type: 'dashed', color: '#ECEFF4' } }, axisLabel: { color: '#606266' } },
        series: [{
          type: 'line', smooth: true, data: values, symbolSize: 8,
          itemStyle: { color: '#409EFF' },
          lineStyle: { width: 3, color: '#409EFF' },
          areaStyle: {
            color: new this._echarts.graphic.LinearGradient(0, 0, 0, 1,
              [{ offset: 0, color: 'rgba(64,158,255,0.35)' }, { offset: 1, color: 'rgba(64,158,255,0.02)' }])
          },
          label: { show: true, color: '#303133', position: 'top' }
        }]
      })
    },
    renderDevicePie(list) {
      const palette = { 1: '#67C23A', 2: '#E6A23C', 3: '#F56C6C' }
      const nameMap = { 1: '在用', 2: '维修', 3: '报废' }
      const data = list.map(x => ({ name: nameMap[x.status] || ('状态' + x.status), value: Number(x.count || 0), itemStyle: { color: palette[x.status] || '#409EFF' } }))
      const total = data.reduce((s, x) => s + x.value, 0)
      if (data.length === 0 || total === 0) {
        this.devicePieChart.setOption({
          title: { text: '暂无设备数据', left: 'center', top: 'center', textStyle: { color: '#909399', fontSize: 14, fontWeight: 400 } }
        })
        return
      }
      this.devicePieChart.setOption({
        tooltip: { trigger: 'item', formatter: '{b}: {c} 台 ({d}%)' },
        legend: { bottom: 8, icon: 'circle', itemWidth: 10 },
        graphic: [{ type: 'group', left: 'center', top: '38%', children: [
          { type: 'text', style: { text: String(total), fill: '#303133', fontSize: 26, fontWeight: 'bold', textAlign: 'center' } },
          { type: 'text', style: { text: '台', fill: '#909399', fontSize: 12, textAlign: 'center', y: 24 } }
        ]}],
        series: [{
          type: 'pie', radius: ['52%', '75%'], center: ['50%', '45%'], avoidLabelOverlap: true,
          itemStyle: { borderColor: '#fff', borderWidth: 3, borderRadius: 6 },
          label: { show: true, formatter: '{b}\n{c}台 ({d}%)', color: '#303133' },
          emphasis: { label: { fontWeight: 'bold' } },
          data
        }]
      })
    },
    renderLabBar(list) {
      const top = (list || []).slice(0, 5)
      const names = top.map(x => x.labName || x.name || '未知').reverse()
      const counts = top.map(x => Number(x.reserveCount || x.count || 0)).reverse()
      if (names.length === 0) {
        this.labBarChart.setOption({
          title: { text: '暂无预约数据', left: 'center', top: 'center', textStyle: { color: '#909399', fontSize: 14, fontWeight: 400 } }
        })
        return
      }
      this.labBarChart.setOption({
        tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
        grid: { left: 120, right: 50, top: 30, bottom: 30 },
        xAxis: { type: 'value', splitLine: { lineStyle: { type: 'dashed', color: '#ECEFF4' } }, axisLabel: { color: '#606266' } },
        yAxis: { type: 'category', data: names, axisLine: { lineStyle: { color: '#D8DEE9' } }, axisLabel: { color: '#303133' } },
        series: [{
          type: 'bar', data: counts, barWidth: 22,
          itemStyle: {
            color: new this._echarts.graphic.LinearGradient(0, 0, 1, 0,
              [{ offset: 0, color: '#67B0FF' }, { offset: 1, color: '#3D7FFF' }]),
            borderRadius: [0, 8, 8, 0]
          },
          label: { show: true, position: 'right', color: '#303133', fontWeight: 'bold' }
        }]
      })
    },
    renderRepairChart(list) {
      const palette = { 0: '#F56C6C', 1: '#E6A23C', 2: '#67C23A', 3: '#909399' }
      const nameMap = { 0: '待指派', 1: '处理中', 2: '已完成', 3: '已驳回' }
      const data = list.map(x => ({ name: nameMap[x.status] || ('状态' + x.status), value: Number(x.count || 0), itemStyle: { color: palette[x.status] || '#409EFF' } }))
      if (!this.repairChart) return
      const total = data.reduce((s, x) => s + x.value, 0)
      if (data.length === 0 || total === 0) {
        this.repairChart.setOption({
          title: { text: '暂无维修数据', left: 'center', top: 'center', textStyle: { color: '#909399', fontSize: 14, fontWeight: 400 } }
        })
        return
      }
      this.repairChart.setOption({
        tooltip: { trigger: 'item', formatter: '{b}: {c} 单 ({d}%)' },
        legend: { bottom: 8, icon: 'circle', itemWidth: 10 },
        graphic: [{ type: 'group', left: 'center', top: '38%', children: [
          { type: 'text', style: { text: String(total), fill: '#303133', fontSize: 26, fontWeight: 'bold', textAlign: 'center' } },
          { type: 'text', style: { text: '单', fill: '#909399', fontSize: 12, textAlign: 'center', y: 24 } }
        ]}],
        series: [{
          type: 'pie', radius: ['52%', '75%'], center: ['50%', '45%'], avoidLabelOverlap: true,
          itemStyle: { borderColor: '#fff', borderWidth: 3, borderRadius: 6 },
          label: { show: true, formatter: '{b}\n{c}单', color: '#303133' },
          data
        }]
      })
    },
    renderRolePieChart(list) {
      if (!this.rolePieChart) return
      const palette = ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#909399', '#B37FEB', '#36CFC9', '#FA8C16']
      const data = (list || []).map((x, i) => ({
        name: x.roleName || x.roleCode || ('角色' + i),
        value: Number(x.userCount || 0),
        itemStyle: { color: palette[i % palette.length] }
      }))
      const total = data.reduce((s, x) => s + x.value, 0)
      if (data.length === 0 || total === 0) {
        this.rolePieChart.setOption({
          title: { text: '暂无角色数据', left: 'center', top: 'center', textStyle: { color: '#909399', fontSize: 14, fontWeight: 400 } }
        })
        return
      }
      this.rolePieChart.setOption({
        tooltip: { trigger: 'item', formatter: '{b}: {c} 人 ({d}%)' },
        legend: { bottom: 8, icon: 'circle', itemWidth: 10 },
        graphic: [{ type: 'group', left: 'center', top: '38%', children: [
          { type: 'text', style: { text: String(total), fill: '#303133', fontSize: 26, fontWeight: 'bold', textAlign: 'center' } },
          { type: 'text', style: { text: '人', fill: '#909399', fontSize: 12, textAlign: 'center', y: 24 } }
        ]}],
        series: [{
          type: 'pie', radius: ['52%', '75%'], center: ['50%', '45%'], avoidLabelOverlap: true,
          itemStyle: { borderColor: '#fff', borderWidth: 3, borderRadius: 6 },
          label: { show: true, formatter: '{b}\n{c}人', color: '#303133' },
          data
        }]
      })
    }
  }
}
</script>

<style scoped lang="scss">
.hero-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: linear-gradient(135deg, #2B65EC 0%, #409EFF 50%, #67B0FF 100%);
  color: #fff;
  border-radius: 12px;
  padding: 28px 32px;
  margin-bottom: 20px;
  box-shadow: 0 6px 20px rgba(64, 158, 255, 0.25);
  position: relative;
  overflow: hidden;
}
.hero-header::before {
  content: '';
  position: absolute;
  right: -80px;
  top: -80px;
  width: 240px;
  height: 240px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.08);
}
.hero-header::after {
  content: '';
  position: absolute;
  right: 120px;
  bottom: -100px;
  width: 180px;
  height: 180px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.05);
}
.hero-badge {
  display: inline-block;
  background: rgba(255, 255, 255, 0.2);
  color: #fff;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  letter-spacing: 1px;
  margin-bottom: 12px;
}
.hero-title {
  font-size: 24px;
  font-weight: 600;
  margin: 0 0 8px 0;
  color: #fff;
}
.hero-subtitle {
  font-size: 13px;
  opacity: 0.9;
  margin: 0 0 18px 0;
  max-width: 600px;
}
.hero-role-row {
  display: flex;
  align-items: center;
  gap: 10px;
}
.hero-role-row .el-tag {
  border: none;
  background: rgba(255, 255, 255, 0.2);
  color: #fff;
}
.hero-date {
  color: rgba(255, 255, 255, 0.85);
  font-size: 13px;
}
.hero-avatar {
  width: 86px;
  height: 86px;
  border-radius: 50%;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 36px;
  color: #409EFF;
  font-weight: bold;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  z-index: 1;
  text-transform: uppercase;
}

.metric-row {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}
@media (max-width: 1440px) {
  .metric-row { grid-template-columns: repeat(3, 1fr); }
}
@media (max-width: 900px) {
  .metric-row { grid-template-columns: repeat(2, 1fr); }
}
.metric-card {
  border-radius: 12px;
  border: none;
  overflow: hidden;
  transition: transform .25s ease, box-shadow .25s ease;
}
.metric-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 10px 24px rgba(0, 0, 0, 0.08);
}
.gradient {
  color: #fff;
  position: relative;
}
.gradient-blue { background: linear-gradient(135deg, #4D9EFF, #1D6FEC); }
.gradient-orange { background: linear-gradient(135deg, #FFC07F, #F08F3D); }
.gradient-green { background: linear-gradient(135deg, #8FE38F, #3EB063); }
.gradient-purple { background: linear-gradient(135deg, #B8A4F0, #7E5FFF); }
.gradient-pink { background: linear-gradient(135deg, #FFB1C8, #F06292); }
.gradient-red { background: linear-gradient(135deg, #FF8A8A, #E84545); }
.gradient-yellow { background: linear-gradient(135deg, #FFD86F, #E8A628); }
.pending-teacher-card { cursor: pointer; transition: transform 0.2s ease, box-shadow 0.2s ease; }
.pending-teacher-card:hover { transform: translateY(-2px); box-shadow: 0 4px 16px rgba(0,0,0,0.15) !important; }
.gradient .metric-label {
  color: rgba(255, 255, 255, 0.9);
  font-size: 13px;
  margin-bottom: 6px;
}
.gradient .metric-value {
  color: #fff;
  font-size: 32px;
  font-weight: 700;
  margin-bottom: 6px;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.15);
}
.gradient .metric-foot {
  color: rgba(255, 255, 255, 0.85);
  font-size: 12px;
}
.warning-value {
  color: #fff !important;
}

.chart-row { margin-bottom: 16px; }
.chart-card {
  border-radius: 12px;
  border: 1px solid #EBEEF5;
  overflow: hidden;
}
.card-header {
  padding: 18px 20px 0;
  border-bottom: 1px solid #F2F6FC;
  padding-bottom: 16px;
  margin-bottom: 6px;
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
  height: 320px;
  width: 100%;
}

.empty-state {
  padding: 60px 20px;
  text-align: center;
  color: #909399;
}
.empty-icon {
  font-size: 42px;
  display: block;
  margin-bottom: 12px;
}
.success { color: #67C23A; }

.warning-list {
  list-style: none;
  margin: 0;
  padding: 4px 16px 16px;
}
.warning-line {
  padding: 12px 10px;
  border-bottom: 1px dashed #EBEEF5;
  transition: background .2s;
}
.warning-line:last-child { border-bottom: none; }
.warning-line:hover { background: #FAFBFC; }
.warning-line-main {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}
.warning-line-name {
  font-weight: 600;
  color: #303133;
  font-size: 14px;
}
.warning-line-code {
  color: #909399;
  font-size: 12px;
  font-family: monospace;
}
.warning-line-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
}
.warning-line-lab {
  color: #909399;
}
.qty-tag {
  background: #FEF0F0;
  color: #F56C6C;
  padding: 3px 10px;
  border-radius: 14px;
  font-weight: 600;
  font-family: monospace;
}
.qty-sep { color: #F56C6C; opacity: 0.4; }

.date-text {
  color: #606266;
  font-size: 12px;
  font-family: monospace;
}
</style>
