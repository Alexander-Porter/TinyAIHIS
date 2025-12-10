<template>
  <div class="login-wrapper">
    <!-- Background Elements -->
    <div class="bg-shape shape-1"></div>
    <div class="bg-shape shape-2"></div>

    <div class="login-container">
      <div class="brand-section">
        <img src="@/assets/logo_with_slogan.png" alt="TinyHIS - Caring for Life" class="brand-logo" />
        <p class="brand-welcome">欢迎回来，即刻开启您的健康管理之旅</p>
      </div>

      <div class="login-card">
        <div class="card-header">
          <h3>患者登录</h3>
          <span class="subtitle">Patient Login</span>
        </div>

        <div v-if="isDemo" class="demo-mode-section">
          <div class="demo-selector-card" @click="showPicker = true">
            <div class="avatar-placeholder">
              <span>{{ selectedDemoUser ? selectedDemoUser[0] : '?' }}</span>
            </div>
            <div class="user-info">
              <div class="label">演示账号</div>
              <div class="value">{{ selectedDemoUser || '点击选择' }}</div>
            </div>
            <van-icon name="arrow" color="#999" />
          </div>
          <p class="demo-tip">演示模式下无需输入密码</p>
        </div>

        <van-form @submit="onSubmit" class="login-form">
          <div class="input-group" v-if="!isDemo">
            <div class="input-label">手机号</div>
            <van-field v-model="form.username" name="username" placeholder="请输入您的手机号"
              :rules="[{ required: true, message: '请填写手机号' }]" class="custom-input" :border="false">
              <template #left-icon>
                <i class="icon-phone">📱</i>
              </template>
            </van-field>
          </div>

          <div class="input-group" v-if="!isDemo">
            <div class="input-label">密码</div>
            <van-field v-model="form.password" type="password" name="password" placeholder="请输入登录密码"
              :rules="[{ required: true, message: '请填写密码' }]" class="custom-input" :border="false">
              <template #left-icon>
                <i class="icon-lock">🔒</i>
              </template>
            </van-field>
          </div>

          <div class="action-area">
            <van-button block type="primary" native-type="submit" :loading="loading" class="login-btn">
              {{ isDemo ? '一键登录' : '登 录' }}
            </van-button>
          </div>

          <!-- Role Switcher -->
          <div class="role-switcher">
            <span @click="goToStaffLogin">工作人员入口</span>
            <span class="divider">|</span>
            <span @click="showScreenSelector = true">大屏展示</span>
          </div>
        </van-form>

        <div class="card-footer" v-if="!isDemo">
          <span>还没有账号？</span>
          <a class="text-link" @click="$router.push('/patient/register')">立即注册</a>
        </div>
      </div>
    </div>

    <!-- Demo Picker Popup -->
    <van-popup v-model:show="showPicker" round position="bottom">
      <van-picker title="选择演示账号" :columns="demoUserColumns" @cancel="showPicker = false" @confirm="onDemoUserConfirm" />
    </van-popup>

    <!-- Screen Selector Popup -->
    <van-popup v-model:show="showScreenSelector" round position="bottom" :style="{ height: '40%' }">
      <div class="screen-selector-content">
        <div class="popup-title">选择候诊科室</div>
        <div class="dept-grid">
          <div v-for="dept in departments" :key="dept.deptId" class="dept-item"
            @click="router.push(`/screen/${dept.deptId}`)">
            {{ dept.deptName }}
          </div>
          <div v-if="departments.length === 0" class="empty-tip">
            暂无科室数据
          </div>
        </div>
      </div>
    </van-popup>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { Form as VanForm, Field as VanField, Button as VanButton, showToast, Popup as VanPopup, Picker as VanPicker } from 'vant'
import { useUserStore } from '@/stores/user'
import { authApi } from '@/utils/api'
import axios from 'axios'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)

const form = ref({
  username: '',
  password: ''
})

const isDemo = ref(false)
const demoUsers = ref([])
const selectedDemoUser = ref('')
const showPicker = ref(false)

const demoUserColumns = computed(() => {
  return demoUsers.value.map(u => ({ text: `${u.name} (${u.phone})`, value: u.phone, user: u }))
})

