<template>
  <page-shell title="耗材档案" desc="维护实验耗材基础信息。库存低于预警阈值时将重点提示。">
    <template #actions>
      <el-button v-if="canManage" type="primary" icon="el-icon-plus" size="small" @click="onAdd">新增耗材</el-button>
      <el-button v-if="canManage" icon="el-icon-upload2" size="small" @click="onImport" :loading="importing" type="primary" plain>批量导入</el-button>
      <el-button v-if="canManage" icon="el-icon-document" size="small" @click="onDownloadTemplate">下载模板</el-button>
      <el-button v-if="canManage" icon="el-icon-download" size="small" @click="onExport" :loading="exporting" type="success">导出</el-button>
    </template>

    <search-bar
      keyword-placeholder="按编号/名称搜索"
      :options="[
        { prop: 'labId', label: '实验室', filterable: true, items: roomOptions },
        { prop: 'warning', label: '库存状态', items: [ { label: '仅看低库存', value: 1 } ] }
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
      <template slot="stock" slot-scope="{ row }">
        <span :style="{ color: isWarning(row) ? '#F56C6C' : '#303133', fontWeight: isWarning(row) ? 600 : 400 }">{{ row.qty }}</span>
        <span style="color:#909399"> / {{ row.warnQty }}</span>
      </template>
      <template slot="status" slot-scope="{ row }">
        <el-tag size="mini" :type="isWarning(row) ? 'danger' : 'success'" effect="dark">{{ isWarning(row) ? '库存偏低' : '库存正常' }}</el-tag>
      </template>
      <template slot="actions" slot-scope="{ row }">
        <el-button v-if="canManage" size="mini" @click="onEdit(row)">编辑</el-button>
        <el-button v-if="canManage" size="mini" type="danger" plain @click="onDelete(row)">删除</el-button>
      </template>
    </data-table>

    <el-dialog :title="form.id ? '编辑耗材' : '新增耗材'" :visible.sync="dialog" width="620px" @closed="resetForm">
      <el-form :model="form" label-width="100px">
        <el-form-item label="耗材编号"><el-input v-model="form.code" /></el-form-item>
        <el-form-item label="耗材名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="分类"><el-input v-model="form.category" /></el-form-item>
        <el-form-item label="单位"><el-input v-model="form.unit" /></el-form-item>
        <el-form-item label="当前库存"><el-input-number v-model="form.qty" :min="0" /></el-form-item>
        <el-form-item label="预警阈值"><el-input-number v-model="form.warnQty" :min="0" /></el-form-item>
        <el-form-item label="存放实验室">
          <el-select v-model="form.labId" filterable placeholder="请选择" style="width:100%">
            <el-option v-for="r in rooms" :key="r.id" :label="r.name" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注"><el-input type="textarea" v-model="form.remark" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="onSave" :loading="formLoading">保存</el-button>
      </template>
    </el-dialog>
  </page-shell>
</template>

<script>
import { stockItemPage, stockItemCreate, stockItemUpdate, stockItemDelete, stockItemTemplate, stockItemImport, stockItemExport } from '@/api/stock'
import { roomAll } from '@/api/lab'
import { currentCanManageLab } from '@/utils/permission'
import { downloadBlob, triggerFileInput } from '@/utils/download'
import PageShell from '@/components/PageShell.vue'
import SearchBar from '@/components/SearchBar.vue'
import DataTable from '@/components/DataTable.vue'

export default {
  name: 'StockItem',
  components: { PageShell, SearchBar, DataTable },
  data() {
    return {
      query: { pageNum: 1, pageSize: 10, keyword: '', labId: null, warning: null },
      list: [], total: 0, loading: false, rooms: [], exporting: false, importing: false,
      dialog: false, formLoading: false,
      form: { id: null, code: '', name: '', category: '', unit: '', qty: 0, warnQty: 0, labId: null, remark: '' },
      columns: [
        { prop: 'code', label: '耗材编号', width: 130 },
        { prop: 'name', label: '耗材名称', minWidth: 160 },
        { prop: 'category', label: '分类', width: 110 },
        { prop: 'unit', label: '单位', width: 80 },
        { prop: 'labName', label: '存放实验室', width: 160 },
        { prop: 'stock', label: '库存/预警', width: 140, slot: 'stock' },
        { prop: 'status', label: '状态', width: 110, slot: 'status' },
        { prop: 'remark', label: '备注', minWidth: 160 },
        { prop: 'actions', label: '操作', width: 170, slot: 'actions' }
      ]
    }
  },
  computed: {
    canManage() { return currentCanManageLab() },
    roomOptions() { return (this.rooms || []).map(r => ({ label: r.name, value: r.id })) }
  },
  async mounted() {
    try { const r = await roomAll(); this.rooms = r.data || [] } catch (e) {}
    this.reload()
  },
  methods: {
    isWarning(row) { return Number(row.qty || 0) <= Number(row.warnQty || 0) },
    onSearch(q) { this.query = { ...this.query, pageNum: 1, ...q }; this.reload() },
    onReset() { this.query = { pageNum: 1, pageSize: 10, keyword: '', labId: null, warning: null }; this.reload() },
    onPage(n) { this.query.pageNum = n; this.reload() },
    onSize(n) { this.query.pageSize = n; this.query.pageNum = 1; this.reload() },
    async reload() {
      this.loading = true
      try {
        const params = { ...this.query }
        if (params.warning) params.warningOnly = 1
        delete params.warning
        const r = await stockItemPage(params)
        this.list = r.data.records || []
        this.total = r.data.total || 0
      } finally { this.loading = false }
    },
    resetForm() { this.form = { id: null, code: '', name: '', category: '', unit: '', qty: 0, warnQty: 0, labId: null, remark: '' } },
    onAdd() { this.resetForm(); this.dialog = true },
    onEdit(row) { this.form = { ...row }; this.dialog = true },
    async onSave() {
      this.formLoading = true
      try {
        if (this.form.id) await stockItemUpdate(this.form)
        else await stockItemCreate(this.form)
        this.$message.success('保存成功')
        this.dialog = false
        this.reload()
      } finally { this.formLoading = false }
    },
    onDelete(row) {
      this.$confirm(`确定删除「${row.name}」？`, '删除确认', { type: 'warning' })
        .then(async () => { try { await stockItemDelete(row.id); this.$message.success('已删除'); this.reload() } catch (e) {} })
        .catch(() => {})
    },
    async onExport() {
      if (this.exporting) return
      this.exporting = true
      try {
        const blob = await stockItemExport()
        downloadBlob(blob, `耗材档案_${new Date().toLocaleDateString()}.xlsx`)
        this.$message.success('导出成功')
      } catch (e) {}
      finally { this.exporting = false }
    },
    async onDownloadTemplate() {
      try { const blob = await stockItemTemplate(); downloadBlob(blob, '耗材档案导入模板.xlsx') }
      catch (e) {}
    },
    onImport() {
      triggerFileInput('.xlsx,.xls', async file => {
        if (!file) return
        this.importing = true
        try {
          const r = await stockItemImport(file)
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
