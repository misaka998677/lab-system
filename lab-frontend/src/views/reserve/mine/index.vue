<template>
  <page-shell title="我的预约" desc="查看、新增和管理实验室预约记录。审核通过后可在预约时段内签到使用。">
    <template #actions>
      <el-button type="success" icon="el-icon-plus" size="small" @click="onAdd">新增预约</el-button>
    </template>

    <search-bar
      :keyword-visible="false"
      :options="[
        { prop: 'status', label: '状态', items: [
          { label: '待审核', value: 0 },
          { label: '已通过', value: 1 },
          { label: '已驳回', value: 2 },
          { label: '已签到', value: 3 },
          { label: '已签退', value: 4 },
          { label: '已取消', value: 5 }
        ] }
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
      <template slot="status" slot-scope="{ row }">
        <el-tag size="mini" :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
      </template>
      <template slot="actions" slot-scope="{ row }">
        <el-button size="mini" type="warning" v-if="row.status === 0 || row.status === 1 || row.status === 3" @click="onCancel(row)">取消</el-button>
        <el-button size="mini" type="primary" v-if="row.status === 1 && canCheckIn(row)" @click="onCheckIn(row)">签到</el-button>
        <el-button size="mini" type="success" v-if="row.status === 3 && canCheckOut(row)" @click="onCheckOut(row)">签退</el-button>
        <span v-if="row.status === 1 && !canCheckIn(row)" style="color:#909399; font-size:12px">时段外不可签到</span>
        <span v-if="row.status === 3 && !canCheckOut(row)" style="color:#909399; font-size:12px">时段外不可签退</span>
      </template>
    </data-table>

    <el-dialog title="新增预约" :visible.sync="dialog" width="560px" @closed="resetForm">
      <el-form :model="form" label-width="100px">
        <el-form-item label="实验室">
          <el-select v-model="form.labId" filterable placeholder="请选择可用实验室" style="width:100%">
            <el-option v-for="r in rooms" :key="r.id" :label="r.name" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="时段">
          <el-date-picker v-model="form.range" type="datetimerange" value-format="yyyy-MM-dd HH:mm:ss"
            range-separator="至" start-placeholder="开始" end-placeholder="结束" style="width:100%" />
        </el-form-item>
        <el-form-item label="使用目的"><el-input type="textarea" v-model="form.purpose" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="onSave" :loading="formLoading">提交</el-button>
      </template>
    </el-dialog>
  </page-shell>
</template>

<script>
import { reserveMine, reserveApply, reserveCancel, reserveCheckIn, reserveCheckOut } from '@/api/reserve'
import { roomAll } from '@/api/lab'
import PageShell from '@/components/PageShell.vue'
import SearchBar from '@/components/SearchBar.vue'
import DataTable from '@/components/DataTable.vue'

const STATUS_MAP = { 0: ['待审核', 'info'], 1: ['已通过', 'success'], 2: ['已驳回', 'danger'], 3: ['已签到', 'primary'], 4: ['已签退', ''], 5: ['已取消', 'warning'] }

export default {
  name: 'ReserveMine',
  components: { PageShell, SearchBar, DataTable },
  data() {
    return {
      query: { pageNum: 1, pageSize: 10, keyword: '', status: null },
      list: [], total: 0, loading: false, rooms: [],
      dialog: false, formLoading: false,
      form: { labId: null, range: [], purpose: '' },
      columns: [
        { prop: 'reserveNo', label: '预约号', width: 160 },
        { prop: 'labName', label: '实验室', width: 180 },
        { prop: 'purpose', label: '用途', minWidth: 220 },
        { prop: 'startTime', label: '开始时间', width: 170 },
        { prop: 'endTime', label: '结束时间', width: 170 },
        { prop: 'status', label: '状态', width: 110, slot: 'status' },
        { prop: 'actions', label: '操作', width: 240, slot: 'actions' }
      ]
    }
  },
  async mounted() {
    try { const r = await roomAll(); this.rooms = r.data || [] } catch (e) {}
    this.reload()
  },
  methods: {
    statusText(s) { const m = STATUS_MAP[s]; return m ? m[0] : '未知' },
    statusType(s) { const m = STATUS_MAP[s]; return m ? m[1] : '' },
    isInReservationTime(row) {
      if (!row.startTime || !row.endTime) return false
      const now = new Date()
      const start = new Date(row.startTime)
      const end = new Date(row.endTime)
      const effectiveStart = new Date(start.getTime() - 15 * 60 * 1000)
      const effectiveEnd = new Date(end.getTime() + 30 * 60 * 1000)
      return now >= effectiveStart && now <= effectiveEnd
    },
    canCheckIn(row) { return row.status === 1 && this.isInReservationTime(row) },
    canCheckOut(row) { return row.status === 3 && this.isInReservationTime(row) },
    onSearch(q) { this.query = { ...this.query, pageNum: 1, ...q }; this.reload() },
    onReset() { this.query = { pageNum: 1, pageSize: 10, keyword: '', status: null }; this.reload() },
    onPage(n) { this.query.pageNum = n; this.reload() },
    onSize(n) { this.query.pageSize = n; this.query.pageNum = 1; this.reload() },
    async reload() {
      this.loading = true
      try {
        const params = { ...this.query }
        const r = await reserveMine(params)
        this.list = r.data.records || []
        this.total = r.data.total || 0
      } finally { this.loading = false }
    },
    resetForm() { this.form = { labId: null, range: [], purpose: '' } },
    onAdd() { this.resetForm(); this.dialog = true },
    async onSave() {
      this.formLoading = true
      try {
        await reserveApply({
          labId: this.form.labId,
          purpose: this.form.purpose,
          startTime: this.form.range && this.form.range[0],
          endTime: this.form.range && this.form.range[1]
        })
        this.$message.success('预约已提交，等待审核')
        this.dialog = false
        this.reload()
      } finally { this.formLoading = false }
    },
    onCancel(row) {
      if (row.status === 3) {
        // 已签到必须填写原因
        this.$prompt('已签到的预约取消时必须填写原因', '取消预约', {
          confirmButtonText: '确认取消',
          cancelButtonText: '返回',
          inputPattern: /\S+/,
          inputErrorMessage: '取消原因不能为空'
        }).then(async ({ value }) => {
          try { await reserveCancel(row.id, value); this.$message.success('已取消'); this.reload() } catch (e) {}
        }).catch(() => {})
      } else {
        this.$confirm('确认取消此次预约？', '提示', { type: 'warning' })
          .then(async () => { try { await reserveCancel(row.id); this.$message.success('已取消'); this.reload() } catch (e) {} })
          .catch(() => {})
      }
    },
    async onCheckIn(row) { try { await reserveCheckIn(row.id); this.$message.success('已签到'); this.reload() } catch (e) {} },
    async onCheckOut(row) { try { await reserveCheckOut(row.id); this.$message.success('已签退'); this.reload() } catch (e) {} }
  }
}
</script>