onMounted(async () => {
  try {
    const res = await axios.get('/api/auth/demo-info')
    if (res.data.code === 200) {
      const data = res.data.data
      isDemo.value = data.isDemo
      if (data.isDemo) {
        demoUsers.value = data.patients
      }
    }
  } catch (e) {
    console.error('Failed to fetch demo info', e)
  }
})

const onDemoUserConfirm = ({ selectedOptions }) => {
  const user = selectedOptions[0].user
  if (user) {
    selectedDemoUser.value = `${user.name}`
    form.value.username = user.phone
    form.value.password = 'demo' // Dummy password for demo mode
    showPicker.value = false
  }
}

const onSubmit = async () => {
  loading.value = true
  try {
    if (isDemo.value) {
      if (!selectedDemoUser.value) {
        showToast('请先选择演示账号')
        loading.value = false
        return
      }
      form.value.password = 'demo'
    }
    const data = await authApi.patientLogin(form.value)
    userStore.login({ ...data, patientId: data.userId })
    showToast({
      message: '欢迎回来',
      icon: 'smile-o',
    })
    router.push('/patient/home')
  } catch (e) {
    console.error('Login failed', e)
  } finally {
    loading.value = false
  }
}

// Role Switching & Screen Logic
const showScreenSelector = ref(false)
const selectedDept = ref(undefined)
const departments = ref([])

onMounted(async () => {
  // ... existing demo info fetch ...
  try {
    const res = await axios.get('/api/auth/demo-info')
    if (res.data.code === 200) {
      const data = res.data.data
      isDemo.value = data.isDemo
      if (data.isDemo) {
        demoUsers.value = data.patients
      }
    }
  } catch (e) { /* ... */ }

  // Fetch departments for screen selector
  try {
    const deptRes = await axios.get('/api/schedule/departments') // Assuming this endpoint exists or similar
    if (deptRes.data.code === 200) {
      departments.value = deptRes.data.data
    }
  } catch (e) {
    // Fallback or retry
  }
})

const openScreen = () => {
  if (selectedDept.value) {
    router.push(`/screen/${selectedDept.value}`)
  }
}

const goToStaffLogin = () => {
  router.push('/doctor/login')
}

</script>

<style scoped lang="scss">
.login-wrapper {
  min-height: 100vh;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  padding: 20px;

  /* Abstract background shapes */
  .bg-shape {
    position: absolute;
    border-radius: 50%;
    filter: blur(80px);
    opacity: 0.6;
    z-index: 0;
  }

  .shape-1 {
    width: 300px;
    height: 300px;
    background: #a1c4fd;
    top: -50px;
    left: -50px;
    animation: float 10s infinite ease-in-out;
  }

  .shape-2 {
    width: 400px;
    height: 400px;
    background: #c2e9fb;
    bottom: -100px;
    right: -100px;
    animation: float 12s infinite ease-in-out reverse;
  }
}

.login-container {
  width: 100%;
  max-width: 440px;
  z-index: 10;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 24px;
}

.brand-section {
  text-align: center;
  animation: fadeInDown 0.8s ease-out;

  .brand-logo {
    height: 80px;
    /* Adjust based on actual logo aspect ratio */
    width: auto;
    margin-bottom: 12px;
    filter: drop-shadow(0 4px 12px rgba(0, 0, 0, 0.1));
  }

  .brand-welcome {
    color: #475569;
    font-size: 1.05rem;
    font-weight: 500;
    margin: 0;
    opacity: 0.9;
  }
}

.login-card {
  width: 100%;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(20px);
  border-radius: 24px;
  padding: 32px 28px;
  box-shadow: 0 20px 40px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.8);
  animation: fadeInUp 0.8s ease-out 0.2s backwards;

  .card-header {
    margin-bottom: 32px;
    text-align: left;

    h3 {
      font-size: 1.75rem;
      color: #1e293b;
      margin: 0 0 4px 0;
      font-weight: 700;
      letter-spacing: -0.025em;
    }

    .subtitle {
      font-size: 0.875rem;
      color: #94a3b8;
      font-weight: 500;
      text-transform: uppercase;
      letter-spacing: 0.05em;
    }
  }
}

.demo-tag {
  background: #f0f9ff;
  border: 1px dashed #0ea5e9;
  color: #0284c7;
  padding: 10px 14px;
  border-radius: 12px;
  margin-bottom: 24px;
  font-size: 0.9rem;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  transition: all 0.2s;

  &:hover {
    background: #e0f2fe;
    transform: translateY(-1px);
  }

  .tag-icon {
    font-size: 1.1rem;
  }
}

