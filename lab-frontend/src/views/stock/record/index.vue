<template>
  <page-shell title="出入库记录" desc="记录耗材入库、实验领用和库存调整。出库时可关联预约单，便于追踪实验耗材去向。">
    <template #actions>
      <el-button v-if="canManage" type="success" icon="el-icon-download" size="small" @click="onOpen(1)">入库登记</el-button>
      <el-button v-if="canManage" type="warning" icon="el-icon-upload2" size="small" @click="onOpen(2)">出库登记</el-button>
      <el-button icon="el-icon-download" size="small" @click="onExport" :loading="exporting" type="success">导出</el-button>
    </template>

    <search-bar
      :keyword-visible="false"
      :options="[
        { prop: 'itemId', label: '耗材', filterable: true, items: itemOptions },
        { prop: 'type', label: '类型', items: [ { label: '入库', value: 1 }, { label: '出库', value: 2 }, { label: '盘点', value: 3 } ] }
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
      <template slot="type" slot-scope="{ row }">
        <el-tag size="mini" :type="typeTag(row.type)">{{ typeLabel(row.type) }}</el-tag>
      </template>
      <template slot="qty" slot-scope="{ row }">
        <span style="font-weight:500; color:#303133">{{ row.type === 2 ? '-' : '+' }}{{ row.qty }}</span>
        <span style="color:#909399; margin-left:4px">{{ row.unit || '' }}</span>
      </template>
      <template slot="reservation" slot-scope="{ row }">
        <span v-if="row.reservationNo" style="color:#409EFF">{{ row.reservationNo }}</span>
        <span v-else style="color:#C0C4CC">—</span>
      </template>
    </data-table>

    <el-dialog :title="form.type === 1 ? '入库登记' : '出库登记'" :visible.sync="dialog" width="540px" @closed="resetForm">
      <el-form :model="form" label-width="100px">
        <el-form-item label="耗材">
          <el-select v-model="form.itemId" filterable placeholder="请选择耗材" style="width:100%">
            <el-option v-for="i in items" :key="i.id" :label="`${i.code} ${i.name}`" :value="i.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="数量"><el-input-number v-model="form.qty" :min="1" /></el-form-item>
        <el-form-item v-if="form.type === 2" label="关联预约">
          <el-select v-model="form.reservationId" clearable filterable placeholder="可选，关联本次领用" style="width:100%">
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
  </page-shell>
</template>

<script>
import { stockItemPage, stockRecordPage, stockRecordIn, stockRecordOut, stockRecordExport } from '@/api/stock'
import { reservePage } from '@/api/reserve'
import { currentCanManageLab } from '@/utils/permission'
import { downloadBlob } from '@/utils/download'
import PageShell from '@/components/PageShell.vue'
import SearchBar from '@/components/SearchBar.vue'
import DataTable from '@/components/DataTable.vue'

export default {
  name: 'StockRecord',
  components: { PageShell, SearchBar, DataTable },
  data() {
    return {
      query: { pageNum: 1, pageSize: 10, keyword: '', itemId: null, type: null },
      list: [], total: 0, loading: false, exporting: false,
      items: [], reserves: [],
      dialog: false, submitting: false,
      form: { type: 1, itemId: null, qty: 1, reservationId: null, remark: '' },
      columns: [
        { prop: 'createTime', label: '时间', width: 170 },
        { prop: 'itemCode', label: '耗材编号', width: 130 },
        { prop: 'itemName', label: '耗材名称', minWidth: 160 },
        { prop: 'type', label: '类型', width: 100, slot: 'type' },
        { prop: 'qty', label: '数量', width: 130, slot: 'qty' },
        { prop: 'reservationNo', label: '关联预约', width: 180, slot: 'reservation' },
        { prop: 'operatorName', label: '经办人', width: 120 },
        { prop: 'remark', label: '备注', minWidth: 160 }
      ]
    }
  },
  computed: {
    canManage() { return currentCanManageLab() },
    itemOptions() { return (this.items || []).map(i => ({ label: `${i.code} ${i.name}`, value: i.id })) }
  },
  mounted() { this.loadOptions(); this.reload() },
  methods: {
    typeLabel(t) { return { 1: '入库', 2: '出库', 3: '盘点' }[t] || '—' },
    typeTag(t) { return { 1: 'success', 2: 'warning', 3: 'info' }[t] || '' },
    async loadOptions() {
      try { const r = await stockItemPage({ pageNum: 1, pageSize: 200 }); this.items = r.data.records || [] } catch (e) {}
      try {
        const r = await reservePage({ pageNum: 1, pageSize: 100 })
        const records = r.data.records || []
        this.reserves = records
      } catch (e) {}
    },
    onSearch(q) { this.query = { ...this.query, pageNum: 1, ...q }; this.reload() },
    onReset() { this.query = { pageNum: 1, pageSize: 10, keyword: '', itemId: null, type: null }; this.reload() },
    onPage(n) { this.query.pageNum = n; this.reload() },
    onSize(n) { this.query.pageSize = n; this.query.pageNum = 1; this.reload() },
    async reload() {
      this.loading = true
      try { const r = await stockRecordPage(this.query); this.list = r.data.records || []; this.total = r.data.total || 0 }
      finally { this.loading = false }
    },
    resetForm() { this.form = { type: 1, itemId: null, qty: 1, reservationId: null, remark: '' } },
    onOpen(type) { this.form = { type, itemId: null, qty: 1, reservationId: null, remark: '' }; this.dialog = true },
    async onSubmit() {
      this.submitting = true
      try {
        const payload = { itemId: this.form.itemId, qty: this.form.qty, remark: this.form.remark }
        if (this.form.type === 2) payload.reservationId = this.form.reservationId || null
        if (this.form.type === 1) await stockRecordIn(payload)
        else await stockRecordOut(payload)
        this.$message.success(this.form.type === 1 ? '入库已记录' : '出库已记录')
        this.dialog = false
        this.reload()
      } finally { this.submitting = false }
    },
    async onExport() {
      if (this.exporting) return
      this.exporting = true
      try {
        const blob = await stockRecordExport()
        downloadBlob(blob, `出入库记录_${new Date().toLocaleDateString()}.xlsx`)
        this.$message.success('导出成功')
      } catch (e) {}
      finally { this.exporting = false }
    }
  }
}
</script>
