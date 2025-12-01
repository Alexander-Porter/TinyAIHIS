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
        <h2>智慧医疗</h2>
        <p>便捷就医，健康生活</p>
      </div>
    </div>
    
    <!-- Quick Actions -->
    <div class="quick-actions">
      <div class="action-item" @click="goTriage">
        <div class="icon-wrapper"><van-icon name="service" /></div>
        <div class="text">AI智能分诊</div>
      </div>
      <div class="action-item" @click="goAppointment">
        <div class="icon-wrapper"><van-icon name="calendar-o" /></div>
        <div class="text">预约挂号</div>
      </div>
      <div class="action-item" @click="goPayment">
        <div class="icon-wrapper"><van-icon name="card" /></div>
        <div class="text">门诊缴费</div>
      </div>
      <!-- Check-in removed -->
      <div class="action-item" @click="goReports">
        <div class="icon-wrapper"><van-icon name="description" /></div>
        <div class="text">检查报告</div>
      </div>
      <div class="action-item" @click="goRecords">
        <div class="icon-wrapper"><van-icon name="notes-o" /></div>
        <div class="text">就诊记录</div>
      </div>
    </div>
    
    <!-- Recent Registrations -->
    <div class="section" v-if="userStore.isLoggedIn && registrations.length > 0">
      <div class="section-title">近期就诊</div>
      <div class="reg-list">
        <div class="reg-item" v-for="reg in registrations" :key="reg.regId">
          <div class="info">
            <div class="dept">{{ reg.deptName || '未知科室' }}</div>
            <div class="status" :class="getStatusClass(reg.status)">{{ getStatusText(reg.status) }}</div>
          </div>
          <div class="actions">
            <div class="queue" v-if="reg.queueNumber && reg.status !== 5">
              排队号：{{ reg.queueNumber }}
            </div>
            <van-button 
              v-if="reg.status === 0 || reg.status === 1 || reg.status === 2" 
              size="small" 
              type="danger" 
              plain 
              @click.stop="cancelReg(reg)"
              style="margin-left: 10px"
            >取消</van-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Button as VanButton, Icon as VanIcon, showToast, showConfirmDialog } from 'vant'
import { useUserStore } from '@/stores/user'
import { registrationApi } from '@/utils/api'

const router = useRouter()
const userStore = useUserStore()
const registrations = ref([])

onMounted(() => {
  if (userStore.isLoggedIn) {
    loadRegistrations()
  }
})

const loadRegistrations = async () => {
  try {
    const patientId = userStore.userInfo.patientId || userStore.userInfo.userId
    registrations.value = await registrationApi.getByPatient(patientId)
  } catch (e) {
    console.error('Failed to load registrations', e)
  }
}

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
// const goCheckin = () => checkLogin() && router.push('/patient/checkin')
const goReports = () => checkLogin() && router.push('/patient/reports')
const goRecords = () => checkLogin() && router.push('/patient/records')

const logout = () => {
  userStore.logout()
  router.push('/patient/login')
}

const cancelReg = async (reg) => {
  try {
    await showConfirmDialog({
      title: '提示',
      message: '确定要取消该挂号吗？'
    })
    
    await registrationApi.cancel(reg.regId)
    showToast('取消成功')
    loadRegistrations()
  } catch (e) {
    // cancel
  }
}

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
  min-height: 100vh;
  background-color: var(--bg-body);
  padding-bottom: 60px;

  .header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 20px;
    background: var(--bg-surface);
    box-shadow: var(--shadow-sm);
    
    .greeting {
      font-size: 1.125rem;
      font-weight: 600;
      color: var(--text-primary);
    }
  }
  
  .banner {
    margin: 20px;
    height: 140px;
    background: var(--primary-color);
    border-radius: var(--radius-lg);
    display: flex;
    align-items: center;
    padding: 24px;
    color: #fff;
    box-shadow: var(--shadow-md);
    
    h2 {
      margin: 0 0 8px;
      font-size: 1.5rem;
      font-weight: 600;
    }
    
    p {
      margin: 0;
      opacity: 0.9;
      font-size: 0.9rem;
    }
  }
  
  .quick-actions {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 16px;
    padding: 0 20px;
    
    .action-item {
      background: var(--bg-surface);
      border-radius: var(--radius-md);
      padding: 24px 16px;
      text-align: center;
      cursor: pointer;
      box-shadow: var(--shadow-sm);
      transition: transform 0.2s;
      
      &:active {
        transform: scale(0.98);
      }
      
      .icon-wrapper {
        font-size: 28px;
        margin-bottom: 12px;
        color: var(--primary-color);
        display: flex;
        justify-content: center;
      }
      
      .text {
        font-size: 0.875rem;
        color: var(--text-secondary);
        font-weight: 500;
      }
    }
  }
  
  .section {
    margin: 24px 20px;
    
    .section-title {
      font-size: 1.125rem;
      font-weight: 600;
      margin-bottom: 16px;
      color: var(--text-primary);
    }
    
    .reg-list {
      .reg-item {
        background: var(--bg-surface);
        border-radius: var(--radius-md);
        padding: 16px;
        margin-bottom: 12px;
        display: flex;
        justify-content: space-between;
        align-items: center;
        box-shadow: var(--shadow-sm);
        
        .info {
          .dept {
            font-weight: 600;
            color: var(--text-primary);
            margin-bottom: 4px;
          }
          .status {
            font-size: 0.75rem;
            
            &.warning { color: var(--warning-color); }
            &.info { color: var(--text-tertiary); }
            &.primary { color: var(--primary-color); }
            &.success { color: var(--success-color); }
            &.danger { color: var(--danger-color); }
          }
        }
        
        .queue {
          font-size: 1rem;
          font-weight: 600;
          color: var(--accent-color);
        }
      }
    }
  }
}
</style>
