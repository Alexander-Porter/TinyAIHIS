<template>
  <div class="patient-home">
    <!-- Header with Patient Card -->
    <div class="header-card" v-if="userStore.isLoggedIn">
      <div class="patient-info">
        <div class="avatar">
          <van-icon name="user-o" size="40" />
        </div>
        <div class="info">
          <div class="name">就诊人：{{ userStore.userInfo.realName || userStore.userInfo.name }}</div>
          <div class="card-no">就诊卡号：{{ userStore.userInfo.patientId || userStore.userInfo.userId }}</div>
        </div>
      </div>
      <div class="qr-section" @click="logout">
        <van-icon name="setting-o" size="24" />
        <span>退出</span>
      </div>
    </div>
    <div class="header-card login-prompt" v-else>
      <div class="patient-info">
        <div class="avatar">
          <van-icon name="user-o" size="40" />
        </div>
        <div class="info">
          <div class="name">欢迎使用 TinyAIHIS</div>
          <div class="card-no">请登录后使用完整功能</div>
        </div>
      </div>
      <van-button size="small" type="primary" @click="$router.push('/patient/login')">登录</van-button>
    </div>
    
    <!-- Notification Banners -->
    <div class="notification-banners" v-if="userStore.isLoggedIn">
      <!-- 可签到的号 Banner -->
      <div class="notify-banner checkin-banner" v-if="checkInableRegs.length > 0" @click="showCheckInModal">
        <div class="banner-icon">
          <van-icon name="clock-o" />
        </div>
        <div class="banner-content">
          <div class="title">您有 {{ checkInableRegs.length }} 个预约可以签到</div>
          <div class="sub">点击签到进入候诊队列</div>
        </div>
        <van-icon name="arrow" class="arrow" />
      </div>
      
      <!-- 候诊中的号 Banner -->
      <div class="notify-banner queue-banner" v-if="waitingRegs.length > 0" @click="showQueueDetail">
        <div class="banner-icon">
          <van-icon name="friends-o" />
        </div>
        <div class="banner-content">
          <div class="title">
            <span v-if="waitingRegs[0].hasPendingLabs" style="color: #faad14">
              <van-icon name="todo-list-o" /> 请前往检查/缴费
            </span>
            <span v-else>
              {{ waitingRegs.length > 1 ? `${waitingRegs.length}个号正在候诊` : `${waitingRegs[0].deptName || '门诊'} 候诊中` }}
            </span>
          </div>
          <div class="sub" v-if="waitingRegs.length === 1">
            <span v-if="waitingRegs[0].hasPendingLabs">医生已开具检查单，请完成后回诊室</span>
            <span v-else>排队号 {{ waitingRegs[0].queueNumber }}，请耐心等候</span>
          </div>
          <div class="sub" v-else>点击查看详情</div>
        </div>
        <van-icon name="arrow" class="arrow" />
      </div>
    </div>
    
    <!-- Quick Actions Section -->
    <div class="section-card">
      <div class="section-title">门诊服务</div>
      <div class="quick-actions">
        <div class="action-item" @click="goAppointment">
          <div class="icon-wrapper purple"><van-icon name="friends-o" /></div>
          <div class="text">预约挂号</div>
        </div>
        <div class="action-item" @click="goPayment">
          <div class="icon-wrapper red"><van-icon name="gold-coin-o" /></div>
          <div class="text">门诊缴费</div>
        </div>
        <div class="action-item" @click="goRegistrationRecords">
          <div class="icon-wrapper blue"><van-icon name="notes-o" /></div>
          <div class="text">挂号记录</div>
        </div>
        <div class="action-item" @click="goRecords">
          <div class="icon-wrapper pink"><van-icon name="orders-o" /></div>
          <div class="text">就诊记录</div>
        </div>
        <div class="action-item" @click="goReports">
          <div class="icon-wrapper green"><van-icon name="description" /></div>
          <div class="text">报告查询</div>
        </div>
        <div class="action-item" @click="goTriage">
          <div class="icon-wrapper cyan"><van-icon name="service-o" /></div>
          <div class="text">智能导诊</div>
        </div>
      </div>
    </div>
    

    
    <!-- CheckIn Modal -->
    <van-action-sheet v-model:show="checkInModalVisible" title="选择要签到的预约">
      <div class="checkin-list">
        <div class="checkin-item" v-for="reg in checkInableRegs" :key="reg.regId">
          <div class="info">
            <div class="dept">{{ reg.deptName || '未知科室' }}</div>
            <div class="doctor">{{ reg.doctorName }} · {{ formatShift(reg.shift) }}</div>
            <div class="time">{{ reg.scheduleDate }}</div>
          </div>
          <van-button size="small" type="primary" :loading="checkingIn === reg.regId" @click="doCheckIn(reg)">
            签到
          </van-button>
        </div>
        <van-empty v-if="checkInableRegs.length === 0" description="暂无可签到的预约" />
      </div>
    </van-action-sheet>
    
    <!-- Queue Detail Modal -->
    <van-action-sheet v-model:show="queueModalVisible" title="我的排队">
      <div class="queue-list">
        <div class="queue-item" v-for="reg in waitingRegs" :key="reg.regId">
          <div class="info">
            <div class="dept">{{ reg.deptName || '未知科室' }}</div>
            <div class="doctor">{{ reg.doctorName }} · {{ formatShift(reg.shift) }}</div>
          </div>
          <div class="queue-number">
            <div class="label">排队号</div>
            <div class="number">{{ reg.queueNumber || '-' }}</div>
          </div>
        </div>
        <van-empty v-if="waitingRegs.length === 0" description="暂无排队中的号" />
      </div>
    </van-action-sheet>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Button as VanButton, Icon as VanIcon, showToast, ActionSheet as VanActionSheet, Empty as VanEmpty } from 'vant'
