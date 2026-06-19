<template>
  <page-shell title="我的报修记录" desc="查看我提交的设备报修记录及处理状态。">
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
    </data-table>
  </page-shell>
</template>

<script>
import { repairMine } from '@/api/lab'
import PageShell from '@/components/PageShell.vue'
import SearchBar from '@/components/SearchBar.vue'
import DataTable from '@/components/DataTable.vue'

export default {
  name: 'LabRepairMine',
  components: { PageShell, SearchBar, DataTable },
  data() {
    return {
      query: { pageNum: 1, pageSize: 10, keyword: '', status: null },
      list: [], total: 0, loading: false,
      columns: [
        { prop: 'assetNo', label: '资产号', width: 130 },
        { prop: 'deviceName', label: '设备名称', width: 160 },
        { prop: 'labName', label: '所在实验室', width: 160 },
        { prop: 'faultDesc', label: '故障描述', minWidth: 200 },
        { prop: 'reportTime', label: '报修时间', width: 170 },
        { prop: 'handlerName', label: '处理人', width: 120 },
        { prop: 'status', label: '状态', width: 110, slot: 'status' }
      ]
    }
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
      try { const r = await repairMine(this.query); this.list = r.data.records || []; this.total = r.data.total || 0 }
      finally { this.loading = false }
    }
  }
}
</script>