.input-group {
  margin-bottom: 20px;

  .input-label {
    font-size: 0.9rem;
    font-weight: 600;
    color: #475569;
    margin-bottom: 8px;
    margin-left: 4px;
  }
}

.custom-input {
  background: #f8fafc;
  border-radius: 16px;
  padding: 12px 16px;
  transition: all 0.3s ease;

  &:focus-within {
    background: #fff;
    box-shadow: 0 0 0 3px rgba(14, 165, 233, 0.15);
  }

  :deep(.van-field__control) {
    font-weight: 500;
    color: #1e293b;
  }

  .icon-phone,
  .icon-lock {
    font-style: normal;
    margin-right: 8px;
    font-size: 1.1rem;
    filter: grayscale(100%);
    opacity: 0.6;
  }
}

.login-btn {
  margin-top: 12px;
  height: 52px;
  border-radius: 16px;
  font-size: 1.1rem;
  font-weight: 600;
  letter-spacing: 0.05em;
  background: linear-gradient(135deg, #0ea5e9 0%, #2563eb 100%);
  border: none;
  box-shadow: 0 10px 20px -5px rgba(37, 99, 235, 0.4);
  transition: transform 0.2s, box-shadow 0.2s;

  &:active {
    transform: scale(0.98);
  }
}

.card-footer {
  margin-top: 28px;
  text-align: center;
  font-size: 0.95rem;
  color: #64748b;

  .text-link {
    color: #2563eb;
    font-weight: 600;
    cursor: pointer;
    margin-left: 4px;
    text-decoration: none;
    transition: color 0.2s;

    &:hover {
      color: #1d4ed8;
      text-decoration: underline;
    }
  }
}

@keyframes float {
  0% {
    transform: translate(0, 0);
  }

  50% {
    transform: translate(20px, 40px);
  }

  100% {
    transform: translate(0, 0);
  }
}

@keyframes fadeInDown {
  from {
    opacity: 0;
    transform: translateY(-20px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* Demo Mode Styles */
.demo-mode-section {
  margin-bottom: 24px;
}

.demo-selector-card {
  background: #f0f9ff;
  border: 1px solid #bae6fd;
  border-radius: 16px;
  padding: 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    background: #e0f2fe;
    transform: translateY(-2px);
  }

  .avatar-placeholder {
    width: 40px;
    height: 40px;
    background: #38bdf8;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    color: white;
    font-weight: bold;
    font-size: 1.1rem;
  }

  .user-info {
    flex: 1;

    .label {
      font-size: 0.75rem;
      color: #64748b;
      margin-bottom: 2px;
    }

    .value {
      font-size: 1rem;
      color: #0f172a;
      font-weight: 600;
    }
  }
}

.demo-tip {
  font-size: 0.8rem;
  color: #94a3b8;
  text-align: center;
  margin-top: 8px;
}

/* Role Switcher */
.role-switcher {
  margin-top: 24px;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 16px;
  font-size: 0.9rem;
  color: #64748b;

  span:not(.divider) {
    cursor: pointer;
    padding: 4px 8px;
    transition: color 0.2s;

    &:hover {
      color: #0ea5e9;
    }
  }

  .divider {
    color: #cbd5e1;
  }
}

/* Screen Selector Popup */
.screen-selector-content {
  padding: 24px;
  height: 100%;
  display: flex;
  flex-direction: column;

  .popup-title {
    font-size: 1.25rem;
    font-weight: 600;
    text-align: center;
    margin-bottom: 24px;
    color: #1e293b;
  }

  .dept-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 16px;
    overflow-y: auto;
    padding-bottom: 20px;

    .dept-item {
      background: #f8fafc;
      border: 1px solid #e2e8f0;
      padding: 16px;
      border-radius: 12px;
      text-align: center;
      font-weight: 500;
      color: #334155;
      cursor: pointer;
      transition: all 0.2s;

      &:hover {
        background: #e0f2fe;
        border-color: #38bdf8;
        color: #0284c7;
      }
    }
  }

  .empty-tip {
    grid-column: 1 / -1;
    text-align: center;
    color: #94a3b8;
    padding: 40px 0;
  }
}
</style>