import { useUserStore } from '@/stores/user'
import { registrationApi } from '@/utils/api'

const router = useRouter()
const userStore = useUserStore()
const registrations = ref([])
const checkInModalVisible = ref(false)
const queueModalVisible = ref(false)
const checkingIn = ref(null)

// 可签到的预约（状态=1待签到，且时间符合，急诊不需要签到）
const checkInableRegs = computed(() => {
  return registrations.value.filter(reg => {
    if (reg.status !== 1) return false
    // 急诊不需要签到，不显示在签到列表中
    if (isEmergency(reg)) return false
    return canCheckIn(reg)
  })
})

// 判断是否是急诊
const isEmergency = (reg) => {
  return reg.shift === '急诊(全天)' || reg.shift === 'ER' || reg.shift === '急诊'
}

// 候诊中的号（状态=2候诊中 或 状态=3就诊中，急诊状态1也算候诊中）
const waitingRegs = computed(() => {
  return registrations.value.filter(reg => {
    if (reg.status === 2 || reg.status === 3) return true
    // 急诊已缴费(状态1)也视为候诊中
    if (reg.status === 1 && isEmergency(reg)) return true
    return false
  })
})

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

// 判断是否可以签到
const canCheckIn = (reg) => {
  if (!reg.scheduleDate) return true
  
  const now = new Date()
  const scheduleDate = new Date(reg.scheduleDate)
  
  // 急诊全天可签到
  if (reg.shift === '急诊(全天)' || reg.shift === 'ER') {
    // 只要是今天就可以
    const today = new Date()
    today.setHours(0,0,0,0)
    scheduleDate.setHours(0,0,0,0)
    return scheduleDate.getTime() === today.getTime()
  }
  
  // 普通门诊
  if (reg.shift === '上午' || reg.shift === 'AM') {
    scheduleDate.setHours(8, 0, 0, 0)
  } else {
    scheduleDate.setHours(14, 0, 0, 0)
  }
  
  const diffMinutes = (scheduleDate - now) / (1000 * 60)
  
  // 已过时的号也允许签到
  if (diffMinutes <= 0) return true
  
  // 就诊开始时间前30分钟内可以签到
  return diffMinutes <= 30
}

const checkLogin = () => {
  if (!userStore.isLoggedIn) {
    showToast('请先登录')
    router.push('/patient/login')
    return false
  }
  return true
}

// 格式化班次显示
const formatShift = (shift) => {
  if (shift === 'AM' || shift === '上午') return '上午'
  if (shift === 'PM' || shift === '下午') return '下午'
  if (shift === 'ER' || shift === '急诊(全天)' || shift === '急诊') return '急诊'
  return shift || ''
}

const goTriage = () => router.push('/patient/triage')
const goAppointment = () => checkLogin() && router.push('/patient/appointment')
const goPayment = () => checkLogin() && router.push('/patient/payment')
const goReports = () => checkLogin() && router.push('/patient/reports')
const goRecords = () => checkLogin() && router.push('/patient/records')
const goRegistrationRecords = () => checkLogin() && router.push('/patient/registration-records')

const logout = () => {
  userStore.logout()
  router.push('/patient/login')
}

const showCheckInModal = () => {
  checkInModalVisible.value = true
}

const showQueueDetail = () => {
  if (!checkLogin()) return
  queueModalVisible.value = true
}


const doCheckIn = async (reg) => {
  checkingIn.value = reg.regId
  
  try {
    await registrationApi.checkIn({ regId: reg.regId })
    showToast('签到成功，请前往科室候诊')
    checkInModalVisible.value = false
    loadRegistrations()
  } catch (e) {
    console.error('Check-in failed', e)
  } finally {
    checkingIn.value = null
  }
}
</script>

