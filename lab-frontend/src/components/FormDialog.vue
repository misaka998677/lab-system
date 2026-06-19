<template>
  <el-dialog
    :title="title"
    :visible.sync="innerVisible"
    :width="width"
    :close-on-click-modal="false"
    append-to-body
    @closed="$emit('closed')"
  >
    <el-form
      :model="innerModel"
      :rules="rules"
      ref="formRef"
      :label-width="labelWidth"
      @submit.native.prevent
    >
      <el-form-item
        v-for="f in fields"
        :key="f.prop"
        :label="f.label"
        :prop="f.prop"
      >
        <el-input
          v-if="f.type === 'input'"
          :placeholder="f.placeholder || '请输入'"
          :type="f.inputType || 'text'"
          v-model="innerModel[f.prop]"
          :clearable="false"
        ></el-input>
        <el-input-number
          v-else-if="f.type === 'number'"
          :min="f.min != null ? f.min : 0"
          :max="f.max != null ? f.max : 999999"
          v-model="innerModel[f.prop]"
        ></el-input-number>
        <el-select
          v-else-if="f.type === 'select'"
          :placeholder="f.placeholder || '请选择'"
          :clearable="true"
          v-model="innerModel[f.prop]"
        >
          <el-option
            v-for="opt in f.options"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          ></el-option>
        </el-select>
        <el-radio-group v-else-if="f.type === 'radio'" v-model="innerModel[f.prop]">
          <el-radio
            v-for="opt in f.options"
            :key="opt.value"
            :label="opt.value"
          >{{ opt.label }}</el-radio>
        </el-radio-group>
        <el-date-picker
          v-else-if="f.type === 'date'"
          v-model="innerModel[f.prop]"
          type="date"
          value-format="yyyy-MM-dd"
          placeholder="选择日期"
        ></el-date-picker>
        <el-input
          v-else-if="f.type === 'textarea'"
          type="textarea"
          :rows="f.rows || 3"
          :placeholder="f.placeholder || '请输入'"
          v-model="innerModel[f.prop]"
        ></el-input>
      </el-form-item>
    </el-form>

    <div slot="footer">
      <el-button @click="handleCancel">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
    </div>
  </el-dialog>
</template>

<script>
/**
 * 通用表单弹窗。
 * fields 示例：
 *   [{ prop: 'name', label: '名称', type: 'input', placeholder: '请输入实验室名' },
 *    { prop: 'status', label: '状态', type: 'radio',
 *      options: [{ label: '启用', value: 0 }, { label: '禁用', value: 1 }] }]
 *
 * 事件：@submit(payload, resetCallback)
 */
export default {
  name: 'FormDialog',
  props: {
    visible:    { type: Boolean, default: false },
    title:      { type: String, default: '编辑' },
    width:      { type: String, default: '520px' },
    fields:     { type: Array, default: () => [] },
    rules:      { type: Object, default: () => ({}) },
    modelValue: { type: Object, default: () => ({}) },
    labelWidth: { type: String, default: '100px' },
    submitting: { type: Boolean, default: false }
  },
  data() {
    return { innerModel: { ...this.modelValue } }
  },
  computed: {
    innerVisible: {
      get() { return this.visible },
      set(v) { this.$emit('update:visible', v) }
    }
  },
  watch: {
    visible(v) {
      if (v) {
        this.innerModel = { ...this.modelValue }
        this.$nextTick(() => {
          if (this.$refs.formRef) this.$refs.formRef.clearValidate()
        })
      }
    },
    modelValue: {
      deep: true,
      handler(val) { this.innerModel = { ...val } }
    }
  },
  methods: {
    handleSubmit() {
      this.$refs.formRef.validate(valid => {
        if (!valid) return
        this.$emit('submit', { ...this.innerModel })
      })
    },
    handleCancel() {
      this.$emit('cancel')
      this.$emit('update:visible', false)
    }
  }
}
</script>
