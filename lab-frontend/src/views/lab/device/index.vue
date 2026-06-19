<template>
  <page-shell title="设备台账" desc="管理实验设备基础信息，记录资产编号、品牌型号和运行状态。">
    <template #actions>
      <el-button v-if="canManage" type="primary" icon="el-icon-plus" size="small" @click="onAdd">新增设备</el-button>
      <el-button v-if="canManage" icon="el-icon-upload2" size="small" @click="onImport" :loading="importing" type="primary" plain>批量导入</el-button>
      <el-button v-if="canManage" icon="el-icon-document" size="small" @click="onDownloadTemplate">下载模板</el-button>
      <el-button v-if="canManage" icon="el-icon-download" size="small" @click="onExport" :loading="exporting" type="success">导出</el-button>
    </template>

    <search-bar
      keyword-placeholder="按资产号/名称搜索"
      :options="[
        { prop: 'labId', label: '实验室', filterable: true, items: roomOptions },
        { prop: 'status', label: '状态', items: [ { label: '在用', value: 1 }, { label: '维修', value: 2 }, { label: '报废', value: 3 } ] }
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
        <el-button v-if="canManage" size="mini" @click="onEdit(row)">编辑</el-button>
        <el-button v-if="canReport && row.status === 1" size="mini" type="warning" @click="onReport(row)">报修</el-button>
        <el-button v-if="canReport && row.status === 2" size="mini" type="info" disabled>维修中</el-button>
        <el-button v-if="canReport && row.status === 3" size="mini" type="danger" disabled>已报废</el-button>
        <el-button v-if="canManage" size="mini" type="danger" plain @click="onDelete(row)">删除</el-button>
      </template>
    </data-table>

    <el-dialog :title="form.id ? '编辑设备' : '新增设备'" :visible.sync="dialog" width="560px" @closed="resetForm">
      <el-form :model="form" label-width="100px">
        <el-form-item label="资产号"><el-input v-model="form.assetNo" /></el-form-item>
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="类型"><el-input v-model="form.category" /></el-form-item>
        <el-form-item label="品牌"><el-input v-model="form.brand" /></el-form-item>
        <el-form-item label="型号"><el-input v-model="form.model" /></el-form-item>
        <el-form-item label="实验室">
          <el-select v-model="form.labId" filterable placeholder="请选择" style="width:100%">
            <el-option v-for="r in rooms" :key="r.id" :label="r.name" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">在用</el-radio>
            <el-radio :label="2">维修</el-radio>
            <el-radio :label="3">报废</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="onSave" :loading="formLoading">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog title="设备报修" :visible.sync="reportDialog" width="500px" @closed="resetReportForm">
      <el-form label-width="80px">
        <el-form-item label="设备"><el-input :value="reportForm.deviceName" disabled /></el-form-item>
        <el-form-item label="故障描述"><el-input type="textarea" v-model="reportForm.faultDesc" :rows="4" placeholder="请描述故障现象" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reportDialog = false">取消</el-button>
        <el-button type="primary" @click="onReportSubmit" :loading="formLoading">提交</el-button>
      </template>
    </el-dialog>
  </page-shell>
</template>

<script>
import { devicePage, deviceCreate, deviceUpdate, deviceDelete, deviceTemplate, deviceImport, deviceExport, roomAll, repairReport } from '@/api/lab'
import { currentCanManageLab, currentCanReportRepair } from '@/utils/permission'
import { downloadBlob, triggerFileInput } from '@/utils/download'
import PageShell from '@/components/PageShell.vue'
import SearchBar from '@/components/SearchBar.vue'
import DataTable from '@/components/DataTable.vue'

