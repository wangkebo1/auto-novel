import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import {
  ArrowDown,
  ArrowLeft,
  ArrowRight,
  ChatDotRound,
  ChatDotSquare,
  CircleCloseFilled,
  DataAnalysis,
  Delete,
  Document,
  Download,
  EditPen,
  FolderOpened,
  HomeFilled,
  Loading,
  MagicStick,
  Notebook,
  Plus,
  Promotion,
  Refresh,
  SwitchButton,
  Tools,
  Upload,
  UploadFilled,
  User,
  VideoCamera,
  VideoPause,
  VideoPlay,
} from '@element-plus/icons-vue'
import 'element-plus/dist/index.css'
import zhCn from 'element-plus/es/locale/lang/zh-cn'

import App from './App.vue'
import router from './router'

const app = createApp(App)

// 仅注册实际使用到的图标，避免全量图标进入首包
app.component('ArrowDown', ArrowDown)
app.component('ArrowLeft', ArrowLeft)
app.component('ArrowRight', ArrowRight)
app.component('ChatDotRound', ChatDotRound)
app.component('ChatDotSquare', ChatDotSquare)
app.component('CircleCloseFilled', CircleCloseFilled)
app.component('DataAnalysis', DataAnalysis)
app.component('Delete', Delete)
app.component('Document', Document)
app.component('Download', Download)
app.component('EditPen', EditPen)
app.component('FolderOpened', FolderOpened)
app.component('HomeFilled', HomeFilled)
app.component('Loading', Loading)
app.component('MagicStick', MagicStick)
app.component('Notebook', Notebook)
app.component('Plus', Plus)
app.component('Promotion', Promotion)
app.component('Refresh', Refresh)
app.component('SwitchButton', SwitchButton)
app.component('Tools', Tools)
app.component('Upload', Upload)
app.component('UploadFilled', UploadFilled)
app.component('User', User)
app.component('VideoCamera', VideoCamera)
app.component('VideoPause', VideoPause)
app.component('VideoPlay', VideoPlay)

app.use(createPinia())
app.use(router)
app.use(ElementPlus, { locale: zhCn })

app.mount('#app')
