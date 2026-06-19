<template>
  <div class="login-wrap">
    <div class="login-left">
      <div class="grid-bg"></div>
      <div class="glow glow-1"></div>
      <div class="glow glow-2"></div>
      <div class="brand-area">
        <div class="brand-icon"><i class="el-icon-office-building"></i></div>
        <h1 class="brand-title">实验室管理系统</h1>
        <div class="brand-line"></div>
        <p class="brand-desc">预约管理 · 设备台账 · 耗材追踪</p>
      </div>
      <div class="feature-list">
        <div class="feature-item">
          <div class="feature-badge"><i class="el-icon-date"></i></div>
          <div class="feature-text"><div class="feature-title">在线预约实验室</div><div class="feature-sub">一键锁定实验时段</div></div>
        </div>
        <div class="feature-item">
          <div class="feature-badge"><i class="el-icon-monitor"></i></div>
          <div class="feature-text"><div class="feature-title">设备全生命周期管理</div><div class="feature-sub">采购 · 维保 · 报废追踪</div></div>
        </div>
        <div class="feature-item">
          <div class="feature-badge"><i class="el-icon-box"></i></div>
          <div class="feature-text"><div class="feature-title">耗材库存智能预警</div><div class="feature-sub">低于阈值自动提醒</div></div>
        </div>
        <div class="feature-item">
          <div class="feature-badge"><i class="el-icon-data-analysis"></i></div>
          <div class="feature-text"><div class="feature-title">使用率数据统计</div><div class="feature-sub">可视化图表一图看懂</div></div>
        </div>
      </div>
    </div>
    <div class="login-right">
      <el-card class="login-card" shadow="hover">
        <h2 class="card-title">账号登录</h2>
        <p class="card-welcome">欢迎使用实验室管理系统</p>
        <el-form :model="form" :rules="rules" ref="f" @submit.native.prevent="handle">
          <el-form-item prop="username">
            <el-input v-model="form.username" placeholder="请输入账号" prefix-icon="el-icon-user" size="large" />
          </el-form-item>
          <el-form-item prop="password">
            <el-input show-password v-model="form.password" placeholder="请输入密码" prefix-icon="el-icon-lock" size="large" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" class="login-btn" :loading="loading" @click="handle">登 录</el-button>
          </el-form-item>
        </el-form>
        <div class="demo-tip">
          <div class="demo-row demo-row-label">
            <span class="demo-label">演示账号</span>
            <span class="demo-tags">
              <el-tag size="mini" effect="plain">admin</el-tag>
              <el-tag size="mini" effect="plain">labadmin</el-tag>
              <el-tag size="mini" effect="plain">teacher</el-tag>
              <el-tag size="mini" effect="plain">student</el-tag>
            </span>
          </div>
          <div class="demo-row demo-row-password">
            <span class="demo-label">默认密码</span>
            <span class="demo-value">123456</span>
          </div>
        </div>
        <div class="register-link">
          <span>没有账号？</span>
          <el-link type="primary" @click="$router.push('/register')">立即注册</el-link>
        </div>
      </el-card>
      <div class="copyright">© 2026 实验室管理系统 · v1.0.0</div>
    </div>
  </div>
</template>

<script>
export default {
  data() {
    return {
      form: { username: 'admin', password: '123456' },
      loading: false,
      rules: {
        username: [{ required: true, message: '请输入账号' }],
        password: [{ required: true, message: '请输入密码' }, { min: 6, message: '密码至少6位' }]
      }
    }
  },
  methods: {
    handle() {
      this.$refs.f.validate(async valid => {
        if (!valid) return
        this.loading = true
        try {
          await this.$store.dispatch('user/logout')
          const tokenRes = await this.$store.dispatch('user/login', this.form)
          const token = (tokenRes && tokenRes.token) || ''
          await this.$store.dispatch('user/fetchInfo')
          await this.$store.dispatch('permission/generateRoutes')
          if (token) {
            try {
              const ws = await import('@/utils/websocket')
              ws.init(token)
            } catch (e) {}
          }
          const roles = this.$store.state.user.roles || []
          const { defaultRouteForRole } = await import('@/utils/role-map')
          const redirect = this.$route.query.redirect
          const target = (redirect && redirect !== '/login' && redirect !== '/')
            ? redirect
            : (defaultRouteForRole(roles) || '/dashboard')
          window.location.replace(target)
        } catch (e) {
          const msg = (e && e.message) || (e && e.data && e.data.message) || '登录失败，请检查账号密码'
          this.$message.error(msg)
        } finally {
          this.loading = false
        }
      })
    }
  }
}
</script>

<style scoped lang="scss">
.login-wrap {
  height: 100vh;
  display: flex;
  background: #f5f7fa;
  overflow: hidden;
}

