<template>
  <div class="register-page">
    <van-nav-bar title="患者注册" left-arrow @click-left="$router.back()" />
    
    <div class="register-form">
      <van-form @submit="onSubmit">
        <van-cell-group inset>
          <van-field
            v-model="form.name"
            name="name"
            label="姓名"
            placeholder="请输入真实姓名"
            :rules="[{ required: true, message: '请输入姓名' }]"
          />
          <van-field
            v-model="form.phone"
            name="phone"
            label="手机号"
            placeholder="请输入手机号"
            :rules="[{ required: true, message: '请输入手机号' }]"
          />
          <van-field
            v-model="form.idCard"
            name="idCard"
            label="身份证号"
            placeholder="请输入身份证号（选填）"
          />
          <van-field
            v-model="form.password"
            type="password"
            name="password"
            label="密码"
            placeholder="请设置登录密码"
            :rules="[{ required: true, message: '请设置密码' }]"
          />
          <van-field name="gender" label="性别">
            <template #input>
              <van-radio-group v-model="form.gender" direction="horizontal">
                <van-radio :name="1">男</van-radio>
                <van-radio :name="0">女</van-radio>
              </van-radio-group>
            </template>
          </van-field>
          <van-field
            v-model="form.age"
            type="digit"
            name="age"
            label="年龄"
            placeholder="请输入年龄"
          />
        </van-cell-group>
        
        <div class="submit-btn">
          <van-button block type="primary" native-type="submit" :loading="loading">
            注册
          </van-button>
        </div>
      </van-form>
      
      <div class="login-link">
        已有账号？<a @click="$router.push('/patient/login')">立即登录</a>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { NavBar as VanNavBar, Form as VanForm, Field as VanField, CellGroup as VanCellGroup, Button as VanButton, RadioGroup as VanRadioGroup, Radio as VanRadio, showToast } from 'vant'
import { authApi } from '@/utils/api'

const router = useRouter()
const loading = ref(false)

const form = ref({
  name: '',
  phone: '',
  idCard: '',
  password: '',
  gender: 1,
  age: null
})

const onSubmit = async () => {
  loading.value = true
  try {
    await authApi.patientRegister(form.value)
    showToast('注册成功，请登录')
    router.push('/patient/login')
  } catch (e) {
    console.error('Register failed', e)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.register-page {
  min-height: 100vh;
  background: #f5f7fa;
  
  .register-form {
    padding: 20px;
    
    .submit-btn {
      margin: 30px 16px;
    }
    
    .login-link {
      text-align: center;
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
