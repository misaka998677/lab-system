<template>
  <div class="app-container">
    <el-form :inline="true" class="filter-bar">
      <el-form-item><el-input v-model="query.username" placeholder="账号" clearable @keyup.enter.native="reload" /></el-form-item>
      <el-form-item><el-select v-model="query.status" placeholder="状态" clearable style="width:120px">
        <el-option :value="1" label="成功" /><el-option :value="0" label="失败" />
      </el-select></el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" @click="reload">查询</el-button>
        <el-button type="success" icon="el-icon-download" @click="doExport" :loading="exporting">导出</el-button>
        <el-button type="danger" icon="el-icon-delete" @click="onClear">清空</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="list" v-loading="loading" border>
      <el-table-column prop="username" label="账号"  width="120" />
      <el-table-column prop="action"   label="动作"  width="80" />
      <el-table-column prop="method"   label="方法"  width="240" />
      <el-table-column prop="ip"       label="IP"     width="140" />
      <el-table-column prop="costMs"   label="耗时(ms)" width="100" />
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '成功' : '失败' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="errorMsg" label="错误" show-overflow-tooltip />
      <el-table-column prop="createTime" label="时间" width="160" />
    </el-table>

    <el-pagination style="margin-top:12px" background layout="total,prev,pager,next"
      :total="total" :current-page.sync="query.pageNum" :page-size="query.pageSize" @current-change="reload" />
  </div>
</template>

<script>
import { logPage, logClear } from '@/api/system'
import request from '@/utils/request'
export default {
  data() { return { query: { pageNum: 1, pageSize: 10, username: '', status: null }, list: [], total: 0, loading: false, exporting: false } },
  mounted() { this.reload() },
  methods: {
    async doExport() {
      if (this.exporting) return
      this.exporting = true
      try {
        const resp = await request.get('/system/log/export', { responseType: 'blob' })
        const blob = new Blob([resp], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
        const url = URL.createObjectURL(blob)
        const a = document.createElement('a')
        a.href = url
        a.download = `操作日志_${new Date().toLocaleDateString()}.xlsx`
        a.click()
        URL.revokeObjectURL(url)
        this.$message.success('导出成功')
      } catch (e) {
        this.$message.error('导出失败')
      } finally {
        this.exporting = false
      }
    },
    async reload() { this.loading = true; try { const r = await logPage(this.query); this.list = r.data.records; this.total = r.data.total } catch (e) {} finally { this.loading = false } },
    onClear() { this.$confirm('清空全部日志？', '提示', { type: 'warning' }).then(async () => { await logClear(); this.reload() }).catch(() => {}) }
  }
}
</script>
