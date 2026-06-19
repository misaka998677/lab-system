<template>
  <div class="register-wrap">
    <el-card class="register-card">
      <h2 class="title">注册实验室账号</h2>
      <p class="subtitle">提交后即可使用学生账号；教师账号需管理员审核启用。</p>
      <el-form :model="form" :rules="rules" ref="f" label-width="90px" @submit.native.prevent="handleSubmit">
        <el-form-item label="账号" prop="username">
          <el-input v-model="form.username" placeholder="请输入账号" prefix-icon="el-icon-user" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input show-password v-model="form.password" placeholder="请输入密码" prefix-icon="el-icon-lock" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input show-password v-model="form.confirmPassword" placeholder="请再次输入密码" prefix-icon="el-icon-lock" />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="form.realName" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="账号类型" prop="roleType">
          <el-radio-group v-model="form.roleType">
            <el-radio label="student">学生</el-radio>
            <el-radio label="teacher">教师</el-radio>
          </el-radio-group>
          <el-alert
            v-if="form.roleType === 'teacher'"
            class="teacher-tip"
            title="教师账号提交后需要管理员审核启用。"
            type="warning"
            :closable="false"
            show-icon
          />
        </el-form-item>
        <el-form-item label="所属部门" prop="deptId">
          <el-select v-model="form.deptId" placeholder="请选择部门" filterable clearable style="width:100%" :loading="deptLoading">
            <el-option v-for="item in deptOptions" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" style="width:100%" :loading="loading" @click="handleSubmit">注 册</el-button>
        </el-form-item>
      </el-form>
      <p class="login-link">已有账号？<el-link type="primary" @click="$router.push('/login')">返回登录</el-link></p>
    </el-card>
  </div>
</template>

<script>
import { register } from '@/api/auth'
import { deptAll } from '@/api/system'

export default {
  data() {
    const validateConfirmPassword = (rule, value, callback) => {
      if (!value) return callback(new Error('请再次输入密码'))
      if (value !== this.form.password) return callback(new Error('两次输入的密码不一致'))
      callback()
    }
    const validateOptionalEmail = (rule, value, callback) => {
      if (!value) return callback()
      if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)) return callback(new Error('请输入正确的邮箱格式'))
      callback()
    }
    return {
      form: {
        username: '',
        password: '',
        confirmPassword: '',
        realName: '',
        phone: '',
        email: '',
        roleType: 'student',
        deptId: ''
      },
      deptOptions: [],
      deptLoading: false,
      loading: false,
      rules: {
        username: [
          { required: true, message: '请输入账号' },
          { pattern: /^[A-Za-z0-9_]{3,20}$/, message: '账号需为3-20位字母、数字或下划线' }
        ],
        password: [
          { required: true, message: '请输入密码' },
          { min: 6, message: '密码至少6位' }
        ],
        confirmPassword: [{ required: true, validator: validateConfirmPassword }],
        realName: [{ required: true, message: '请输入真实姓名' }],
        phone: [{ pattern: /^\d{11}$/, message: '手机号需为11位数字' }],
        email: [{ validator: validateOptionalEmail }],
        roleType: [{ required: true, message: '请选择账号类型' }],
        deptId: [{ required: true, message: '请选择部门' }]
      }
    }
  },
  created() {
    this.loadDeptOptions()
  },
  methods: {
    async loadDeptOptions() {
      this.deptLoading = true
      try {
        const res = await deptAll()
        this.deptOptions = res.data || []
      } catch (e) {}
      finally {
        this.deptLoading = false
      }
    },
    handleSubmit() {
      this.form.username = this.form.username.trim()
      this.form.realName = this.form.realName.trim()
      this.form.phone = this.form.phone.trim()
      this.form.email = this.form.email.trim()

      this.$refs.f.validate(async valid => {
        if (!valid) return
        this.loading = true
        try {
          const payload = { ...this.form }
          const res = await register(payload)
          const msg = res.data || res.message || '注册成功'

          if (this.form.roleType === 'student') {
            try {
              await this.$store.dispatch('user/logout')
              await this.$store.dispatch('user/login', {
                username: this.form.username,
                password: this.form.password
              })
              const info = await this.$store.dispatch('user/fetchInfo')
              await this.$store.dispatch('permission/generateRoutes')
              const roles = (info && info.roles) || []
              const { defaultRouteForRole } = await import('@/utils/role-map')
              const target = defaultRouteForRole(roles)
              this.$message.success(msg + '，已为你登录')
              location.replace(target)
              return
            } catch (_) {
              this.$message.warning(msg + '，请手动登录')
              this.$router.replace('/login')
              return
            }
          }
          this.$message.success(msg + '，教师账号需管理员审核，请稍后登录')
          this.$router.replace('/login')
        } catch (e) {}
        finally {
          this.loading = false
        }
      })
    }
  }
}
</script>

<style scoped>
.register-wrap { min-height: 100vh; background: linear-gradient(135deg, #304156 0%, #2b85e4 100%); display:flex; align-items:center; justify-content:center; padding: 32px 16px; box-sizing: border-box; }
.register-card { width: 520px; max-width: 100%; }
.title { text-align:center; margin:0; color:#303133; }
.subtitle { color:#909399; font-size:13px; text-align:center; margin:10px 0 22px; }
.teacher-tip { margin-top: 10px; }
.login-link { color:#999; font-size:13px; text-align:center; margin: 6px 0 0; }
@media (max-width: 560px) {
  .register-card { width: 100%; }
  .register-wrap >>> .el-form-item__label { float: none; display: block; text-align: left; width: auto !important; }
  .register-wrap >>> .el-form-item__content { margin-left: 0 !important; }
}
</style>
