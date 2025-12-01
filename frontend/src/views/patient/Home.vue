<template>
  <div class="patient-home">
    <!-- Header -->
    <div class="header">
      <div class="greeting">
        <span v-if="userStore.isLoggedIn">您好，{{ userStore.userInfo.realName || userStore.userInfo.name }}</span>
        <span v-else>欢迎使用 TinyHIS</span>
      </div>
      <div class="actions" v-if="!userStore.isLoggedIn">
        <van-button size="small" type="primary" @click="$router.push('/patient/login')">登录</van-button>
      </div>
      <div class="actions" v-else>
        <van-button size="small" plain @click="logout">退出</van-button>
      </div>
    </div>
    
    <!-- Banner -->
    <div class="banner">
      <div class="banner-content">
        <h2>🏥 智慧医疗</h2>
        <p>便捷就医，健康生活</p>
      </div>
    </div>
    
    <!-- Quick Actions -->
    <div class="quick-actions">
      <div class="action-item" @click="goTriage">
        <div class="icon">🤖</div>
        <div class="text">AI智能分诊</div>
      </div>
      <div class="action-item" @click="goAppointment">
        <div class="icon">📅</div>
        <div class="text">预约挂号</div>
      </div>
      <div class="action-item" @click="goPayment">
        <div class="icon">💳</div>
        <div class="text">门诊缴费</div>
      </div>
      <div class="action-item" @click="goCheckin">
        <div class="icon">📍</div>
        <div class="text">到院签到</div>
      </div>
      <div class="action-item" @click="goReports">
        <div class="icon">📋</div>
        <div class="text">检查报告</div>
      </div>
      <div class="action-item" @click="goRecords">
        <div class="icon">📝</div>
        <div class="text">就诊记录</div>
      </div>
    </div>
    
    <!-- Recent Registrations -->
    <div class="section" v-if="userStore.isLoggedIn && registrations.length > 0">
      <div class="section-title">近期就诊</div>
      <div class="reg-list">
        <div class="reg-item" v-for="reg in registrations" :key="reg.regId">
          <div class="info">
            <div class="dept">{{ getDeptName(reg.scheduleId) }}</div>
            <div class="status" :class="getStatusClass(reg.status)">{{ getStatusText(reg.status) }}</div>
          </div>
          <div class="queue" v-if="reg.queueNumber">
            排队号：{{ reg.queueNumber }}
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Button as VanButton, showToast } from 'vant'
import { useUserStore } from '@/stores/user'
import { registrationApi } from '@/utils/api'

const router = useRouter()
const userStore = useUserStore()
const registrations = ref([])

onMounted(async () => {
  if (userStore.isLoggedIn) {
    try {
      const patientId = userStore.userInfo.patientId || userStore.userInfo.userId
      registrations.value = await registrationApi.getByPatient(patientId)
    } catch (e) {
      console.error('Failed to load registrations', e)
    }
  }
})

const checkLogin = () => {
  if (!userStore.isLoggedIn) {
    showToast('请先登录')
    router.push('/patient/login')
    return false
  }
  return true
}

const goTriage = () => router.push('/patient/triage')
const goAppointment = () => checkLogin() && router.push('/patient/appointment')
const goPayment = () => checkLogin() && router.push('/patient/payment')
const goCheckin = () => checkLogin() && router.push('/patient/checkin')
const goReports = () => checkLogin() && router.push('/patient/reports')
const goRecords = () => checkLogin() && router.push('/patient/records')

const logout = () => {
  userStore.logout()
  router.push('/patient/login')
}

const getDeptName = (scheduleId) => '内科' // TODO: fetch from schedule
const getStatusText = (status) => {
  const map = { 0: '待缴费', 1: '待签到', 2: '候诊中', 3: '就诊中', 4: '已完成', 5: '已取消' }
  return map[status] || '未知'
}
const getStatusClass = (status) => {
  const map = { 0: 'warning', 1: 'info', 2: 'primary', 3: 'success', 4: 'default', 5: 'danger' }
  return map[status] || ''
}
</script>

<style scoped lang="scss">
.patient-home {
  .header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 15px;
    background: #fff;
    
    .greeting {
      font-size: 16px;
      font-weight: 500;
    }
  }
  
  .banner {
    margin: 15px;
    height: 120px;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    border-radius: 12px;
    display: flex;
    align-items: center;
    padding: 20px;
    color: #fff;
    
    h2 {
      margin: 0 0 8px;
      font-size: 24px;
    }
    
    p {
      margin: 0;
      opacity: 0.9;
    }
  }
  
  .quick-actions {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 15px;
    padding: 0 15px;
    
    .action-item {
      background: #fff;
      border-radius: 12px;
      padding: 20px;
      text-align: center;
      cursor: pointer;
      
      &:active {
        background: #f5f5f5;
      }
      
      .icon {
        font-size: 32px;
        margin-bottom: 8px;
      }
      
      .text {
        font-size: 13px;
        color: #333;
      }
    }
  }
  
  .section {
    margin: 20px 15px;
    
    .section-title {
      font-size: 16px;
      font-weight: 500;
      margin-bottom: 10px;
    }
    
    .reg-list {
      .reg-item {
        background: #fff;
        border-radius: 8px;
        padding: 15px;
        margin-bottom: 10px;
        display: flex;
        justify-content: space-between;
        align-items: center;
        
        .info {
          .dept {
            font-weight: 500;
          }
          .status {
            font-size: 12px;
            margin-top: 5px;
            
            &.warning { color: #e6a23c; }
            &.info { color: #909399; }
            &.primary { color: #409eff; }
            &.success { color: #67c23a; }
            &.danger { color: #f56c6c; }
          }
        }
        
        .queue {
          font-size: 14px;
          color: #409eff;
        }
      }
    }
  }
}
</style>
