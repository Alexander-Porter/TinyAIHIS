<template>
  <div class="doctor-login">
    <div class="login-box">
      <div class="logo">🏥</div>
      <h1>医生工作站</h1>
      <p>TinyHIS Hospital Information System</p>
      
      <el-form :model="form" :rules="rules" ref="formRef" @submit.prevent="onSubmit">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" prefix-icon="User" size="large" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" prefix-icon="Lock" size="large" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" native-type="submit" :loading="loading" size="large" style="width: 100%">
            登录
          </el-button>
        </el-form-item>
      </el-form>
      
      <div class="back-link">
        <a @click="$router.push('/')">返回首页</a>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { authApi } from '@/utils/api'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const onSubmit = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  
  loading.value = true
  try {
    const data = await authApi.staffLogin(form)
    
    if (!['DOCTOR', 'CHIEF'].includes(data.role)) {
      ElMessage.error('请使用医生账号登录')
      return
    }
    
    userStore.login(data)
    ElMessage.success('登录成功')
    router.push('/doctor/workstation')
  } catch (e) {
    console.error('Login failed', e)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.doctor-login {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
  
  .login-box {
    width: 400px;
    background: #fff;
    border-radius: 12px;
    padding: 40px;
    text-align: center;
    
    .logo {
      font-size: 64px;
      margin-bottom: 10px;
    }
    
    h1 {
      margin: 0 0 5px;
      font-size: 24px;
    }
    
    p {
      margin: 0 0 30px;
      color: #999;
      font-size: 14px;
    }
    
    .back-link {
      margin-top: 20px;
      
      a {
        color: #409eff;
        cursor: pointer;
      }
    }
  }
}
</style>
