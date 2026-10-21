<template>
  <div class="page-shell">
    <div class="page-header">
      <div>
        <h2 class="page-title">设备台账</h2>
        <p class="page-desc">管理实验设备基础信息，记录资产编号、品牌型号和运行状态。</p>
      </div>
      <el-button v-if="canManage" type="primary" icon="el-icon-plus" @click="onAdd">新增设备</el-button>
      <el-button icon="el-icon-download" @click="doExport" :loading="exporting" type="success">导出</el-button>
    </div>

    <el-card shadow="never" class="table-card" body-style="padding:0">
      <div class="filter-bar">
        <el-input v-model="query.keyword" placeholder="资产号/名称" clearable style="width:220px" @keyup.enter.native="reload" />
        <el-select v-model="query.labId" placeholder="实验室" clearable filterable style="width:180px; margin-left:12px">
          <el-option v-for="r in rooms" :key="r.id" :label="r.name" :value="r.id" />
        </el-select>
        <el-select v-model="query.status" placeholder="状态" clearable style="width:120px; margin-left:12px">
          <el-option :value="1" label="在用" />
          <el-option :value="2" label="维修" />
          <el-option :value="3" label="报废" />
        </el-select>
        <el-button type="primary" icon="el-icon-search" style="margin-left:12px" @click="reload">查询</el-button>
      </div>

      <el-table :data="list" v-loading="loading" :border="false" style="width:100%" :header-cell-style="headerStyle" :row-style="rowStyle" empty-text="暂无设备数据">
        <el-table-column prop="assetNo" label="资产号" width="130" />
        <el-table-column prop="name" label="设备名称" min-width="160" />
        <el-table-column prop="category" label="类型" width="110" />
        <el-table-column prop="brand" label="品牌" width="100" />
        <el-table-column prop="model" label="型号" width="120" />
        <el-table-column prop="labName" label="所在实验室" width="160" />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 1 ? 'success' : (row.status === 2 ? 'warning' : 'info')" effect="dark">
              {{ ['', '在用', '维修', '报废'][row.status] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" align="center">
          <template #default="{ row }">
            <el-button v-if="canManage" size="mini" @click="onEdit(row)">编辑</el-button>
            <el-button v-if="canReport" size="mini" type="warning" @click="onReport(row)">报修</el-button>
            <el-button v-if="canManage" size="mini" type="danger" plain @click="onDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination background layout="total,prev,pager,next"
        :total="total" :current-page.sync="query.pageNum" :page-size="query.pageSize"
        @current-change="reload"
        style="padding: 16px 20px; border-top: 1px solid #F2F6FC;" />
    </el-card>

    <el-dialog :title="form.id ? '编辑设备' : '新增设备'" :visible.sync="dialog" width="560px" @closed="resetForm">
      <el-form :model="form" label-width="100px">
        <el-form-item label="资产号"><el-input v-model="form.assetNo" /></el-form-item>
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="类型"><el-input v-model="form.category" /></el-form-item>
        <el-form-item label="品牌"><el-input v-model="form.brand" /></el-form-item>
        <el-form-item label="型号"><el-input v-model="form.model" /></el-form-item>
        <el-form-item label="实验室">
          <el-select v-model="form.labId" filterable style="width:100%">
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
        <el-button type="primary" @click="onSave">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog title="设备报修" :visible.sync="reportDialog" width="500px" @closed="resetReportForm">
      <el-form label-width="80px">
        <el-form-item label="设备"><el-input :value="reportForm.deviceName" disabled /></el-form-item>
        <el-form-item label="故障描述"><el-input type="textarea" v-model="reportForm.faultDesc" :rows="4" placeholder="请描述故障现象" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reportDialog = false">取消</el-button>
        <el-button type="primary" @click="onReportSubmit">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { devicePage, deviceCreate, deviceUpdate, deviceDelete, roomAll, repairReport } from '@/api/lab'
import request from '@/utils/request'

export default {
  name: 'LabDevice',
  data() {
    return {
      query: { pageNum: 1, pageSize: 10, keyword: '', labId: null, status: null },
      list: [], total: 0, loading: false, rooms: [], exporting: false,
      dialog: false,
      form: { id: null, assetNo: '', name: '', category: '', brand: '', model: '', labId: null, status: 1 },
      reportDialog: false,
      reportForm: { deviceId: null, deviceName: '', faultDesc: '' },
      headerStyle: { background: '#FAFBFC', color: '#606266', fontWeight: 500 }
    }
  },
  computed: {
    canManage() { return (this.$store.state.user.roles || []).some(r => ['ROLE_ADMIN', 'ROLE_LABADMIN'].includes(r)) },
    canReport() { return (this.$store.state.user.roles || []).some(r => ['ROLE_ADMIN', 'ROLE_LABADMIN', 'ROLE_TEACHER'].includes(r)) }
  },
  async mounted() {
    try { const r = await roomAll(); this.rooms = r.data || [] } catch (e) {}
    this.reload()
  },
  methods: {
    rowStyle({ rowIndex }) { return rowIndex % 2 === 1 ? { background: '#FAFBFC' } : {} },
    resetForm() { this.form = { id: null, assetNo: '', name: '', category: '', brand: '', model: '', labId: null, status: 1 } },
    resetReportForm() { this.reportForm = { deviceId: null, deviceName: '', faultDesc: '' } },
    async reload() {
      this.loading = true
      try { const r = await devicePage(this.query); this.list = r.data.records; this.total = r.data.total } catch (e) {} finally { this.loading = false }
    },
    onAdd() { this.resetForm(); this.dialog = true },
    onEdit(row) { this.form = { ...row }; this.dialog = true },
    async onSave() {
      try {
        if (this.form.id) await deviceUpdate(this.form)
        else await deviceCreate(this.form)
        this.dialog = false
        this.$message.success('保存成功')
        this.reload()
      } catch (e) {}
    },
    onDelete(row) {
      this.$confirm(`删除「${row.name}」？`, '提示', { type: 'warning' })
        .then(async () => { try { await deviceDelete(row.id); this.$message.success('已删除'); this.reload() } catch (e) {} })
        .catch(() => {})
    },
    onReport(row) { this.reportForm = { deviceId: row.id, deviceName: row.name, faultDesc: '' }; this.reportDialog = true },
    async onReportSubmit() {
      if (!this.reportForm.faultDesc) return this.$message.warning('请填写故障描述')
      try {
        await repairReport({ deviceId: this.reportForm.deviceId, faultDesc: this.reportForm.faultDesc })
        this.reportDialog = false
        this.$message.success('已提交报修')
        this.reload()
      } catch (e) {}
    },
    async doExport() {
      if (this.exporting) return
      this.exporting = true
      try {
        const resp = await request.get('/lab/device/export', { responseType: 'blob' })
        const blob = new Blob([resp], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
        const url = URL.createObjectURL(blob)
        const a = document.createElement('a')
        a.href = url
        a.download = `设备台账_${new Date().toLocaleDateString()}.xlsx`
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