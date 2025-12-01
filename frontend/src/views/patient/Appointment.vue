<template>
  <div class="appointment-page">
    <van-nav-bar title="预约挂号" left-arrow @click-left="$router.back()" />
    
    <div class="content">
      <!-- Step 1: Select Department -->
      <div class="step" v-if="step === 1">
        <div class="step-title">选择科室</div>
        <div class="dept-grid">
          <div 
            class="dept-item" 
            v-for="dept in departments" 
            :key="dept.deptId"
            :class="{ active: selectedDept?.deptId === dept.deptId }"
            @click="selectDept(dept)">
            {{ dept.deptName }}
          </div>
        </div>
        <div class="btn-group" v-if="selectedDept">
          <van-button type="primary" block @click="step = 2">下一步</van-button>
        </div>
      </div>
      
      <!-- Step 2: Select Schedule -->
      <div class="step" v-if="step === 2">
        <div class="step-title">选择医生和时间</div>
        <div class="schedule-list">
          <div 
            class="schedule-item" 
            v-for="s in schedules" 
            :key="s.scheduleId"
            :class="{ active: selectedSchedule?.scheduleId === s.scheduleId, disabled: s.quotaLeft <= 0 }"
            @click="selectSchedule(s)">
            <div class="doctor">{{ s.doctorName }}</div>
            <div class="time">{{ s.date }} {{ s.shift === 'AM' ? '上午' : '下午' }}</div>
            <div class="quota" :class="{ warning: s.quotaLeft < 5 }">
              余号: {{ s.quotaLeft }}
            </div>
            <div class="fee">¥{{ s.fee }}</div>
          </div>
        </div>
        <div class="btn-group">
          <van-button plain @click="step = 1">上一步</van-button>
          <van-button type="primary" :disabled="!selectedSchedule" @click="confirmAppointment">
            确认挂号
          </van-button>
        </div>
      </div>
      
      <!-- Step 3: Success -->
      <div class="step success-step" v-if="step === 3">
        <div class="success-icon">✅</div>
        <div class="success-title">挂号成功</div>
        <div class="success-info">
          <p>科室：{{ selectedDept?.deptName }}</p>
          <p>医生：{{ selectedSchedule?.doctorName }}</p>
          <p>时间：{{ selectedSchedule?.date }} {{ selectedSchedule?.shift === 'AM' ? '上午' : '下午' }}</p>
          <p>挂号费：¥{{ selectedSchedule?.fee }}</p>
        </div>
        <div class="btn-group">
          <van-button type="primary" block @click="goPayment">去缴费</van-button>
          <van-button block @click="$router.push('/patient/home')">返回首页</van-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { NavBar as VanNavBar, Button as VanButton, showToast, showLoadingToast, closeToast } from 'vant'
import { scheduleApi, registrationApi } from '@/utils/api'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const step = ref(1)
const departments = ref([])
const schedules = ref([])
const selectedDept = ref(null)
const selectedSchedule = ref(null)
const newRegId = ref(null)

onMounted(async () => {
  try {
    departments.value = await scheduleApi.getDepartments()
    
    // Pre-select department if passed from triage
    if (route.query.deptId) {
      const dept = departments.value.find(d => d.deptId == route.query.deptId)
      if (dept) {
        selectDept(dept)
        step.value = 2
      }
    }
  } catch (e) {
    console.error('Failed to load departments', e)
  }
})

const selectDept = async (dept) => {
  selectedDept.value = dept
  selectedSchedule.value = null
  
  // Load schedules for this department
  const today = new Date().toISOString().split('T')[0]
  const nextWeek = new Date(Date.now() + 7 * 24 * 60 * 60 * 1000).toISOString().split('T')[0]
  
  try {
    schedules.value = await scheduleApi.getScheduleList(dept.deptId, today, nextWeek)
  } catch (e) {
    console.error('Failed to load schedules', e)
  }
}

const selectSchedule = (schedule) => {
  if (schedule.quotaLeft <= 0) {
    showToast('该时段已约满')
    return
  }
  selectedSchedule.value = schedule
}

const confirmAppointment = async () => {
  if (!userStore.isLoggedIn) {
    showToast('请先登录')
    router.push('/patient/login')
    return
  }
  
  showLoadingToast({ message: '挂号中...', forbidClick: true })
  
  try {
    const patientId = userStore.userInfo.patientId || userStore.userInfo.userId
    const result = await registrationApi.create({
      patientId,
      scheduleId: selectedSchedule.value.scheduleId
    })
    newRegId.value = result.regId
    closeToast()
    showToast('挂号成功')
    step.value = 3
  } catch (e) {
    closeToast()
    console.error('Appointment failed', e)
  }
}

const goPayment = () => {
  router.push({
    path: '/patient/payment',
    query: { regId: newRegId.value }
  })
}
</script>

<style scoped lang="scss">
.appointment-page {
  min-height: 100vh;
  background: #f5f7fa;
  
  .content {
    padding: 15px;
  }
  
  .step-title {
    font-size: 16px;
    font-weight: 500;
    margin-bottom: 15px;
  }
  
  .dept-grid {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 10px;
    
    .dept-item {
      background: #fff;
      border: 2px solid #eee;
      border-radius: 8px;
      padding: 15px;
      text-align: center;
      cursor: pointer;
      font-size: 14px;
      
      &:active, &.active {
        border-color: #409eff;
        color: #409eff;
        background: #ecf5ff;
      }
    }
  }
  
  .schedule-list {
    .schedule-item {
      background: #fff;
      border: 2px solid #eee;
      border-radius: 8px;
      padding: 15px;
      margin-bottom: 10px;
      display: flex;
      align-items: center;
      justify-content: space-between;
      cursor: pointer;
      
      &.active {
        border-color: #409eff;
        background: #ecf5ff;
      }
      
      &.disabled {
        opacity: 0.5;
        cursor: not-allowed;
      }
      
      .doctor {
        font-weight: 500;
        flex: 1;
      }
      
      .time {
        font-size: 13px;
        color: #666;
        flex: 1;
      }
      
      .quota {
        font-size: 13px;
        color: #67c23a;
        
        &.warning {
          color: #e6a23c;
        }
      }
      
      .fee {
        font-weight: 500;
        color: #f56c6c;
        margin-left: 15px;
      }
    }
  }
  
  .btn-group {
    margin-top: 20px;
    display: flex;
    gap: 10px;
    
    > * {
      flex: 1;
    }
  }
  
  .success-step {
    text-align: center;
    padding: 40px 20px;
    
    .success-icon {
      font-size: 64px;
      margin-bottom: 20px;
    }
    
    .success-title {
      font-size: 24px;
      font-weight: 500;
      margin-bottom: 20px;
    }
    
    .success-info {
      background: #fff;
      border-radius: 12px;
      padding: 20px;
      margin-bottom: 20px;
      text-align: left;
      
      p {
        margin: 10px 0;
        color: #666;
      }
    }
  }
}
</style>
