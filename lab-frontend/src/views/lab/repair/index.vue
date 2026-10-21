<template>
  <div class="page-shell">
    <div class="page-header">
      <div>
        <h2 class="page-title">维修工单</h2>
        <p class="page-desc">处理设备故障报修，指派维修人员并跟踪维修进度。</p>
      </div>
      <el-button icon="el-icon-download" @click="doExport" :loading="exporting" type="success">导出</el-button>
    </div>

    <el-card shadow="never" class="table-card" body-style="padding:0">
      <div class="filter-bar">
        <el-input v-model="query.keyword" placeholder="设备/资产号" clearable style="width:220px" @keyup.enter.native="reload" />
        <el-select v-model="query.status" placeholder="状态" clearable style="width:140px; margin-left:12px">
          <el-option :value="0" label="待指派" />
          <el-option :value="1" label="处理中" />
          <el-option :value="2" label="已完成" />
          <el-option :value="3" label="已驳回" />
        </el-select>
        <el-button type="primary" icon="el-icon-search" style="margin-left:12px" @click="reload">查询</el-button>
      </div>

      <el-table :data="list" v-loading="loading" :border="false" style="width:100%" :header-cell-style="headerStyle" :row-style="rowStyle" empty-text="暂无维修工单">
        <el-table-column prop="assetNo" label="资产号" width="130" />
        <el-table-column prop="deviceName" label="设备名称" width="160" />
        <el-table-column prop="reporterName" label="报修人" width="120" />
        <el-table-column prop="faultDesc" label="故障描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="reportTime" label="报修时间" width="170" />
        <el-table-column prop="handlerName" label="处理人" width="120">
          <template #default="{ row }">
            <span v-if="row.handlerName">{{ row.handlerName }}</span>
            <span v-else style="color:#C0C4CC">—</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="['info', 'warning', 'success', 'danger'][row.status]" effect="dark">
              {{ ['待指派', '处理中', '已完成', '已驳回'][row.status] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" align="center">
          <template #default="{ row }">
            <template v-if="canHandle">
              <el-button size="mini" v-if="row.status < 2" @click="onHandle(row, 1)">受理</el-button>
              <el-button size="mini" type="success" v-if="row.status < 2" @click="onHandle(row, 2)">完成</el-button>
              <el-button size="mini" type="danger" v-if="row.status === 0" @click="onHandle(row, 3)">驳回</el-button>
            </template>
            <span v-else style="color:#909399">仅查看</span>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination background layout="total,prev,pager,next"
        :total="total" :current-page.sync="query.pageNum" :page-size="query.pageSize"
        @current-change="reload"
        style="padding: 16px 20px; border-top: 1px solid #F2F6FC;" />
    </el-card>
  </div>
</template>

<script>
import { repairPage, repairHandle } from '@/api/lab'
import request from '@/utils/request'

export default {
  name: 'LabRepair',
  data() {
    return {
      query: { pageNum: 1, pageSize: 10, keyword: '', status: null },
      list: [], total: 0, loading: false, exporting: false,
      headerStyle: { background: '#FAFBFC', color: '#606266', fontWeight: 500 }
    }
  },
  computed: {
    canHandle() { return (this.$store.state.user.roles || []).some(r => ['ROLE_ADMIN', 'ROLE_LABADMIN'].includes(r)) }
  },
  mounted() { this.reload() },
  methods: {
    rowStyle({ rowIndex }) { return rowIndex % 2 === 1 ? { background: '#FAFBFC' } : {} },
    async reload() {
      this.loading = true
      try { const r = await repairPage(this.query); this.list = r.data.records; this.total = r.data.total } catch (e) {} finally { this.loading = false }
    },
    onHandle(row, status) {
      const action = ['指派', '完成', '驳回'][status - 1] || '处理'
      this.$prompt(`处理说明（可空）`, action + '维修单', { inputValue: '' })
        .then(async ({ value }) => {
          try { await repairHandle(row.id, { status, note: value || '' }); this.$message.success('已处理'); this.reload() } catch (e) {}
        }).catch(() => {})
    },
    async doExport() {
      if (this.exporting) return
      this.exporting = true
      try {
        const resp = await request.get('/lab/repair/export', { responseType: 'blob' })
        const blob = new Blob([resp], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
        const url = URL.createObjectURL(blob)
        const a = document.createElement('a')
        a.href = url
        a.download = `维修工单_${new Date().toLocaleDateString()}.xlsx`
        a.click()
        URL.revokeObjectURL(url)
        this.$message.success('导出成功')
      } catch (e) {
        this.$message.error('导出失败')
      } finally {
        this.exporting = false
      }
    }
  }
}
</script>

<style scoped lang="scss">
</style>