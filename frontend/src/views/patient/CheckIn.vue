<template>
  <div class="checkin-page">
    <van-nav-bar title="到院签到" left-arrow @click-left="$router.back()" />
    
    <div class="content">
      <div class="info-section">
        <van-icon name="clock-o" size="48" color="var(--primary-color)" />
        <div class="tips">
          <p>签到规则</p>
          <ul>
            <li>就诊前 <strong>30分钟</strong> 内可签到</li>
            <li>半天内的号无需签到，直接候诊</li>
            <li>签到后请前往相应科室等候</li>
          </ul>
        </div>
      </div>
      
      <div class="reg-list">
        <div class="list-title">待签到挂号</div>
        <van-empty v-if="pendingRegs.length === 0" description="暂无待签到挂号" />
        
        <div class="reg-item" v-for="reg in pendingRegs" :key="reg.regId">
          <div class="info">
            <div class="dept">{{ reg.deptName || '未知科室' }}</div>
            <div class="doctor">{{ reg.doctorName || '未知医生' }} · {{ reg.shift || '' }}</div>
            <div class="time">{{ reg.scheduleDate }}</div>
          </div>
          <div class="action">
            <div class="queue">排队号: {{ reg.queueNumber }}</div>
            <van-button 
              size="small" 
              type="primary" 
              :disabled="!canCheckIn(reg)"
              :loading="checkingIn === reg.regId" 
              @click="doCheckIn(reg)">
              {{ getCheckInText(reg) }}
            </van-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { NavBar as VanNavBar, Button as VanButton, Empty as VanEmpty, Icon as VanIcon, showToast } from 'vant'
import { registrationApi } from '@/utils/api'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const pendingRegs = ref([])
const checkingIn = ref(null)

onMounted(() => {
  loadPendingRegs()
})

const loadPendingRegs = async () => {
  const patientId = userStore.userInfo.patientId || userStore.userInfo.userId
  
  try {
    const regs = await registrationApi.getByPatient(patientId)
    pendingRegs.value = regs.filter(r => r.status === 1) // Paid, waiting for check-in
  } catch (e) {
    console.error('Failed to load registrations', e)
  }
}

// 判断是否可以签到：就诊前30分钟内，或半天内的号
const canCheckIn = (reg) => {
  if (!reg.scheduleDate) return true // 如果没有日期信息，允许签到
  
  const now = new Date()
  const scheduleDate = new Date(reg.scheduleDate)
  
  // 设置预约时段的开始时间
  if (reg.shift === '上午' || reg.shift === 'AM') {
    scheduleDate.setHours(8, 0, 0, 0)
  } else {
    scheduleDate.setHours(14, 0, 0, 0)
  }
  
  const diffMinutes = (scheduleDate - now) / (1000 * 60)
  
  // 已过时的号也允许签到（可能迟到）
  if (diffMinutes <= 0) {
    return true
  }
  
  // 就诊前30分钟内可以签到
  return diffMinutes <= 30
}

const getCheckInText = (reg) => {
  if (!reg.scheduleDate) return '签到'
  
  const now = new Date()
  const scheduleDate = new Date(reg.scheduleDate)
  
  if (reg.shift === '上午' || reg.shift === 'AM') {
    scheduleDate.setHours(8, 0, 0, 0)
  } else {
    scheduleDate.setHours(14, 0, 0, 0)
  }
  
  const diffMinutes = (scheduleDate - now) / (1000 * 60)
  
  if (diffMinutes > 30) {
    const mins = Math.floor(diffMinutes - 30)
    if (mins >= 60) {
      return `${Math.floor(mins/60)}小时后`
    }
    return `${mins}分钟后`
  }
  
  return '签到'
}

const doCheckIn = async (reg) => {
  checkingIn.value = reg.regId
  
  try {
    await registrationApi.checkIn({ regId: reg.regId })
    showToast('签到成功，请前往科室候诊')
    
    // Remove from pending list
    const idx = pendingRegs.value.findIndex(r => r.regId === reg.regId)
    if (idx > -1) {
      pendingRegs.value.splice(idx, 1)
    }
  } catch (e) {
    console.error('Check-in failed', e)
  } finally {
    checkingIn.value = null
  }
}
</script>

<style scoped lang="scss">
.checkin-page {
  min-height: 100vh;
  background: var(--bg-body);
  
  .content {
    padding: 16px;
  }
  
  .info-section {
    background: var(--bg-surface);
    border-radius: var(--radius-lg);
    padding: 24px;
    text-align: center;
    margin-bottom: 16px;
    box-shadow: var(--shadow-sm);
    
    .tips {
      text-align: left;
      margin-top: 16px;
      color: var(--text-secondary);
      font-size: 14px;
      
      p {
        margin: 0 0 8px;
        font-weight: 600;
        color: var(--text-primary);
      }
      
      ul {
        margin: 0;
        padding-left: 20px;
        
        li {
          margin-bottom: 4px;
          line-height: 1.6;
          
          strong {
            color: var(--primary-color);
          }
        }
      }
    }
  }
  
  .reg-list {
    background: var(--bg-surface);
    border-radius: var(--radius-lg);
    padding: 16px;
    box-shadow: var(--shadow-sm);
    
    .list-title {
      font-size: 16px;
      font-weight: 600;
      margin-bottom: 16px;
      color: var(--text-primary);
    }
    
    .reg-item {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 16px;
      background: var(--bg-body);
      border-radius: var(--radius-md);
      margin-bottom: 12px;
      
      &:last-child {
        margin-bottom: 0;
      }
      
      .info {
        .dept {
          font-weight: 600;
          color: var(--text-primary);
          margin-bottom: 4px;
        }
        .doctor {
          font-size: 13px;
          color: var(--text-secondary);
          margin-bottom: 2px;
        }
        .time {
          font-size: 12px;
          color: var(--text-tertiary);
        }
      }
      
      .action {
        text-align: right;
        
        .queue {
          font-size: 12px;
          color: var(--text-tertiary);
          margin-bottom: 8px;
        }
      }
    }
  }
}
</style>
