<template>
  <page-shell title="维修工单" desc="处理设备故障报修，指派维修人员并跟踪维修进度。">
    <template #actions>
      <el-button icon="el-icon-download" size="small" @click="onExport" :loading="exporting" type="success">导出</el-button>
    </template>

    <search-bar
      keyword-placeholder="按设备/资产号搜索"
      :options="[
        { prop: 'status', label: '状态', items: [
          { label: '待指派', value: 0 },
          { label: '处理中', value: 1 },
          { label: '已完成', value: 2 },
          { label: '已驳回', value: 3 }
        ] }
      ]"
      @search="onSearch"
      @reset="onReset"
    />

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
        <el-tag size="mini" :type="statusTag(row.status).type" effect="dark">{{ statusTag(row.status).text }}</el-tag>
      </template>
      <template slot="actions" slot-scope="{ row }">
        <template v-if="canHandle">
          <el-button size="mini" v-if="row.status < 2" @click="onHandle(row, 1)">受理</el-button>
          <el-button size="mini" type="success" v-if="row.status < 2" @click="onHandle(row, 2)">完成</el-button>
          <el-button size="mini" type="danger" v-if="row.status === 0" @click="onHandle(row, 3)">驳回</el-button>
        </template>
        <span v-else style="color:#909399">仅查看</span>
      </template>
    </data-table>
  </page-shell>
</template>

<script>
import { repairPage, repairHandle } from '@/api/lab'
import { currentCanManageLab } from '@/utils/permission'
import request from '@/utils/request'
import { downloadBlob } from '@/utils/download'
import PageShell from '@/components/PageShell.vue'
import SearchBar from '@/components/SearchBar.vue'
import DataTable from '@/components/DataTable.vue'

export default {
  name: 'LabRepair',
  components: { PageShell, SearchBar, DataTable },
  data() {
    return {
      query: { pageNum: 1, pageSize: 10, keyword: '', status: null },
      list: [], total: 0, loading: false, exporting: false,
      columns: [
        { prop: 'assetNo', label: '资产号', width: 130 },
        { prop: 'deviceName', label: '设备名称', width: 160 },
        { prop: 'reporterName', label: '报修人', width: 120 },
        { prop: 'faultDesc', label: '故障描述', minWidth: 200 },
        { prop: 'reportTime', label: '报修时间', width: 170 },
        { prop: 'handlerName', label: '处理人', width: 120 },
        { prop: 'status', label: '状态', width: 110, slot: 'status' },
        { prop: 'actions', label: '操作', width: 220, slot: 'actions' }
      ]
    }
  },
  computed: {
    canHandle() { return currentCanManageLab() }
  },
  mounted() { this.reload() },
  methods: {
    statusTag(s) {
      const map = {
        0: { type: 'info', text: '待指派' },
        1: { type: 'warning', text: '处理中' },
        2: { type: 'success', text: '已完成' },
        3: { type: 'danger', text: '已驳回' }
      }
      return map[s] || { type: 'info', text: '-' }
    },
    onSearch(q) { this.query = { ...this.query, pageNum: 1, ...q }; this.reload() },
    onReset() { this.query = { pageNum: 1, pageSize: 10, keyword: '', status: null }; this.reload() },
    onPage(n) { this.query.pageNum = n; this.reload() },
    onSize(n) { this.query.pageSize = n; this.query.pageNum = 1; this.reload() },
    async reload() {
      this.loading = true
      try { const r = await repairPage(this.query); this.list = r.data.records || []; this.total = r.data.total || 0 }
      finally { this.loading = false }
    },
    onHandle(row, status) {
      this.$prompt('处理说明（可空）', '处理工单', { inputValue: '' })
        .then(async ({ value }) => {
          try { await repairHandle(row.id, value || '', status); this.$message.success('已处理'); this.reload() }
          catch (e) {}
        }).catch(() => {})
    },
    async onExport() {
      if (this.exporting) return
      this.exporting = true
      try {
        const blob = await request.get('/lab/repair/export', { responseType: 'blob' })
        downloadBlob(blob, `维修工单_${new Date().toLocaleDateString()}.xlsx`)
        this.$message.success('导出成功')
      } catch (e) {}
      finally { this.exporting = false }
    }
  }
}
</script>
