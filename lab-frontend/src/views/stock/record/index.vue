<template>
  <div class="page-shell">
    <div class="page-header">
      <div>
        <h2 class="page-title">出入库记录</h2>
        <p class="page-desc">记录耗材入库、实验领用和库存调整。出库时可关联预约单，便于追踪实验耗材去向。</p>
      </div>
      <div>
        <el-button v-if="canManage" type="success" icon="el-icon-download" @click="onOpen(1)">入库登记</el-button>
        <el-button v-if="canManage" type="warning" icon="el-icon-upload2" @click="onOpen(2)">出库登记</el-button>
        <el-button icon="el-icon-download" @click="doExport" :loading="exporting" type="success">导出</el-button>
      </div>
    </div>

    <el-card shadow="never" class="table-card" body-style="padding:0">
      <div class="filter-bar">
        <el-select v-model="query.itemId" clearable filterable placeholder="全部耗材" style="width:220px">
          <el-option v-for="i in items" :key="i.id" :label="`${i.code} ${i.name}`" :value="i.id" />
        </el-select>
        <el-select v-model="query.type" clearable placeholder="全部类型" style="width:140px; margin-left:12px">
          <el-option :value="1" label="入库" />
          <el-option :value="2" label="出库" />
          <el-option :value="3" label="盘点" />
        </el-select>
        <el-button type="primary" icon="el-icon-search" style="margin-left:12px" @click="onSearch">查询</el-button>
        <el-button icon="el-icon-refresh-left" @click="reset">重置</el-button>
      </div>

      <el-table :data="list" v-loading="loading" :border="false" style="width:100%" :header-cell-style="headerStyle" :row-style="rowStyle" empty-text="暂无记录">
        <el-table-column prop="createTime" label="时间" width="170" />
        <el-table-column prop="itemCode" label="耗材编号" width="130" />
        <el-table-column prop="itemName" label="耗材名称" min-width="160" />
        <el-table-column label="类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="typeTag(row.type)">{{ typeLabel(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="数量" width="130" align="right">
          <template #default="{ row }">
            <span style="font-weight:500; color:#303133">{{ row.type === 2 ? '-' : '+' }}{{ row.qty }}</span>
            <span style="color:#909399; margin-left:4px">{{ row.unit || '' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="reservationNo" label="关联预约" width="160">
          <template #default="{ row }">
            <span v-if="row.reservationNo" style="color:#409EFF">{{ row.reservationNo }}</span>
            <span v-else style="color:#C0C4CC">—</span>
          </template>
        </el-table-column>
        <el-table-column prop="operatorName" label="经办人" width="120" />
        <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
      </el-table>

      <el-pagination background layout="total,sizes,prev,pager,next"
        :total="total" :current-page.sync="query.pageNum" :page-size.sync="query.pageSize"
        :page-sizes="[10,20,50]" @size-change="onSizeChange" @current-change="reload"
        style="padding: 16px 20px; border-top: 1px solid #F2F6FC;" />
    </el-card>

    <el-dialog :title="form.type === 1 ? '入库登记' : '出库登记'" :visible.sync="dialog" width="540px" @closed="resetForm">
      <el-form :model="form" :rules="rules" ref="f" label-width="100px">
        <el-form-item label="耗材" prop="itemId">
          <el-select v-model="form.itemId" filterable placeholder="请选择耗材" style="width:100%">
            <el-option v-for="i in items" :key="i.id" :label="`${i.code} ${i.name} (库存${i.qty}${i.unit || ''})`" :value="i.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="数量" prop="qty"><el-input-number v-model="form.qty" :min="1" /></el-form-item>
        <el-form-item v-if="form.type === 2" label="关联预约">
          <el-select v-model="form.reservationId" clearable filterable placeholder="可选，关联本次领用的预约单" style="width:100%">
            <el-option v-for="r in reserves" :key="r.id" :label="`${r.reserveNo} ${r.labName || ''}`" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注"><el-input type="textarea" v-model="form.remark" :rows="3" placeholder="可填写来源、用途等" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="onSubmit">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { stockItemPage, stockRecordPage, stockRecordIn, stockRecordOut } from '@/api/stock'
import { reservePage } from '@/api/reserve'
import request from '@/utils/request'

export default {
  name: 'StockRecord',
  data() {
    return {
      query: { pageNum: 1, pageSize: 10, itemId: null, type: null },
      list: [], total: 0, loading: false, exporting: false,
      items: [], reserves: [],
      dialog: false, submitting: false,
      form: { type: 1, itemId: null, qty: 1, reservationId: null, remark: '' },
      rules: {
        itemId: [{ required: true, message: '请选择耗材' }],
        qty: [{ required: true, message: '请填写数量' }]
      },
      headerStyle: { background: '#FAFBFC', color: '#606266', fontWeight: 500 }
    }
  },
  computed: {
    canManage() { return (this.$store.state.user.roles || []).some(r => ['ROLE_ADMIN', 'ROLE_LABADMIN'].includes(r)) }
  },
  mounted() { this.loadOptions(); this.reload() },
  methods: {
    typeLabel(t) { return { 1: '入库', 2: '出库', 3: '盘点' }[t] || '—' },
    typeTag(t) { return { 1: 'success', 2: 'warning', 3: 'info' }[t] || '' },
    rowStyle({ rowIndex }) { return rowIndex % 2 === 1 ? { background: '#FAFBFC' } : {} },
    resetForm() { this.form = { type: 1, itemId: null, qty: 1, reservationId: null, remark: '' } },
    async loadOptions() {
      try {
        const r = await stockItemPage({ pageNum: 1, pageSize: 200 })
        this.items = r.data.records || []
      } catch (e) { this.items = [] }
      try {
        const r = await reservePage({ pageNum: 1, pageSize: 100 })
        const records = r.data.records || []
        this.reserves = records.filter(it => it.status === 1 || it.status === 3)
      } catch (e) { this.reserves = [] }
    },
    async reload() {
      this.loading = true
      try {
        const r = await stockRecordPage(this.query)
        this.list = r.data.records || []
        this.total = r.data.total || 0
      } catch (e) {}
      finally { this.loading = false }
    },
    onSearch() { this.query.pageNum = 1; this.reload() },
    onSizeChange(size) { this.query.pageSize = size; this.query.pageNum = 1; this.reload() },
    reset() { this.query = { pageNum: 1, pageSize: 10, itemId: null, type: null }; this.reload() },
    onOpen(type) {
      this.form = { type, itemId: null, qty: 1, reservationId: null, remark: '' }
      this.dialog = true
      this.$nextTick(() => this.$refs.f && this.$refs.f.clearValidate())
    },
    onSubmit() {
      this.$refs.f.validate(async ok => {
        if (!ok) return
        this.submitting = true
        try {
          const payload = { itemId: this.form.itemId, qty: this.form.qty, remark: this.form.remark }
          if (this.form.type === 2) payload.reservationId = this.form.reservationId || null
          if (this.form.type === 1) await stockRecordIn(payload)
          else await stockRecordOut(payload)
          this.dialog = false
          this.$message.success(this.form.type === 1 ? '入库已记录' : '出库已记录')
          this.reload()
          this.loadOptions()
        } catch (e) {}
        finally { this.submitting = false }
      })
    },
    async doExport() {
      if (this.exporting) return
      this.exporting = true
      try {
        const resp = await request.get('/stock/record/export', { responseType: 'blob' })
        const blob = new Blob([resp], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
        const url = URL.createObjectURL(blob)
        const a = document.createElement('a')
        a.href = url
        a.download = `出入库记录_${new Date().toLocaleDateString()}.xlsx`
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
