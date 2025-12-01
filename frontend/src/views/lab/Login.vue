<template>
  <div class="lab-login">
    <div class="login-box">
      <div class="logo"></div>
      <h1>检验科工作站</h1>
      <a-form :model="form" @finish="onSubmit">
        <a-form-item>
          <a-input v-model:value="form.username" placeholder="用户名" size="large">
            <template #prefix><UserOutlined /></template>
          </a-input>
        </a-form-item>
        <a-form-item>
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
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { UserOutlined, LockOutlined } from '@ant-design/icons-vue'
import { useUserStore } from '@/stores/user'
import { authApi } from '@/utils/api'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const form = reactive({ username: '', password: '' })

const onSubmit = async () => {
  loading.value = true
  try {
    const data = await authApi.staffLogin(form)
    if (data.role !== 'LAB') { message.error('请使用检验科账号登录'); return }
    userStore.login(data)
    router.push('/lab/workstation')
  } catch (e) { console.error(e) } finally { loading.value = false }
}
</script>

<style scoped lang="scss">
.lab-login {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f0f2f5 0%, #fff7e6 100%);
  
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
