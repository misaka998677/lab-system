<template>
  <page-shell title="实验室档案" desc="管理实验室基本信息：名称、楼栋、容量与运行状态">
    <template #actions>
      <el-button v-if="canManage" type="primary" icon="el-icon-plus" size="small" @click="onAdd">新增实验室</el-button>
    </template>

    <search-bar
      keyword-placeholder="按名称/编号搜索"
      :options="[
        { prop: 'status', label: '状态', items: [{label:'可用',value:1},{label:'停用',value:0},{label:'维护中',value:2}] }
      ]"
      @search="onSearch"
      @reset="onReset"
    ></search-bar>

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
        <el-tag size="mini" :type="statusTag(row.status).type">{{ statusTag(row.status).text }}</el-tag>
      </template>
      <template slot="actions" slot-scope="{ row }">
        <el-button v-if="canManage" size="mini" @click="onEdit(row)">编辑</el-button>
        <el-button v-if="canManage" size="mini" type="danger" plain @click="onDelete(row)">删除</el-button>
      </template>
    </data-table>

    <el-dialog :title="form.id ? '编辑实验室' : '新增实验室'" :visible.sync="dialog" width="560px" @closed="resetForm">
      <el-form :model="form" label-width="80px">
        <el-form-item label="编号"><el-input v-model="form.code" /></el-form-item>
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="楼栋"><el-input v-model="form.building" /></el-form-item>
        <el-form-item label="房间号"><el-input v-model="form.roomNo" /></el-form-item>
        <el-form-item label="容量"><el-input-number v-model="form.capacity" :min="0" /></el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">可用</el-radio>
            <el-radio :label="0">停用</el-radio>
            <el-radio :label="2">维护</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注"><el-input type="textarea" v-model="form.remark" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="onSave" :loading="formLoading">确定</el-button>
      </template>
    </el-dialog>
  </page-shell>
</template>

<script>
import { roomPage, roomCreate, roomUpdate, roomDelete } from '@/api/lab'
import { currentCanManageLab } from '@/utils/permission'
import PageShell from '@/components/PageShell.vue'
import SearchBar from '@/components/SearchBar.vue'
import DataTable from '@/components/DataTable.vue'

export default {
  name: 'LabRoom',
  components: { PageShell, SearchBar, DataTable },
  data() {
    return {
      query: { pageNum: 1, pageSize: 10, keyword: '', status: null },
      list: [],
      total: 0,
      loading: false,
      dialog: false,
      formLoading: false,
      form: { id: null, code: '', name: '', building: '', roomNo: '', capacity: 0, status: 1, remark: '' },
      columns: [
        { prop: 'code',     label: '编号',      width: 120 },
        { prop: 'name',     label: '实验室名称', minWidth: 180 },
        { prop: 'building', label: '楼栋',       width: 120 },
        { prop: 'roomNo',   label: '房间号',     width: 100 },
        { prop: 'capacity', label: '容量',       width: 80, align: 'right' },
        { prop: 'managerName', label: '管理员',  width: 120 },
        { prop: 'status',   label: '状态',       width: 110, slot: 'status' },
        { prop: 'actions',  label: '操作',       width: 180, slot: 'actions' }
      ]
    }
  },
  computed: {
    canManage() { return currentCanManageLab() }
  },
  mounted() { this.reload() },
  methods: {
    statusTag(s) {
      const map = { 1: { type: 'success', text: '可用' }, 0: { type: 'info', text: '停用' }, 2: { type: 'warning', text: '维护中' } }
      return map[s] || { type: 'info', text: '-' }
    },
    onSearch(q) { this.query = { ...this.query, pageNum: 1, ...q, status: q.status != null ? q.status : null }; this.reload() },
    onReset() { this.query = { pageNum: 1, pageSize: 10, keyword: '', status: null }; this.reload() },
    onPage(n)  { this.query.pageNum = n; this.reload() },
    onSize(n)  { this.query.pageNum = 1; this.query.pageSize = n; this.reload() },
    async reload() {
      this.loading = true
      try {
        const r = await roomPage({ ...this.query, status: this.query.status === '' ? null : this.query.status })
        this.list = r.data.records || []
        this.total = r.data.total || 0
      } finally { this.loading = false }
    },
    resetForm() { this.form = { id: null, code: '', name: '', building: '', roomNo: '', capacity: 0, status: 1, remark: '' } },
    onAdd() { this.resetForm(); this.dialog = true },
    onEdit(row) { this.form = { ...row }; this.dialog = true },
    async onSave() {
      this.formLoading = true
      try {
        if (this.form.id) await roomUpdate(this.form)
        else await roomCreate(this.form)
        this.$message.success('保存成功')
        this.dialog = false
        this.reload()
      } finally { this.formLoading = false }
    },
    onDelete(row) {
      this.$confirm(`确认删除「${row.name}」？`, '提示', { type: 'warning' })
        .then(async () => {
          try {
            await roomDelete(row.id)
            this.$message.success('删除成功')
            this.reload()
          } catch (e) {}
        }).catch(() => {})
    }
  }
}
</script>
