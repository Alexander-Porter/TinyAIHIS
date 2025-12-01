<template>
  <div class="admin-login">
    <div class="login-box">
      <div class="logo">⚙️</div>
      <h1>管理后台</h1>
      <el-form :model="form" ref="formRef" @submit.prevent="onSubmit">
        <el-form-item><el-input v-model="form.username" placeholder="用户名" prefix-icon="User" size="large" /></el-form-item>
        <el-form-item><el-input v-model="form.password" type="password" placeholder="密码" prefix-icon="Lock" size="large" show-password /></el-form-item>
        <el-form-item><el-button type="primary" native-type="submit" :loading="loading" size="large" style="width: 100%">登录</el-button></el-form-item>
      </el-form>
      <div class="back-link"><a @click="$router.push('/')">返回首页</a></div>
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
const loading = ref(false)
const form = reactive({ username: '', password: '' })

const onSubmit = async () => {
  loading.value = true
  try {
    const data = await authApi.staffLogin(form)
    if (data.role !== 'ADMIN') { ElMessage.error('请使用管理员账号登录'); return }
    userStore.login(data)
    router.push('/admin/dashboard')
  } catch (e) { console.error(e) } finally { loading.value = false }
}
</script>

<style scoped lang="scss">
.admin-login {
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
    .logo { font-size: 64px; margin-bottom: 10px; }
    h1 { margin: 0 0 30px; font-size: 24px; }
    .back-link { margin-top: 20px; a { color: #409eff; cursor: pointer; } }
  }
}
</style>