<style scoped lang="scss">
.patient-home {
  min-height: 100vh;
  background: linear-gradient(180deg, #4A90D9 0%, #4A90D9 180px, #f5f6f7 180px);
  padding-bottom: 60px;

  .header-card {
    margin: 16px;
    padding: 20px;
    background: #fff;
    border-radius: 12px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    box-shadow: 0 2px 12px rgba(0,0,0,0.08);
    
    .patient-info {
      display: flex;
      align-items: center;
      gap: 12px;
      
      .avatar {
        width: 60px;
        height: 60px;
        border-radius: 50%;
        background: #f0f0f0;
        display: flex;
        align-items: center;
        justify-content: center;
        color: #999;
      }
      
      .info {
        .name {
          font-size: 16px;
          font-weight: 600;
          color: #333;
          margin-bottom: 4px;
        }
        .card-no {
          font-size: 13px;
          color: #999;
        }
      }
    }
    
    .qr-section {
      display: flex;
      flex-direction: column;
      align-items: center;
      color: #4A90D9;
      font-size: 12px;
      cursor: pointer;
    }
    
    &.login-prompt {
      .info .name {
        color: #666;
      }
    }
  }
  
  .notification-banners {
    padding: 0 16px;
    margin-bottom: 16px;
    
    .notify-banner {
      display: flex;
      align-items: center;
      padding: 14px 16px;
      background: #fff;
      border-radius: 10px;
      margin-bottom: 10px;
      box-shadow: 0 2px 8px rgba(0,0,0,0.06);
      cursor: pointer;
      
      .banner-icon {
        width: 40px;
        height: 40px;
        border-radius: 10px;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 20px;
        margin-right: 12px;
      }
      
      .banner-content {
        flex: 1;
        
        .title {
          font-size: 15px;
          font-weight: 600;
          color: #333;
          margin-bottom: 2px;
        }
        .sub {
          font-size: 12px;
          color: #999;
        }
      }
      
      .arrow {
        color: #ccc;
      }
      
      &.checkin-banner .banner-icon {
        background: rgba(255, 153, 0, 0.1);
        color: #ff9900;
      }
      
      &.queue-banner .banner-icon {
        background: rgba(74, 144, 217, 0.1);
        color: #4A90D9;
      }
    }
  }
  
  .section-card {
    margin: 0 16px 16px;
    padding: 16px;
    background: #fff;
    border-radius: 12px;
    box-shadow: 0 2px 8px rgba(0,0,0,0.06);
    
    .section-title {
      font-size: 16px;
      font-weight: 600;
      color: #333;
      margin-bottom: 16px;
    }
  }
  
  .quick-actions {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 16px;
    
    &.small {
      grid-template-columns: repeat(4, 1fr);
    }
    
    .action-item {
      display: flex;
      flex-direction: column;
      align-items: center;
      cursor: pointer;
      
      .icon-wrapper {
        width: 50px;
        height: 50px;
        border-radius: 14px;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 24px;
        margin-bottom: 8px;
        
        &.purple {
          background: linear-gradient(135deg, #a78bfa, #8b5cf6);
          color: #fff;
        }
        &.red {
          background: linear-gradient(135deg, #f87171, #ef4444);
          color: #fff;
        }
        &.blue {
          background: linear-gradient(135deg, #60a5fa, #3b82f6);
          color: #fff;
        }
        &.pink {
          background: linear-gradient(135deg, #f472b6, #ec4899);
          color: #fff;
        }
        &.green {
          background: linear-gradient(135deg, #4ade80, #22c55e);
          color: #fff;
        }
        &.cyan {
          background: linear-gradient(135deg, #22d3ee, #06b6d4);
          color: #fff;
        }
      }
      
      .text {
        font-size: 12px;
        color: #666;
        text-align: center;
      }
    }
  }
}

// Action Sheet Content Styles
.checkin-list, .queue-list {
  padding: 16px;
  max-height: 60vh;
  overflow-y: auto;
}

.checkin-item, .queue-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 10px;
  margin-bottom: 12px;
  
  .info {
    .dept {
      font-size: 15px;
      font-weight: 600;
      color: #333;
      margin-bottom: 4px;
    }
    .doctor {
      font-size: 13px;
      color: #666;
      margin-bottom: 2px;
    }
    .time {
      font-size: 12px;
      color: #999;
    }
  }
}

.queue-item {
  .queue-number {
    text-align: center;
    
    .label {
      font-size: 11px;
      color: #999;
      margin-bottom: 4px;
    }
    .number {
      font-size: 28px;
      font-weight: 700;
      color: #4A90D9;
    }
  }
}
</style>
