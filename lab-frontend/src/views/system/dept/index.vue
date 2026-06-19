<template>
  <page-shell title="部门管理" desc="维护部门/学院树形结构，支持层级管理。">
    <template #actions>
      <el-button type="primary" icon="el-icon-plus" size="small" @click="onAdd(null)">新增顶级</el-button>
    </template>

    <data-table
      :columns="columns"
      :data="tree"
      :total="tree.length"
      :page-num="query.pageNum"
      :page-size="1000"
      :loading="loading"
      row-key="id"
      :tree-props="{ children: 'children' }"
      :default-expand-all="true"
    >
      <template slot="actions" slot-scope="{ row }">
        <el-button size="mini" @click="onAdd(row.id)">添加子项</el-button>
        <el-button size="mini" @click="onEdit(row)">编辑</el-button>
        <el-button size="mini" type="danger" plain @click="onDelete(row)">删除</el-button>
      </template>
    </data-table>

    <el-dialog :title="form.id ? '编辑部门' : '新增部门'" :visible.sync="dialog" width="420px" @closed="resetForm">
      <el-form :model="form" label-width="80px">
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sortNo" :min="0" /></el-form-item>
        <el-form-item label="上级">
          <el-select v-model="form.parentId" filterable placeholder="顶级" clearable style="width:100%">
            <el-option label="顶级（无上级）" :value="0" />
            <el-option v-for="d in flatOptions" :key="d.id" :label="d.name" :value="d.id" />
          </el-select>
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
import { deptAll, deptCreate, deptUpdate, deptDelete } from '@/api/system'
import PageShell from '@/components/PageShell.vue'
import DataTable from '@/components/DataTable.vue'

function flatten(list) {
  const result = []
  const walk = (arr) => {
    arr.forEach(item => {
      result.push({ id: item.id, name: item.name })
      if (item.children && item.children.length) walk(item.children)
    })
  }
  walk(list)
  return result
}

function toTree(list) {
  const map = new Map()
  list.forEach(d => map.set(d.id, { ...d, children: [] }))
  const roots = []
  list.forEach(d => {
    const node = map.get(d.id)
    if (d.parentId && map.has(d.parentId)) map.get(d.parentId).children.push(node)
    else roots.push(node)
  })
  return roots
}

export default {
  name: 'SystemDept',
  components: { PageShell, DataTable },
  data() {
    return {
      query: { pageNum: 1, pageSize: 1000, keyword: '' },
      list: [], loading: false,
      dialog: false, formLoading: false,
      form: { id: null, parentId: 0, name: '', sortNo: 0, status: 1, remark: '' },
      columns: [
        { prop: 'name', label: '名称', minWidth: 220 },
        { prop: 'sortNo', label: '排序', width: 100 },
        { prop: 'remark', label: '备注', minWidth: 220 },
        { prop: 'actions', label: '操作', width: 260, slot: 'actions' }
      ]
    }
  },
  computed: {
    tree() { return toTree(this.list || []) },
    flatOptions() { return flatten(this.tree) }
  },
  mounted() { this.reload() },
  methods: {
    async reload() {
      this.loading = true
      try { const r = await deptAll(); this.list = r.data || [] }
      finally { this.loading = false }
    },
    resetForm() { this.form = { id: null, parentId: 0, name: '', sortNo: 0, status: 1, remark: '' } },
    onAdd(parentId) {
      this.form = { id: null, parentId: parentId || 0, name: '', sortNo: 0, status: 1, remark: '' }
      this.dialog = true
    },
    onEdit(row) { this.form = { id: row.id, parentId: row.parentId || 0, name: row.name, sortNo: row.sortNo || 0, status: row.status || 1, remark: row.remark || '' }; this.dialog = true },
    async onSave() {
      this.formLoading = true
      try {
        if (this.form.id) await deptUpdate(this.form)
        else await deptCreate(this.form)
        this.$message.success('保存成功')
        this.dialog = false
        this.reload()
      } finally { this.formLoading = false }
    },
    onDelete(row) {
      this.$confirm(`删除「${row.name}」？`, '提示', { type: 'warning' })
        .then(async () => { try { await deptDelete(row.id); this.$message.success('已删除'); this.reload() } catch (e) {} })
        .catch(() => {})
    }
  }
}
</script>
