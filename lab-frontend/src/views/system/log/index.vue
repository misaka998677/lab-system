<template>
  <page-shell title="操作日志" desc="平台用户操作行为记录，支持查询、筛选与数据导出">
    <template #actions>
      <el-button v-if="isAdmin" type="danger" icon="el-icon-delete" size="small" plain @click="onClear">清空日志</el-button>
      <el-button type="success" icon="el-icon-download" size="small" plain @click="doExport" :loading="exporting">导出 Excel</el-button>
    </template>

    <search-bar
      keyword-placeholder="按账号/操作搜索"
      :options="[
        { prop: 'status', label: '结果', items: [{label:'成功',value:1},{label:'失败',value:0}] },
        { prop: 'action', label: '操作类型', items: actionOptions }
      ]"
      @search="onSearch"
      @reset="onReset"
    >
      <el-date-picker
        v-model="dateRange"
        type="daterange"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        size="small"
        style="margin-right:8px;width:240px"
        @change="onDateChange"
        :clearable="true"
      />
    </search-bar>

    <data-table
      :columns="columns"
      :data="list"
      :total="total"
      :page-num="query.pageNum"
      :page-size="query.pageSize"
      :loading="loading"
      @page-change="onPage"
      @size-change="onSize"
    >
      <template slot="status" slot-scope="{ row }">
        <el-tag size="mini" :type="row.status === 1 ? 'success' : 'danger'" effect="dark">
          {{ row.status === 1 ? '成功' : '失败' }}
        </el-tag>
      </template>
      <template slot="costMs" slot-scope="{ row }">
        <span :class="costClass(row.costMs)">{{ row.costMs }} ms</span>
      </template>
      <template slot="method" slot-scope="{ row }">
        <code class="method-code">{{ row.method || '-' }}</code>
      </template>
      <template slot="ip" slot-scope="{ row }">
        <span class="ip-text">{{ row.ip || '-' }}</span>
      </template>
      <template slot="createTime" slot-scope="{ row }">
        <span class="time-text">{{ formatTs(row.createTime) }}</span>
      </template>
      <template slot="detail" slot-scope="{ row }">
        <el-tooltip :content="getDetail(row)" placement="top" :disabled="!getDetail(row)" :open-delay="300">
          <el-button size="mini" plain>详情</el-button>
        </el-tooltip>
      </template>
    </data-table>

    <!-- 详情弹层 -->
    <el-dialog title="日志详情" :visible.sync="detailVisible" width="680px">
      <el-descriptions :column="2" border size="small" v-if="currentRow">
        <el-descriptions-item label="账号">{{ currentRow.username }}</el-descriptions-item>
        <el-descriptions-item label="操作">{{ currentRow.action }}</el-descriptions-item>
        <el-descriptions-item label="方法" :span="2"><code>{{ currentRow.method }}</code></el-descriptions-item>
        <el-descriptions-item label="IP地址">{{ currentRow.ip }}</el-descriptions-item>
        <el-descriptions-item label="耗时">{{ currentRow.costMs }} ms</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag size="mini" :type="currentRow.status === 1 ? 'success' : 'danger'">
            {{ currentRow.status === 1 ? '成功' : '失败' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="时间">{{ formatTs(currentRow.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="请求参数" :span="2">
          <pre class="params-pre">{{ currentRow.params || '(无)' }}</pre>
        </el-descriptions-item>
        <el-descriptions-item v-if="currentRow.errorMsg" label="错误信息" :span="2">
          <span class="error-text">{{ currentRow.errorMsg }}</span>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </page-shell>
</template>

<script>
import { logPage, logClear } from '@/api/system'
import { currentIsAdmin } from '@/utils/permission'
import PageShell from '@/components/PageShell.vue'
import SearchBar from '@/components/SearchBar.vue'
import DataTable from '@/components/DataTable.vue'

const ACTION_MAP = {
  LOGIN: '登录', LOGOUT: '登出', CREATE: '新增', UPDATE: '编辑',
  DELETE: '删除', EXPORT: '导出', IMPORT: '导入', AUDIT: '审核',
  REPORT: '报修', CHECK_IN: '签到', CHECK_OUT: '签退'
}

export default {
  name: 'SysLog',
  components: { PageShell, SearchBar, DataTable },
  data() {
    return {
      query: { pageNum: 1, pageSize: 10, username: '', status: null, action: null },
      list: [],
      total: 0,
      loading: false,
      exporting: false,
      dateRange: null,
      detailVisible: false,
      currentRow: null,
      columns: [
        { prop: 'username',     label: '账号',     width: 120 },
        { prop: 'action',       label: '操作',      width: 90, formatter: r => ACTION_MAP[r.action] || r.action || '-' },
        { prop: 'method',       label: '请求路径',   minWidth: 240, slot: 'method' },
        { prop: 'ip',           label: 'IP',        width: 140, slot: 'ip' },
        { prop: 'costMs',       label: '耗时',      width: 100, align: 'right', slot: 'costMs' },
        { prop: 'status',       label: '结果',      width: 80,  slot: 'status' },
        { prop: 'createTime',   label: '时间',      width: 170, slot: 'createTime' },
        { prop: 'detail',       label: '详情',      width: 80,  slot: 'detail', align: 'center' }
      ],
      actionOptions: Object.entries(ACTION_MAP).map(([v, l]) => ({ label: l, value: v }))
    }
  },
  computed: {
    isAdmin() { return currentIsAdmin() }
  },
  mounted() { this.reload() },
  methods: {
    formatTs(v) {
      if (!v) return '-'
      const d = new Date(v)
      const p = n => (n < 10 ? '0' + n : '' + n)
      return `${d.getFullYear()}-${p(d.getMonth()+1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
    },
    costClass(ms) {
      if (ms > 1000) return 'cost-slow'
      if (ms > 300)  return 'cost-warn'
      return 'cost-fast'
    },
    getDetail(row) {
      const parts = []
      if (row.params) parts.push(`参数：${row.params}`)
      if (row.errorMsg) parts.push(`错误：${row.errorMsg}`)
      return parts.join('\n')
    },
    onSearch(q) {
      this.query = { ...this.query, pageNum: 1, ...q }
      this.reload()
    },
    onReset() {
      this.query = { pageNum: 1, pageSize: 10, username: '', status: null, action: null }
      this.dateRange = null
      this.reload()
    },
    onDateChange(val) {
      if (val && val.length === 2) {
        this.query.startDate = val[0].toISOString().slice(0, 10)
        this.query.endDate   = val[1].toISOString().slice(0, 10)
      } else {
        this.query.startDate = null
        this.query.endDate   = null
      }
      this.reload()
    },
    onPage(n)  { this.query.pageNum = n;   this.reload() },
    onSize(n)  { this.query.pageNum = 1;   this.query.pageSize = n; this.reload() },
    async reload() {
      this.loading = true
      try {
        const r = await logPage(this.query)
        this.list = r.data.records || []
        this.total = r.data.total || 0
      } catch (e) {
        this.list = []
      } finally {
        this.loading = false
      }
    },
    async doExport() {
      if (this.exporting) return
      this.exporting = true
      try {
        const resp = await this.$http.get('/system/log/export', { responseType: 'blob' })
        const blob = new Blob([resp], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
        const url = URL.createObjectURL(blob)
        const a = document.createElement('a')
        a.href = url
        a.download = `操作日志_${new Date().toISOString().slice(0, 10)}.xlsx`
        a.click()
        URL.revokeObjectURL(url)
        this.$message.success('导出成功')
      } catch (e) {
        this.$message.error('导出失败：' + (e.message || ''))
      } finally {
        this.exporting = false
      }
    },
    onClear() {
      this.$confirm('确认清空全部操作日志？此操作不可恢复。', '危险操作', { type: 'error' })
        .then(async () => {
          try {
            await logClear()
            this.$message.success('日志已清空')
            this.reload()
          } catch (e) {
            this.$message.error('清空失败')
          }
        }).catch(() => {})
    }
  }
}
</script>

<style scoped>
.cost-fast  { color: #52C41A; }
.cost-warn  { color: #FA8C16; }
.cost-slow  { color: #F5222D; font-weight: 600; }
.method-code {
  background: #F5F7FA;
  padding: 2px 6px;
  border-radius: 3px;
  font-size: 12px;
  color: #409EFF;
  max-width: 300px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  display: inline-block;
}
.ip-text { font-family: monospace; font-size: 12px; color: #606266; }
.time-text { font-size: 12px; color: #909399; }
.params-pre {
  margin: 0;
  background: #F5F7FA;
  padding: 8px;
  border-radius: 4px;
  font-size: 12px;
  color: #606266;
  max-height: 120px;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-all;
}
.error-text { color: #F5222D; font-size: 13px; }
</style>