export default {
  name: 'LabDevice',
  components: { PageShell, SearchBar, DataTable },
  data() {
    return {
      query: { pageNum: 1, pageSize: 10, keyword: '', labId: null, status: null },
      list: [], total: 0, loading: false, rooms: [], exporting: false, importing: false,
      dialog: false, reportDialog: false, formLoading: false,
      form: { id: null, assetNo: '', name: '', category: '', brand: '', model: '', labId: null, status: 1 },
      reportForm: { deviceId: null, deviceName: '', faultDesc: '' },
      columns: [
        { prop: 'assetNo', label: '资产号', width: 130 },
        { prop: 'name', label: '设备名称', minWidth: 160 },
        { prop: 'category', label: '类型', width: 110 },
        { prop: 'brand', label: '品牌', width: 100 },
        { prop: 'model', label: '型号', width: 120 },
        { prop: 'labName', label: '所在实验室', width: 160 },
        { prop: 'status', label: '状态', width: 100, slot: 'status' },
        { prop: 'actions', label: '操作', width: 240, slot: 'actions' }
      ]
    }
  },
  computed: {
    canManage() { return currentCanManageLab() },
    canReport() { return currentCanReportRepair() },
    roomOptions() { return (this.rooms || []).map(r => ({ label: r.name, value: r.id })) }
  },
  async mounted() {
    try { const r = await roomAll(); this.rooms = r.data || [] } catch (e) {}
    this.reload()
  },
  methods: {
    statusTag(s) {
      const map = { 1: { type: 'success', text: '在用' }, 2: { type: 'warning', text: '维修' }, 3: { type: 'info', text: '报废' } }
      return map[s] || { type: 'info', text: '-' }
    },
    onSearch(q) { this.query = { ...this.query, pageNum: 1, ...q }; this.reload() },
    onReset() { this.query = { pageNum: 1, pageSize: 10, keyword: '', labId: null, status: null }; this.reload() },
    onPage(n) { this.query.pageNum = n; this.reload() },
    onSize(n) { this.query.pageSize = n; this.query.pageNum = 1; this.reload() },
    async reload() {
      this.loading = true
      try { const r = await devicePage(this.query); this.list = r.data.records || []; this.total = r.data.total || 0 }
      finally { this.loading = false }
    },
    resetForm() { this.form = { id: null, assetNo: '', name: '', category: '', brand: '', model: '', labId: null, status: 1 } },
    resetReportForm() { this.reportForm = { deviceId: null, deviceName: '', faultDesc: '' } },
    onAdd() { this.resetForm(); this.dialog = true },
    onEdit(row) { this.form = { ...row }; this.dialog = true },
    async onSave() {
      this.formLoading = true
      try {
        if (this.form.id) await deviceUpdate(this.form)
        else await deviceCreate(this.form)
        this.$message.success('保存成功')
        this.dialog = false
        this.reload()
      } finally { this.formLoading = false }
    },
    onDelete(row) {
      this.$confirm(`删除「${row.name}」？`, '提示', { type: 'warning' })
        .then(async () => {
          try { await deviceDelete(row.id); this.$message.success('已删除'); this.reload() } catch (e) {}
        }).catch(() => {})
    },
    onReport(row) { this.reportForm = { deviceId: row.id, deviceName: row.name, faultDesc: '' }; this.reportDialog = true },
    async onReportSubmit() {
      if (!this.reportForm.faultDesc) return this.$message.warning('请填写故障描述')
      this.formLoading = true
      try {
        await repairReport({ deviceId: this.reportForm.deviceId, faultDesc: this.reportForm.faultDesc })
        this.reportDialog = false
        this.$message.success('已提交报修')
        this.reload()
      } finally { this.formLoading = false }
    },
    async onExport() {
      if (this.exporting) return
      this.exporting = true
      try {
        const blob = await deviceExport()
        downloadBlob(blob, `设备台账_${new Date().toLocaleDateString()}.xlsx`)
        this.$message.success('导出成功')
      } catch (e) {}
      finally { this.exporting = false }
    },
    async onDownloadTemplate() {
      try { const blob = await deviceTemplate(); downloadBlob(blob, '设备台账导入模板.xlsx') }
      catch (e) {}
    },
    onImport() {
      triggerFileInput('.xlsx,.xls', async file => {
        if (!file) return
        this.importing = true
        try {
          const r = await deviceImport(file)
          const failRows = (r.data && r.data.failRows) || []
          if (failRows.length > 0) {
            const lines = failRows.map(f => `第 ${f.rowNum} 行：${f.reason}`).join('\n')
            this.$message.warning(`导入完成（${r.data.successCount || 0} 行），部分数据不合法：\n` + lines)
          } else {
            this.$message.success('导入成功，共 ' + ((r.data && r.data.successCount) || 0) + ' 行')
          }
          this.reload()
        } catch (e) {}
        finally { this.importing = false }
      })
    }
  }
}
</script>
