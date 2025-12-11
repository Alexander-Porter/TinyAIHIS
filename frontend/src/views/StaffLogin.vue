<template>
  <div class="staff-login">
    <div class="login-box">
      <div class="logo"></div>
      <h1>TinyAIHIS 工作站登录</h1>
      
      <div v-if="isDemo" class="demo-selector">
        <a-alert message="演示模式已开启" type="info" show-icon style="margin-bottom: 16px" />
        <a-select
          v-model:value="selectedDemoUser"
          style="width: 100%; margin-bottom: 16px"
          placeholder="选择演示账号"
          @change="onDemoUserChange"
        >
          <a-select-option v-for="user in demoUsers" :key="user.username" :value="user.username">
            {{ user.realName }} ({{ user.role }})
          </a-select-option>
        </a-select>
      </div>

      <a-form :model="form" @finish="onSubmit">
        <a-form-item>
          <a-input v-model:value="form.username" placeholder="用户名" size="large">
            <template #prefix><UserOutlined /></template>
          </a-input>
        </a-form-item>
        <a-form-item v-if="!isDemo">
          <a-input-password v-model:value="form.password" placeholder="密码" size="large">
            <template #prefix><LockOutlined /></template>
          </a-input-password>
        </a-form-item>
        <a-form-item>
          <a-button type="primary" html-type="submit" :loading="loading" size="large" style="width: 100%">登录</a-button>
        </a-form-item>
      </a-form>
      <div class="back-link"><a @click="router.push('/')">返回首页</a></div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { UserOutlined, LockOutlined } from '@ant-design/icons-vue'
import { useUserStore } from '@/stores/user'
import { authApi } from '@/utils/api'
import axios from 'axios'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const form = reactive({ username: '', password: '' })

const isDemo = ref(false)
const demoUsers = ref([])
const selectedDemoUser = ref(undefined)

onMounted(async () => {
  try {
    const res = await axios.get('/api/auth/demo-info')
    if (res.data.code === 200) {
      const data = res.data.data
      isDemo.value = data.isDemo
      if (data.isDemo) {
        demoUsers.value = data.staff
      }
    }
  } catch (e) {
    console.error('Failed to fetch demo info', e)
  }
})

const onDemoUserChange = (username) => {
  const user = demoUsers.value.find(u => u.username === username)
  if (user) {
    form.username = user.username
    form.password = 'demo' // Dummy password for demo mode
  }
}

const onSubmit = async () => {
  loading.value = true
  try {
    // In demo mode, if password is empty, set a dummy password to pass backend validation
    if (isDemo.value && !form.password) {
      form.password = 'demo'
    }
    const data = await authApi.staffLogin(form)
    userStore.login(data)
    
    switch (data.role) {
      case 'DOCTOR':
      case 'CHIEF':
        router.push('/doctor/workstation')
        break
      case 'LAB':
        router.push('/lab/workstation')
        break
      case 'PHARMACY':
        router.push('/pharmacy/dispense')
        break
      case 'ADMIN':
        router.push('/admin/dashboard')
        break
      default:
        message.error('未知角色')
    }
  } catch (e) { 
    console.error(e) 
  } finally { 
    loading.value = false 
  }
}
</script>

<style scoped lang="scss">
.staff-login {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f0f2f5 0%, #e6ffed 100%);
  
  .login-box {
    width: 400px;
    padding: 40px;
    background: #fff;
    border-radius: 8px;
    box-shadow: 0 4px 12px rgba(0,0,0,0.1);
    text-align: center;
    
    .logo { font-size: 48px; margin-bottom: 16px; }
    h1 { margin-bottom: 32px; color: #1f1f1f; font-size: 24px; }
    .back-link { margin-top: 16px; }
  }
}
</style>
