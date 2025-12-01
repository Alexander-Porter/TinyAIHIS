<template>
  <div class="login-page">
    <van-nav-bar title="患者登录" left-arrow @click-left="$router.back()" />
    
    <div class="login-form">
      <div class="logo">🏥</div>
      <h2>TinyHIS</h2>
      
      <van-form @submit="onSubmit">
        <van-cell-group inset>
          <van-field
            v-model="form.username"
            name="username"
            label="手机号"
            placeholder="请输入手机号"
            :rules="[{ required: true, message: '请输入手机号' }]"
          />
          <van-field
            v-model="form.password"
            type="password"
            name="password"
            label="密码"
            placeholder="请输入密码"
            :rules="[{ required: true, message: '请输入密码' }]"
          />
        </van-cell-group>
        
        <div class="submit-btn">
          <van-button block type="primary" native-type="submit" :loading="loading">
            登录
          </van-button>
        </div>
      </van-form>
      
      <div class="register-link">
        还没有账号？<a @click="$router.push('/patient/register')">立即注册</a>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { NavBar as VanNavBar, Form as VanForm, Field as VanField, CellGroup as VanCellGroup, Button as VanButton, showToast } from 'vant'
import { useUserStore } from '@/stores/user'
import { authApi } from '@/utils/api'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)

const form = ref({
  username: '',
  password: ''
})

const onSubmit = async () => {
  loading.value = true
  try {
    const data = await authApi.patientLogin(form.value)
    userStore.login({ ...data, patientId: data.userId })
    showToast('登录成功')
    router.push('/patient/home')
  } catch (e) {
    console.error('Login failed', e)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.login-page {
  min-height: 100vh;
  background: #f5f7fa;
  
  .login-form {
    padding: 40px 20px;
    text-align: center;
    
    .logo {
      font-size: 64px;
    }
    
    h2 {
      margin: 10px 0 30px;
      color: #333;
    }
    
    .submit-btn {
      margin: 30px 16px;
    }
    
    .register-link {
      margin-top: 20px;
      font-size: 14px;
      color: #666;
      
      a {
        color: #409eff;
        cursor: pointer;
      }
    }
  }
}
</style>
