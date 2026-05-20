import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as Icons from '@element-plus/icons-vue'
import App from './App.vue'
import router from './router'
import './styles/global.css'

const app = createApp(App)

app.use(ElementPlus, { size: 'default' })
app.use(router)

for (const [key, component] of Object.entries(Icons)) {
  app.component(key, component)
}

app.mount('#app')