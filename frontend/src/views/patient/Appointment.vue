<template>
  <div class="appointment-page">
    <van-nav-bar title="预约挂号" left-arrow @click-left="goBack" />
    
    <div class="content">
      <!-- Step 1: Select Date -->
      <div class="step" v-if="step === 1">
        <div class="step-title">选择日期</div>
        <div class="date-grid">
          <div 
            class="date-item" 
            v-for="date in dates" 
            :key="date.value"
            :class="{ active: selectedDate === date.value }"
            @click="selectDate(date.value)">
            <div class="week">{{ date.week }}</div>
            <div class="day">{{ date.day }}</div>
          </div>
        </div>
      </div>

      <!-- Step 2: Select Department -->
      <div class="step" v-if="step === 2">
        <div class="step-title">选择科室 ({{ formatDate(selectedDate) }})</div>
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
        <div class="btn-group">
          <van-button plain @click="step = 1">上一步</van-button>
        </div>
      </div>
      
      <!-- Step 3: Select Schedule -->
      <div class="step" v-if="step === 3">
        <div class="step-title">选择医生</div>
        <div class="schedule-list">
          <div v-if="availableSchedules.length === 0" class="empty-schedule">
            该科室当日暂无可用号源
          </div>
          <div 
            class="schedule-item" 
            v-for="s in availableSchedules" 
            :key="s.scheduleId"
            :class="{ active: selectedSchedule?.scheduleId === s.scheduleId, disabled: s.quotaLeft <= 0 || s.expired }"
            @click="selectSchedule(s)">
            <div class="doctor">{{ s.doctorName }}</div>
            <div class="time">{{ getShiftLabel(s.shift) }}</div>
            <div class="quota" :class="{ warning: s.quotaLeft < 5, expired: s.expired }">
              {{ s.expired ? '已过期' : '余号: ' + s.quotaLeft }}
            </div>
            <div class="fee">¥{{ s.fee }}</div>
          </div>
        </div>
        <div class="btn-group">
          <van-button plain @click="step = 2">上一步</van-button>
          <van-button type="primary" :disabled="!selectedSchedule" @click="confirmAppointment">
            确认挂号
          </van-button>
        </div>
      </div>
      
      <!-- Step 4: Success -->
      <div class="step success-step" v-if="step === 4">
        <div class="success-icon">✅</div>
        <div class="success-title">预约成功，请缴费</div>
        <div class="success-info">
          <p>日期：{{ formatDate(selectedDate) }}</p>
          <p>科室：{{ selectedDept?.deptName }}</p>
          <p>医生：{{ selectedSchedule?.doctorName }}</p>
          <p>时间：{{ getShiftLabel(selectedSchedule?.shift) }}</p>
          <p>挂号费：¥{{ selectedSchedule?.fee }}</p>
          <p style="color: #f56c6c; font-weight: bold; margin-top: 10px">请立即完成支付，否则无法签到就诊</p>
        </div>
        <div class="btn-group">
          <van-button type="primary" block @click="goPayment">立即缴费</van-button>
          <van-button block @click="$router.push('/patient/home')">稍后缴费</van-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, watch } from 'vue'
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
const selectedDate = ref(null)
const selectedDept = ref(null)
const selectedSchedule = ref(null)
const newRegId = ref(null)
const lastPrefillToken = ref('')

const dates = computed(() => {
  const list = []
  const weeks = ['日', '一', '二', '三', '四', '五', '六']
  for (let i = 0; i < 7; i++) {
    const d = new Date()
    d.setDate(d.getDate() + i)
    list.push({
      value: d.toISOString().split('T')[0],
      day: d.getDate(),
      week: i === 0 ? '今天' : '周' + weeks[d.getDay()]
    })
  }
  return list
})

onMounted(async () => {
  try {
    departments.value = await scheduleApi.getDepartments()
    preselectDeptFromQuery()
  } catch (e) {
    console.error('Failed to load departments', e)
  }
})

const preselectDeptFromQuery = async () => {
  if (!departments.value.length) return
  const queryDeptId = route.query.deptId ? Number(route.query.deptId) : null
  const queryDeptName = route.query.deptName
  if (!queryDeptId && !queryDeptName) return

  const token = `${queryDeptId ?? ''}-${queryDeptName ?? ''}`
  if (lastPrefillToken.value === token && selectedDept.value) return

  const targetDept = departments.value.find((dept) => {
    if (queryDeptId) {
      return Number(dept.deptId) === Number(queryDeptId)
    }
    if (queryDeptName) {
      return dept.deptName === queryDeptName
    }
    return false
  })

  if (!targetDept) return
  if (!selectedDate.value) {
    const firstDate = dates.value[0]?.value || new Date().toISOString().split('T')[0]
    selectedDate.value = firstDate
  }
  lastPrefillToken.value = token
  await selectDept(targetDept)
}

watch(
  () => [route.query.deptId, route.query.deptName, departments.value.length],
  () => {
    preselectDeptFromQuery()
  }
)

const selectDate = (date) => {
  selectedDate.value = date
  step.value = 2
}

const selectDept = async (dept) => {
  selectedDept.value = dept
  selectedSchedule.value = null
  step.value = 3
  
  // Load schedules for this department and date
  try {
    schedules.value = await scheduleApi.getScheduleList(dept.deptId, selectedDate.value, selectedDate.value)
  } catch (e) {
    console.error('Failed to load schedules', e)
    schedules.value = []
  }
}

// 过滤掉已过期的号源（但仍可以显示，只是不可选）
const availableSchedules = computed(() => {
  // 返回所有号源，前端根据 expired 状态显示
  return schedules.value
})

const selectSchedule = (schedule) => {
  if (schedule.expired) {
    showToast('该时段号源已过期')
    return
  }
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
    step.value = 4
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

const goBack = () => {
  if (step.value > 1) {
    step.value--
  } else {
    router.back()
  }
}

const formatDate = (str) => {
  if (!str) return ''
  return new Date(str).toLocaleDateString()
}

const getShiftLabel = (shift) => {
  const labels = { 'AM': '上午', 'PM': '下午', 'ER': '急诊(全天)' }
  return labels[shift] || shift
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

  .date-grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 10px;

    .date-item {
      background: #fff;
      border: 2px solid #eee;
      border-radius: 8px;
      padding: 10px;
      text-align: center;
      cursor: pointer;
      
      &.active {
        border-color: #409eff;
        background: #ecf5ff;
        color: #409eff;
      }

      .week { font-size: 12px; color: #666; margin-bottom: 5px; }
      .day { font-size: 18px; font-weight: bold; }
    }
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
    .empty-schedule {
      text-align: center;
      color: #999;
      padding: 30px 0;
    }

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
        
        &.expired {
          color: #909399;
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