.login-left {
  flex: 1;
  background: linear-gradient(135deg, #1e3a5f 0%, #2d5a87 50%, #3a7ca5 100%);
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 60px 80px;
  color: #fff;
  position: relative;
  overflow: hidden;
}

.grid-bg {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(255,255,255,0.06) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255,255,255,0.06) 1px, transparent 1px);
  background-size: 40px 40px;
  animation: gridMove 18s linear infinite;
  mask-image: radial-gradient(ellipse at center, rgba(0,0,0,0.9) 0%, rgba(0,0,0,0.2) 70%, transparent 100%);
  -webkit-mask-image: radial-gradient(ellipse at center, rgba(0,0,0,0.9) 0%, rgba(0,0,0,0.2) 70%, transparent 100%);
  pointer-events: none;
}

@keyframes gridMove {
  0%   { background-position: 0 0, 0 0; }
  100% { background-position: 40px 40px, 40px 40px; }
}

.glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.45;
  pointer-events: none;
  animation: glowFloat 8s ease-in-out infinite;
}
.glow-1 {
  width: 380px; height: 380px;
  background: #4A9EFF;
  top: -120px; left: -120px;
}
.glow-2 {
  width: 420px; height: 420px;
  background: #67C2FF;
  bottom: -160px; right: -100px;
  animation-delay: -4s;
}
@keyframes glowFloat {
  0%, 100% { transform: translate(0, 0) scale(1); opacity: 0.35; }
  50%      { transform: translate(20px, -30px) scale(1.08); opacity: 0.5; }
}

.brand-area {
  margin-bottom: 50px;
  position: relative;
  z-index: 1;
}

.brand-icon {
  width: 80px;
  height: 80px;
  background: rgba(255,255,255,0.18);
  border: 1px solid rgba(255,255,255,0.25);
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 24px;
  backdrop-filter: blur(10px);
  box-shadow: 0 8px 24px rgba(0,0,0,0.15);
  i { font-size: 40px; }
}

.brand-title {
  margin: 0;
  font-size: 36px;
  font-weight: 700;
  letter-spacing: 1px;
}

.brand-line {
  width: 72px;
  height: 4px;
  margin-top: 16px;
  border-radius: 4px;
  background: linear-gradient(90deg, #67C2FF, #4A9EFF, transparent);
}

.brand-desc {
  margin: 16px 0 0;
  font-size: 15px;
  opacity: 0.85;
  letter-spacing: 0.5px;
}

.feature-list {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 22px;
  position: relative;
  z-index: 1;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 16px;
  background: rgba(255,255,255,0.06);
  border: 1px solid rgba(255,255,255,0.1);
  border-radius: 12px;
  backdrop-filter: blur(8px);
  transition: transform .25s ease, background .25s ease, border-color .25s ease;
}
.feature-item:hover {
  transform: translateY(-2px);
  background: rgba(255,255,255,0.1);
  border-color: rgba(255,255,255,0.25);
}
.feature-badge {
  flex-shrink: 0;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #fff;
  color: #2d5a87;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
  i { font-size: 18px; }
}
.feature-text {
  min-width: 0;
}
.feature-title {
  font-size: 14px;
  font-weight: 600;
  color: #fff;
  margin-bottom: 2px;
}
.feature-sub {
  font-size: 12px;
  color: rgba(255,255,255,0.75);
}

.login-right {
  width: 520px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: #fff;
  padding: 40px 20px;
}

.login-card {
  width: 420px;
  border: 1px solid #EBEEF5;
  border-radius: 16px !important;
  box-shadow: 0 8px 32px rgba(30, 58, 95, 0.08) !important;
}

.card-title {
  margin: 0 0 4px;
  font-size: 24px;
  color: #303133;
  text-align: center;
  font-weight: 700;
}

.card-welcome {
  margin: 0 0 24px;
  text-align: center;
  font-size: 13px;
  color: #909399;
}

::v-deep .el-input--large .el-input__inner {
  height: 46px;
  border-radius: 8px;
  font-size: 14px;
}

.login-btn {
  width: 100%;
  height: 46px;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 4px;
  border-radius: 8px !important;
  border: none !important;
  background: linear-gradient(135deg, #2d5a87 0%, #409EFF 100%) !important;
  box-shadow: 0 4px 14px rgba(64, 158, 255, 0.35);
  transition: transform .2s ease, box-shadow .2s ease;
}
.login-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(64, 158, 255, 0.45);
}

.demo-tip {
  margin-top: 20px;
  padding: 16px 18px;
  background: #F8FAFD;
  border: 1px dashed #DCE4EF;
  border-radius: 10px;
}
.demo-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  font-size: 13px;
}
.demo-row + .demo-row { margin-top: 8px; }
.demo-label {
  color: #909399;
  flex-shrink: 0;
}
.demo-tags .el-tag { margin-left: 6px; }
.demo-value {
  color: #303133;
  font-family: monospace;
  font-weight: 600;
  background: #fff;
  padding: 2px 10px;
  border-radius: 4px;
  border: 1px solid #EBEEF5;
}

.register-link {
  margin-top: 16px;
  font-size: 14px;
  color: #909399;
  text-align: center;
}

.copyright {
  margin-top: 28px;
  font-size: 12px;
  color: #C0C4CC;
  letter-spacing: 0.5px;
}

@media (max-width: 900px) {
  .login-left { display: none; }
  .login-right { width: 100%; }
}
</style>
