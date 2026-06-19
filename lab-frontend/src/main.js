import Vue from 'vue'
import App from './App.vue'
import router from './router'
import store from './store'

import ElementUI from 'element-ui'
import 'element-ui/lib/theme-chalk/index.css'
import './assets/main.scss'

import './router/router-guards'
import './directives/permission'

// 注意：echarts 改为按需加载（utils/echarts.js），不再注册到 Vue.prototype，
// 以避免首屏 bundle 中包含完整的 echarts 代码。

Vue.use(ElementUI, { size: 'small' })
Vue.config.productionTip = false
Vue.config.devtools = false

new Vue({ router, store, render: h => h(App) }).$mount('#app')
