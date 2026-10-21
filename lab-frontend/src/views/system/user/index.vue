<template>
  <div class="page-shell">
    <div class="page-header">
      <div>
        <h2 class="page-title">用户管理</h2>
        <p class="page-desc">管理系统用户账号，分配角色权限，启用或禁用账户。</p>
      </div>
      <div>
        <el-button icon="el-icon-download" @click="doExport" :loading="exporting" type="success">导出</el-button>
        <el-button type="primary" icon="el-icon-plus" @click="onAdd">新增用户</el-button>
      </div>
    </div>

    <el-card shadow="never" class="table-card" body-style="padding:0">
      <div class="filter-bar">
        <el-input v-model="query.keyword" placeholder="账号/姓名" clearable style="width:220px" @keyup.enter.native="reload" />
        <el-select v-model="query.status" placeholder="状态" clearable style="width:120px; margin-left:12px">
          <el-option :value="1" label="启用" />
          <el-option :value="0" label="禁用" />
        </el-select>
        <el-button type="primary" icon="el-icon-search" style="margin-left:12px" @click="reload">查询</el-button>
        <el-button icon="el-icon-refresh-left" @click="reset">重置</el-button>
      </div>

      <el-table :data="list" v-loading="loading" :border="false" style="width:100%" :header-cell-style="headerStyle" :row-style="rowStyle" empty-text="暂无用户数据">
        <el-table-column prop="username" label="账号" width="120" />
        <el-table-column prop="realName" label="姓名" width="120" />
        <el-table-column prop="phone" label="手机" width="130" />
        <el-table-column prop="email" label="邮箱" min-width="180" show-overflow-tooltip />
        <el-table-column prop="deptName" label="部门" width="140">
          <template #default="{ row }">
            <span v-if="row.deptName">{{ row.deptName }}</span>
            <span v-else style="color:#C0C4CC">—</span>
          </template>
        </el-table-column>
        <el-table-column prop="roleNames" label="角色" width="180">
          <template #default="{ row }">
            <template v-if="row.roleNames && row.roleNames.length">
              <el-tag size="mini" v-for="r in row.roleNames.slice(0,2)" :key="r" style="margin-right:4px">{{ r }}</el-tag>
              <span v-if="row.roleNames.length > 2" style="color:#909399;font-size:12px">+{{ row.roleNames.length - 2 }}</span>
            </template>
            <span v-else style="color:#C0C4CC">未分配</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }">
            <span v-if="row.status === 0" style="color:#E6A23C;font-size:12px;margin-right:4px">待审核</span>
            <el-switch :value="row.status === 1" @change="v => onStatus(row, v ? 1 : 0)" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" align="center">
          <template #default="{ row }">
            <el-button size="mini" @click="onEdit(row)">编辑</el-button>
            <el-button size="mini" type="warning" @click="onResetPwd(row)">重置密码</el-button>
            <el-button size="mini" type="danger" plain @click="onDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination background layout="total,sizes,prev,pager,next"
        :total="total" :current-page.sync="query.pageNum" :page-size.sync="query.pageSize"
        :page-sizes="[10,20,50]" @size-change="reload" @current-change="reload"
        style="padding: 16px 20px; border-top: 1px solid #F2F6FC;" />
    </el-card>

    <el-dialog :title="form.id ? '编辑用户' : '新增用户'" :visible.sync="dialog" width="640px" @closed="resetForm">
      <el-form :model="form" ref="f" :rules="rules" label-width="100px">
        <el-form-item label="账号" prop="username"><el-input v-model="form.username" :disabled="!!form.id" /></el-form-item>
        <el-form-item label="姓名" prop="realName"><el-input v-model="form.realName" /></el-form-item>
        <el-form-item label="手机"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item>
        <el-form-item label="部门">
          <el-select v-model="form.deptId" placeholder="选择部门" filterable style="width:100%">
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
        <el-button type="primary" @click="onSave">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { userPage, userCreate, userUpdate, userDelete, userStatus, userResetPwd, userDetail, roleAll, deptAll } from '@/api/system'
import request from '@/utils/request'

export default {
  name: 'SystemUser',
  data() {
    return {
      query: { pageNum: 1, pageSize: 10, keyword: '', status: null },
      list: [], total: 0, loading: false,
      exporting: false,
      roles: [], depts: [],
      dialog: false,
      form: { id: null, username: '', realName: '', phone: '', email: '', deptId: null, status: 1, roleIds: [] },
      rules: {
        username: [{ required: true, message: '请输入账号' }],
        realName: [{ required: true, message: '请输入姓名' }]
      },
      headerStyle: { background: '#FAFBFC', color: '#606266', fontWeight: 500 }
    }
  },
  mounted() {
    const qs = this.$route.query
    if (qs.status !== undefined && qs.status !== null && qs.status !== '') {
      this.query.status = Number(qs.status)
    }
    this.reload()
    roleAll().then(r => this.roles = r.data || []).catch(() => {})
    deptAll().then(r => this.depts = r.data || []).catch(() => {})
  },
  methods: {
    async doExport() {
      if (this.exporting) return
      this.exporting = true
      try {
        const resp = await request.get('/system/user/export', { responseType: 'blob' })
        const blob = new Blob([resp], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
        const url = URL.createObjectURL(blob)
        const a = document.createElement('a')
        a.href = url
        a.download = `用户列表_${new Date().toLocaleDateString()}.xlsx`
        a.click()
        URL.revokeObjectURL(url)
        this.$message.success('导出成功')
      } catch (e) {
        this.$message.error('导出失败')
      } finally {
        this.exporting = false
      }
    },
    rowStyle({ row, rowIndex }) {
      if (row.status === 0) return { background: 'linear-gradient(90deg, #FFF8E1, #FFFBEF)' }
      return rowIndex % 2 === 1 ? { background: '#FAFBFC' } : {}
    },
    resetForm() { this.form = { id: null, username: '', realName: '', phone: '', email: '', deptId: null, status: 1, roleIds: [] } },
    async reload() {
      this.loading = true
      try { const r = await userPage(this.query); this.list = r.data.records; this.total = r.data.total } catch (e) {} finally { this.loading = false }
    },
    reset() { this.query = { pageNum: 1, pageSize: 10, keyword: '', status: null }; this.reload() },
    onAdd() { this.resetForm(); this.dialog = true },
    async onEdit(row) {
      try { const r = await userDetail(row.id); this.form = { ...r.data, roleIds: r.data.roleIds || [] }; this.dialog = true } catch (e) {}
    },
    async onSave() {
      this.$refs.f.validate(async valid => {
        if (!valid) return
        try {
          if (this.form.id) await userUpdate(this.form)
          else await userCreate({ ...this.form, password: '123456' })
          this.dialog = false
          this.$message.success('保存成功')
          this.reload()
        } catch (e) {}
      })
    },
    async onStatus(row, s) { try { await userStatus(row.id, s); row.status = s; this.$message.success('已更新') } catch (e) {} },
    onResetPwd(row) {
      this.$prompt('输入新密码', '重置密码', { inputValue: '123456' })
        .then(async ({ value }) => { try { await userResetPwd(row.id, value); this.$message.success('已重置为: ' + value) } catch (e) {} })
        .catch(() => {})
    },
    onDelete(row) {
      this.$confirm(`确定删除「${row.username}」？`, '提示', { type: 'warning' })
        .then(async () => { try { await userDelete(row.id); this.$message.success('已删除'); this.reload() } catch (e) {} })
        .catch(() => {})
    }
  }
}
</script>

<style scoped lang="scss">
</style>