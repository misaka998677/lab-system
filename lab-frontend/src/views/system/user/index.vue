<template>
  <page-shell title="用户管理" desc="管理系统用户账号，分配角色权限，启用或禁用账户。">
    <template #actions>
      <el-button icon="el-icon-download" size="small" @click="onExport" :loading="exporting" type="success">导出</el-button>
      <el-button type="primary" icon="el-icon-plus" size="small" @click="onAdd">新增用户</el-button>
    </template>

    <search-bar
      keyword-placeholder="按账号/姓名搜索"
      :options="[
        { prop: 'status', label: '状态', items: [ { label: '启用', value: 1 }, { label: '禁用', value: 0 } ] },
        { prop: 'deptId', label: '部门', filterable: true, items: deptOptions }
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
      <template slot="dept" slot-scope="{ row }">
        <span v-if="row.deptName">{{ row.deptName }}</span>
        <span v-else style="color:#C0C4CC">—</span>
      </template>
      <template slot="roles" slot-scope="{ row }">
        <template v-if="row.roleNames && row.roleNames.length">
          <el-tag size="mini" v-for="(r, idx) in row.roleNames.slice(0, 2)" :key="r" style="margin-right:4px">{{ r }}</el-tag>
          <span v-if="row.roleNames.length > 2" style="color:#909399; font-size:12px">+{{ row.roleNames.length - 2 }}</span>
        </template>
        <span v-else style="color:#C0C4CC">未分配</span>
      </template>
      <template slot="status" slot-scope="{ row }">
        <span v-if="row.status === 0" style="color:#E6A23C; font-size:12px; margin-right:4px">待审核</span>
        <el-switch :value="row.status === 1" @change="v => onStatusChange(row, v ? 1 : 0)" />
      </template>
      <template slot="actions" slot-scope="{ row }">
        <el-button size="mini" @click="onEdit(row)">编辑</el-button>
        <el-button size="mini" type="warning" @click="onResetPwd(row)">重置密码</el-button>
        <el-button size="mini" type="danger" plain @click="onDelete(row)">删除</el-button>
      </template>
    </data-table>

    <el-dialog :title="form.id ? '编辑用户' : '新增用户'" :visible.sync="dialog" width="640px" @closed="resetForm">
      <el-form :model="form" label-width="100px">
        <el-form-item label="账号"><el-input v-model="form.username" :disabled="!!form.id" /></el-form-item>
        <el-form-item label="姓名"><el-input v-model="form.realName" /></el-form-item>
        <el-form-item label="手机"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item>
        <el-form-item label="部门">
          <el-select v-model="form.deptId" placeholder="选择部门" filterable clearable style="width:100%">
            <el-option v-for="d in depts" :key="d.id" :label="d.name" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.roleIds" multiple placeholder="选择角色" style="width:100%">
            <el-option v-for="r in roles" :key="r.id" :label="r.roleName" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="onSave" :loading="formLoading">确定</el-button>
      </template>
    </el-dialog>
  </page-shell>
</template>

<script>
import { userPage, userCreate, userUpdate, userDelete, userDetail, userStatus, userResetPwd, userExport, roleAll, deptAll } from '@/api/system'
import { downloadBlob } from '@/utils/download'
import PageShell from '@/components/PageShell.vue'
import SearchBar from '@/components/SearchBar.vue'
import DataTable from '@/components/DataTable.vue'

export default {
  name: 'SystemUser',
  components: { PageShell, SearchBar, DataTable },
  data() {
    return {
      query: { pageNum: 1, pageSize: 10, keyword: '', status: null, deptId: null },
      list: [], total: 0, loading: false, exporting: false,
      roles: [], depts: [],
      dialog: false, formLoading: false,
      form: { id: null, username: '', realName: '', phone: '', email: '', deptId: null, status: 1, roleIds: [] },
      columns: [
        { prop: 'username', label: '账号', width: 120 },
        { prop: 'realName', label: '姓名', width: 120 },
        { prop: 'phone', label: '手机', width: 130 },
        { prop: 'email', label: '邮箱', minWidth: 180 },
        { prop: 'deptName', label: '部门', width: 140, slot: 'dept' },
        { prop: 'roleNames', label: '角色', width: 180, slot: 'roles' },
        { prop: 'status', label: '状态', width: 130, slot: 'status' },
        { prop: 'actions', label: '操作', width: 260, slot: 'actions' }
      ]
    }
  },
  computed: {
    deptOptions() { return (this.depts || []).map(d => ({ label: d.name, value: d.id })) }
  },
  async mounted() {
    try { const r = await roleAll(); this.roles = r.data || [] } catch (e) {}
    try { const r = await deptAll(); this.depts = r.data || [] } catch (e) {}
    this.reload()
  },
  methods: {
    onSearch(q) { this.query = { ...this.query, pageNum: 1, ...q }; this.reload() },
    onReset() { this.query = { pageNum: 1, pageSize: 10, keyword: '', status: null, deptId: null }; this.reload() },
    onPage(n) { this.query.pageNum = n; this.reload() },
    onSize(n) { this.query.pageSize = n; this.query.pageNum = 1; this.reload() },
    async reload() {
      this.loading = true
      try { const r = await userPage(this.query); this.list = r.data.records || []; this.total = r.data.total || 0 }
      finally { this.loading = false }
    },
    resetForm() { this.form = { id: null, username: '', realName: '', phone: '', email: '', deptId: null, status: 1, roleIds: [] } },
    onAdd() { this.resetForm(); this.dialog = true },
    async onEdit(row) {
      try {
        const r = await userDetail(row.id)
        this.form = { ...r.data, roleIds: r.data.roleIds || [] }
        this.dialog = true
      } catch (e) {}
    },
    async onSave() {
      this.formLoading = true
      try {
        if (this.form.id) await userUpdate(this.form)
        else await userCreate({ ...this.form, password: '123456' })
        this.$message.success('保存成功')
        this.dialog = false
        this.reload()
      } finally { this.formLoading = false }
    },
    async onStatusChange(row, status) {
      try { await userStatus(row.id, status); row.status = status; this.$message.success('已更新') }
      catch (e) { this.reload() }
    },
    onResetPwd(row) {
      this.$prompt('输入新密码', '重置密码', { inputValue: '123456' })
        .then(async ({ value }) => {
          try { await userResetPwd(row.id, value); this.$message.success('已重置为: ' + value) }
          catch (e) {}
        })
        .catch(() => {})
    },
    onDelete(row) {
      this.$confirm(`确定删除「${row.username}」？`, '提示', { type: 'warning' })
        .then(async () => { try { await userDelete(row.id); this.$message.success('已删除'); this.reload() } catch (e) {} })
        .catch(() => {})
    },
    async onExport() {
      if (this.exporting) return
      this.exporting = true
      try {
        const blob = await userExport()
        downloadBlob(blob, `用户列表_${new Date().toLocaleDateString()}.xlsx`)
        this.$message.success('导出成功')
      } catch (e) {}
      finally { this.exporting = false }
    }
  }
}
</script>
