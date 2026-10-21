<template>
  <div class="page-shell">
    <div class="page-header">
      <div>
        <h2 class="page-title">耗材档案</h2>
        <p class="page-desc">维护实验耗材基础信息。库存低于预警阈值时，将在首页与统计报表中高亮提示。</p>
      </div>
      <el-button v-if="canManage" type="primary" icon="el-icon-plus" @click="onAdd">新增耗材</el-button>
      <el-button icon="el-icon-download" @click="doExport" :loading="exporting" type="success">导出</el-button>
    </div>

    <el-card shadow="never" class="table-card" body-style="padding:0">
      <div class="filter-bar">
        <el-input v-model="query.keyword" placeholder="编号/名称" clearable style="width:220px" @keyup.enter.native="reload" />
        <el-select v-model="query.labId" clearable filterable placeholder="全部实验室" style="width:180px; margin-left:12px">
          <el-option v-for="r in rooms" :key="r.id" :label="r.name" :value="r.id" />
        </el-select>
        <el-checkbox v-model="warningOnly" style="margin-left:12px">只看低库存</el-checkbox>
        <el-button type="primary" icon="el-icon-search" style="margin-left:12px" @click="reload">查询</el-button>
        <el-button icon="el-icon-refresh-left" @click="reset">重置</el-button>
      </div>

      <el-table :data="list" v-loading="loading" :border="false" style="width:100%" :header-cell-style="headerStyle" :row-style="rowStyle" empty-text="暂无耗材数据">
        <el-table-column prop="code" label="耗材编号" width="130" />
        <el-table-column prop="name" label="耗材名称" min-width="160" />
        <el-table-column prop="category" label="分类" width="110" />
        <el-table-column prop="unit" label="单位" width="80" />
        <el-table-column prop="labName" label="存放实验室" width="160" />
        <el-table-column label="库存 / 预警" width="170">
          <template #default="{ row }">
            <span :style="{ color: isWarning(row) ? '#F56C6C' : '#303133', fontWeight: isWarning(row) ? 600 : 400 }">{{ row.qty }}</span>
            <span style="color:#909399"> / {{ row.warnQty }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="isWarning(row) ? 'danger' : 'success'" effect="dark">
              {{ isWarning(row) ? '库存偏低' : '库存正常' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
        <el-table-column label="操作" width="170" align="center" v-if="canManage">
          <template #default="{ row }">
            <el-button size="mini" @click="onEdit(row)">编辑</el-button>
            <el-button size="mini" type="danger" plain @click="onDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination background layout="total,sizes,prev,pager,next"
        :total="total" :current-page.sync="query.pageNum" :page-size.sync="query.pageSize"
        :page-sizes="[10,20,50]" @size-change="reload" @current-change="reload"
        style="padding: 16px 20px; border-top: 1px solid #F2F6FC;" />
    </el-card>

    <el-dialog :title="form.id ? '编辑耗材' : '新增耗材'" :visible.sync="dialog" width="620px" @closed="resetForm">
      <el-form :model="form" :rules="rules" ref="f" label-width="100px">
        <el-form-item label="耗材编号" prop="code"><el-input v-model="form.code" /></el-form-item>
        <el-form-item label="耗材名称" prop="name"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="分类"><el-input v-model="form.category" placeholder="电子 / 办公 / 耗材" /></el-form-item>
        <el-form-item label="单位"><el-input v-model="form.unit" placeholder="根 / 包 / 只" /></el-form-item>
        <el-form-item label="当前库存" prop="qty"><el-input-number v-model="form.qty" :min="0" /></el-form-item>
        <el-form-item label="预警阈值" prop="warnQty"><el-input-number v-model="form.warnQty" :min="0" /></el-form-item>
        <el-form-item label="存放实验室">
          <el-select v-model="form.labId" clearable filterable style="width:100%">
            <el-option v-for="r in rooms" :key="r.id" :label="r.name" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注"><el-input type="textarea" v-model="form.remark" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="onSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { stockItemPage, stockItemCreate, stockItemUpdate, stockItemDelete } from '@/api/stock'
import { roomAll } from '@/api/lab'
import request from '@/utils/request'

export default {
  name: 'StockItem',
  data() {
    return {
      query: { pageNum: 1, pageSize: 10, keyword: '', labId: null },
      warningOnly: false,
      list: [], total: 0, loading: false, rooms: [], dialog: false, exporting: false,
      form: { id: null, code: '', name: '', category: '', unit: '', qty: 0, warnQty: 0, labId: null, remark: '' },
      rules: {
        code: [{ required: true, message: '请输入耗材编号' }],
        name: [{ required: true, message: '请输入耗材名称' }],
        qty: [{ required: true, message: '请输入当前库存' }],
        warnQty: [{ required: true, message: '请输入预警阈值' }]
      },
      headerStyle: { background: '#FAFBFC', color: '#606266', fontWeight: 500 }
    }
  },
  computed: {
    canManage() { return (this.$store.state.user.roles || []).some(r => ['ROLE_ADMIN', 'ROLE_LABADMIN'].includes(r)) }
  },
  async mounted() {
    try {
      const r = await roomAll()
      this.rooms = r.data || []
    } catch (e) {}
    this.reload()
  },
  methods: {
    isWarning(row) { return Number(row.qty || 0) <= Number(row.warnQty || 0) },
    rowStyle({ rowIndex }) { return rowIndex % 2 === 1 ? { background: '#FAFBFC' } : {} },
    resetForm() { this.form = { id: null, code: '', name: '', category: '', unit: '', qty: 0, warnQty: 0, labId: null, remark: '' } },
    async reload() {
      this.loading = true
      try {
        const params = { ...this.query, warningOnly: this.warningOnly ? 1 : null }
        const r = await stockItemPage(params)
        this.list = r.data.records || []
        this.total = r.data.total || 0
      } catch (e) {}
      finally { this.loading = false }
    },
    reset() {
      this.query = { pageNum: 1, pageSize: 10, keyword: '', labId: null }
      this.warningOnly = false
      this.reload()
    },
    onAdd() { this.resetForm(); this.dialog = true },
    onEdit(row) { this.form = { ...row }; this.dialog = true },
    onSave() {
      this.$refs.f.validate(async ok => {
        if (!ok) return
        try {
          if (this.form.id) await stockItemUpdate(this.form)
          else await stockItemCreate(this.form)
          this.dialog = false
          this.$message.success('耗材信息已保存')
          this.reload()
        } catch (e) {}
      })
    },
    onDelete(row) {
      this.$confirm(`确定删除「${row.name}」？`, '删除确认', { type: 'warning' })
        .then(async () => {
          try {
            await stockItemDelete(row.id)
            this.$message.success('耗材已删除')
            this.reload()
          } catch (e) {}
        }).catch(() => {})
    },
    async doExport() {
      if (this.exporting) return
      this.exporting = true
      try {
        const resp = await request.get('/stock/item/export', { responseType: 'blob' })
        const blob = new Blob([resp], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
        const url = URL.createObjectURL(blob)
        const a = document.createElement('a')
        a.href = url
        a.download = `耗材档案_${new Date().toLocaleDateString()}.xlsx`
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
