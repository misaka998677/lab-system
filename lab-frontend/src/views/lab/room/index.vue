<template>
  <div class="page-shell">
    <div class="page-header">
      <div>
        <h2 class="page-title">实验室档案</h2>
        <p class="page-desc">管理实验室基本信息：名称、楼栋、容量与运行状态。</p>
      </div>
      <el-button v-if="isAdmin" type="primary" icon="el-icon-plus" @click="onAdd">新增实验室</el-button>
    </div>

    <el-card shadow="never" class="table-card" body-style="padding:0">
      <div class="filter-bar">
        <el-input v-model="query.keyword" placeholder="编号/名称" clearable style="width:220px; margin-right:12px;" @keyup.enter.native="reload" />
        <el-select v-model="query.status" placeholder="全部状态" clearable style="width:140px; margin-right:12px;">
          <el-option :value="null" label="全部" />
          <el-option :value="1" label="可用" />
          <el-option :value="0" label="停用" />
          <el-option :value="2" label="维护中" />
        </el-select>
        <el-button type="primary" icon="el-icon-search" @click="reload">查询</el-button>
      </div>

      <el-table
        :data="list"
        :border="false"
        empty-text="暂无实验室数据"
        style="width:100%"
        :header-cell-style="headerStyle"
        :row-style="rowStyle"
      >
        <el-table-column prop="code" label="编号" width="120" />
        <el-table-column prop="name" label="实验室名称" min-width="180" />
        <el-table-column prop="building" label="楼栋" width="120" />
        <el-table-column prop="roomNo" label="房间号" width="100" />
        <el-table-column prop="capacity" label="容量" width="80" align="right" />
        <el-table-column prop="deptName" label="学院" width="140" />
        <el-table-column prop="managerName" label="管理员" width="120" />
        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 1 ? 'success' : (row.status === 2 ? 'warning' : 'info')">
              {{ ['停用', '可用', '维护'][row.status] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" align="center" v-if="canManage">
          <template #default="{ row }">
            <el-button size="mini" @click="onEdit(row)">编辑</el-button>
            <el-button size="mini" type="danger" v-if="isAdmin" @click="onDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        background
        layout="total,prev,pager,next"
        :total="total"
        :current-page.sync="query.pageNum"
        :page-size="query.pageSize"
        @current-change="reload"
        style="padding: 16px 20px; border-top: 1px solid #F2F6FC;"
      />
    </el-card>

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
        <el-button type="primary" @click="onSave">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { roomPage, roomCreate, roomUpdate, roomDelete } from '@/api/lab'

export default {
  name: 'LabRoom',
  data() {
    return {
      query: { pageNum: 1, pageSize: 10, keyword: '', status: null },
      list: [],
      total: 0,
      loading: false,
      dialog: false,
      form: { id: null, code: '', name: '', building: '', roomNo: '', capacity: 0, status: 1, remark: '' },
      headerStyle: { background: '#FAFBFC', color: '#606266', fontWeight: 500 }
    }
  },
  computed: {
    isAdmin() {
      return (this.$store.state.user.roles || []).includes('ROLE_ADMIN')
    },
    canManage() {
      return (this.$store.state.user.roles || []).some(r => ['ROLE_ADMIN', 'ROLE_LABADMIN'].includes(r))
    }
  },
  mounted() { this.reload() },
  methods: {
    rowStyle({ rowIndex }) { return rowIndex % 2 === 1 ? { background: '#FAFBFC' } : {} },
    resetForm() { this.form = { id: null, code: '', name: '', building: '', roomNo: '', capacity: 0, status: 1, remark: '' } },
    async reload() {
      this.loading = true
      try {
        const r = await roomPage(this.query)
        this.list = r.data.records
        this.total = r.data.total
      } catch (e) {}
      finally {
        this.loading = false
      }
    },
    onAdd() { this.resetForm(); this.dialog = true },
    onEdit(row) { this.form = { ...row }; this.dialog = true },
    async onSave() {
      try {
        if (this.form.id) {
          await roomUpdate(this.form)
          this.$message.success('更新成功')
        } else {
          await roomCreate(this.form)
          this.$message.success('创建成功')
        }
        this.dialog = false
        this.reload()
      } catch (e) {}
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

<style scoped lang="scss">
</style>
