<template>
  <div class="page-shell">
    <div class="page-header">
      <div>
        <h2 class="page-title">我的预约</h2>
        <p class="page-desc">查看、新增和管理实验室预约记录。审核通过后可在预约时段内签到使用。</p>
      </div>
      <el-button type="success" icon="el-icon-plus" @click="onAdd">新增预约</el-button>
    </div>

    <el-card shadow="never" class="table-card" body-style="padding:0">
      <div class="filter-bar">
        <el-select v-model="query.status" clearable placeholder="全部状态" style="width:140px">
          <el-option :value="0" label="待审核" />
          <el-option :value="1" label="已通过" />
          <el-option :value="2" label="已驳回" />
          <el-option :value="3" label="已签到" />
          <el-option :value="4" label="已签退" />
          <el-option :value="5" label="已取消" />
        </el-select>
        <el-button type="primary" icon="el-icon-search" style="margin-left:12px" @click="reload">查询</el-button>
      </div>

      <el-table :data="list" v-loading="loading" :border="false" style="width:100%" :header-cell-style="headerStyle" :row-style="rowStyle" empty-text="暂无预约记录">
        <el-table-column prop="reserveNo" label="预约号" width="160" />
        <el-table-column prop="labName" label="实验室" width="180" />
        <el-table-column prop="purpose" label="用途" min-width="220" show-overflow-tooltip />
        <el-table-column prop="startTime" label="开始时间" width="170" />
        <el-table-column prop="endTime" label="结束时间" width="170" />
        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260" align="center">
          <template #default="{ row }">
            <el-button size="mini" type="warning" v-if="row.status === 0" @click="onCancel(row)">取消</el-button>
            <el-button size="mini" type="primary" v-if="row.status === 1 && canCheckIn(row)" @click="onCheckIn(row)">签到</el-button>
            <el-button size="mini" type="success" v-if="row.status === 3 && canCheckOut(row)" @click="onCheckOut(row)">签退</el-button>
            <span v-if="row.status === 1 && !canCheckIn(row)" class="time-tip">时段外不可签到</span>
            <span v-if="row.status === 3 && !canCheckOut(row)" class="time-tip">时段外不可签退</span>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination background layout="total,prev,pager,next"
        :total="total" :current-page.sync="query.pageNum" :page-size="query.pageSize"
        @current-change="reload"
        style="padding: 16px 20px; border-top: 1px solid #F2F6FC;" />
    </el-card>

    <el-dialog title="新增预约" :visible.sync="dialog" width="560px" @closed="resetForm">
      <el-form :model="form" label-width="100px" :rules="rules" ref="f">
        <el-form-item label="实验室" prop="labId">
          <el-select v-model="form.labId" filterable placeholder="请选择可用实验室" style="width:100%">
            <el-option v-for="r in rooms" :key="r.id" :label="r.name" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="时段" prop="range">
          <el-date-picker v-model="form.range" type="datetimerange" value-format="yyyy-MM-dd HH:mm:ss"
            range-separator="至" start-placeholder="开始" end-placeholder="结束" style="width:100%" />
        </el-form-item>
        <el-form-item label="使用目的" prop="purpose"><el-input type="textarea" v-model="form.purpose" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="onSave">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { reserveMine, reserveApply, reserveCancel, reserveCheckIn, reserveCheckOut } from '@/api/reserve'
import { roomAll } from '@/api/lab'

const STATUS = ['待审核', '已通过', '已驳回', '已签到', '已签退', '已取消']
const TYPE = ['', 'success', 'danger', 'primary', 'info', 'warning']

export default {
  data() {
    return {
      query: { pageNum: 1, pageSize: 10, status: null },
      list: [], total: 0, loading: false, rooms: [],
      dialog: false,
      form: { labId: null, range: [], purpose: '' },
      rules: {
        labId: [{ required: true, message: '请选择实验室' }],
        range: [{ required: true, message: '请选择时段' }],
        purpose: [{ required: true, message: '请填写用途' }]
      },
      headerStyle: { background: '#FAFBFC', color: '#606266', fontWeight: 500 }
    }
  },
  async mounted() {
    try { const r = await roomAll(); this.rooms = r.data || [] } catch (e) {}
    this.reload()
  },
  methods: {
    statusText(s) { return STATUS[s] || '未知' },
    statusType(s) { return TYPE[s] || '' },
    rowStyle({ rowIndex }) { return rowIndex % 2 === 1 ? { background: '#FAFBFC' } : {} },
    resetForm() { this.form = { labId: null, range: [], purpose: '' } },
    // 判断当前时间是否在预约时间段内（允许提前15分钟签到，延后30分钟签退）
    isInReservationTime(row) {
      if (!row.startTime || !row.endTime) return false
      const now = new Date()
      const start = new Date(row.startTime)
      const end = new Date(row.endTime)
      // 签入允许提前15分钟
      const effectiveStart = new Date(start.getTime() - 15 * 60 * 1000)
      // 签退允许延后30分钟
      const effectiveEnd = new Date(end.getTime() + 30 * 60 * 1000)
      return now >= effectiveStart && now <= effectiveEnd
    },
    canCheckIn(row) {
      return row.status === 1 && this.isInReservationTime(row)
    },
    canCheckOut(row) {
      return row.status === 3 && this.isInReservationTime(row)
    },
    async reload() {
      this.loading = true
      try {
        const params = { ...this.query, status: this.query.status === null ? undefined : this.query.status }
        const r = await reserveMine(params)
        this.list = r.data.records
        this.total = r.data.total
      } catch (e) {}
      finally { this.loading = false }
    },
    onAdd() { this.resetForm(); this.dialog = true },
    onSave() {
      this.$refs.f.validate(async ok => {
        if (!ok) return
        try {
          await reserveApply({
            labId: this.form.labId,
            purpose: this.form.purpose,
            startTime: this.form.range[0],
            endTime: this.form.range[1]
          })
          this.dialog = false
          this.$message.success('预约已提交，等待审核')
          this.reload()
        } catch (e) {}
      })
    },
    onCancel(row) {
      this.$confirm('确认取消此次预约？', '提示', { type: 'warning' })
        .then(async () => {
          try { await reserveCancel(row.id); this.$message.success('已取消'); this.reload() } catch (e) {}
        }).catch(() => {})
    },
    async onCheckIn(row) { try { await reserveCheckIn(row.id); this.$message.success('已签到'); this.reload() } catch (e) {} },
    async onCheckOut(row) { try { await reserveCheckOut(row.id); this.$message.success('已签退'); this.reload() } catch (e) {} }
  }
}
</script>

<style scoped lang="scss">
.time-tip { color: #909399; font-size: 12px; }
</style>
