<template>
  <page-shell title="角色管理" desc="定义系统角色，为角色分配菜单权限。">
    <template #actions>
      <el-button type="primary" icon="el-icon-plus" size="small" @click="onAdd">新增角色</el-button>
    </template>

    <search-bar
      keyword-placeholder="按角色编码/名称搜索"
      :options="[]"
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
      <template slot="actions" slot-scope="{ row }">
        <el-button size="mini" @click="onEdit(row)">编辑</el-button>
        <el-button size="mini" type="warning" @click="onPerm(row)">分配菜单</el-button>
        <el-button size="mini" type="danger" plain @click="onDelete(row)">删除</el-button>
      </template>
    </data-table>

    <el-dialog :title="form.id ? '编辑角色' : '新增角色'" :visible.sync="dialog" width="500px" @closed="resetForm">
      <el-form :model="form" label-width="80px">
        <el-form-item label="编码"><el-input v-model="form.roleCode" placeholder="例如 ROLE_XXX" /></el-form-item>
        <el-form-item label="名称"><el-input v-model="form.roleName" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sortNo" :min="0" /></el-form-item>
        <el-form-item label="备注"><el-input type="textarea" v-model="form.remark" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="onSave" :loading="formLoading">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog title="分配菜单" :visible.sync="permDialog" width="420px" @closed="resetPerm">
      <el-tree ref="tree" :data="menuTree" node-key="id" show-checkbox :props="{ label: 'name', children: 'children' }" default-expand-all />
      <template #footer>
        <el-button @click="permDialog = false">取消</el-button>
        <el-button type="primary" @click="onSavePerm" :loading="formLoading">保存</el-button>
      </template>
    </el-dialog>
  </page-shell>
</template>

<script>
import { rolePage, roleCreate, roleUpdate, roleDelete, roleDetail, menuTree, roleAssignMenu } from '@/api/system'
import PageShell from '@/components/PageShell.vue'
import SearchBar from '@/components/SearchBar.vue'
import DataTable from '@/components/DataTable.vue'

export default {
  name: 'SystemRole',
  components: { PageShell, SearchBar, DataTable },
  data() {
    return {
      query: { pageNum: 1, pageSize: 10, keyword: '' },
      list: [], total: 0, loading: false,
      dialog: false, permDialog: false, formLoading: false,
      form: { id: null, roleCode: '', roleName: '', sortNo: 0, remark: '', status: 1 },
      menuTree: [],
      permRoleId: null,
      columns: [
        { prop: 'roleCode', label: '编码', width: 160 },
        { prop: 'roleName', label: '名称', width: 160 },
        { prop: 'sortNo', label: '排序', width: 80 },
        { prop: 'remark', label: '备注', minWidth: 200 },
        { prop: 'actions', label: '操作', width: 260, slot: 'actions' }
      ]
    }
  },
  mounted() { this.reload() },
  methods: {
    onSearch(q) { this.query = { ...this.query, pageNum: 1, ...q }; this.reload() },
    onReset() { this.query = { pageNum: 1, pageSize: 10, keyword: '' }; this.reload() },
    onPage(n) { this.query.pageNum = n; this.reload() },
    onSize(n) { this.query.pageSize = n; this.query.pageNum = 1; this.reload() },
    async reload() {
      this.loading = true
      try { const r = await rolePage(this.query); this.list = r.data.records || []; this.total = r.data.total || 0 }
      finally { this.loading = false }
    },
    resetForm() { this.form = { id: null, roleCode: '', roleName: '', sortNo: 0, remark: '', status: 1 } },
    resetPerm() { this.menuTree = []; this.permRoleId = null },
    onAdd() { this.resetForm(); this.dialog = true },
    async onEdit(row) {
      try { const r = await roleDetail(row.id); this.form = { ...r.data }; this.dialog = true }
      catch (e) {}
    },
    async onSave() {
      this.formLoading = true
      try {
        if (this.form.id) await roleUpdate(this.form)
        else await roleCreate(this.form)
        this.$message.success('保存成功')
        this.dialog = false
        this.reload()
      } finally { this.formLoading = false }
    },
    onDelete(row) {
      this.$confirm(`删除角色「${row.roleName}」？`, '提示', { type: 'warning' })
        .then(async () => { try { await roleDelete(row.id); this.$message.success('已删除'); this.reload() } catch (e) {} })
        .catch(() => {})
    },
    async onPerm(row) {
      try {
        const r1 = await menuTree(); this.menuTree = r1.data || []
        const r2 = await roleDetail(row.id); this.permRoleId = row.id
        this.permDialog = true
        this.$nextTick(() => {
          if (this.$refs.tree && r2.data.menuIds) {
            this.$refs.tree.setCheckedKeys(r2.data.menuIds)
          }
        })
      } catch (e) {}
    },
    async onSavePerm() {
      this.formLoading = true
      try {
        const checkedKeys = (this.$refs.tree && this.$refs.tree.getCheckedKeys) ? this.$refs.tree.getCheckedKeys() : []
        await roleAssignMenu(this.permRoleId, checkedKeys)
        this.$message.success('已保存')
        this.permDialog = false
      } finally { this.formLoading = false }
    }
  }
}
</script>
