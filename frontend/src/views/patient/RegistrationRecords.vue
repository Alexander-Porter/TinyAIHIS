<template>
  <div class="reg-records-page">
    <van-nav-bar title="挂号记录" left-arrow @click-left="$router.back()" />
    
    <!-- Tabs -->
    <van-tabs v-model:active="activeTab" sticky>
      <van-tab title="待就诊" name="pending">
        <div class="list-content">
          <van-empty v-if="pendingList.length === 0" description="暂无待就诊挂号" />
          <div class="reg-card" v-for="reg in pendingList" :key="reg.regId">
            <div class="card-header">
              <div class="dept-info">
                <span class="dept-name">{{ reg.deptName || '门诊' }}</span>
                <van-tag :type="getStatusTagType(reg.status, reg)" size="medium">{{ getStatusText(reg.status, reg) }}</van-tag>
              </div>
              <div class="date">{{ formatDate(reg.scheduleDate) }} {{ getShiftText(reg.shift) }}</div>
            </div>
            <div class="card-body">
              <div class="info-row">
                <van-icon name="manager-o" />
                <span>{{ reg.doctorName || '医生' }}</span>
              </div>
              <div class="info-row" v-if="reg.queueNumber">
                <van-icon name="friends-o" />
                <span>排队号：{{ reg.queueNumber }}</span>
              </div>
              <div class="info-row">
                <van-icon name="gold-coin-o" />
                <span>挂号费：¥{{ reg.fee?.toFixed(2) || '0.00' }}</span>
              </div>
            </div>
            <div class="card-footer">
              <!-- 待缴费 -->
              <template v-if="reg.status === 0">
                <van-button size="small" type="primary" @click="payReg(reg)">去缴费</van-button>
                <van-button size="small" plain @click="cancelReg(reg)">取消</van-button>
              </template>
              <!-- 待签到（急诊不需要签到，直接等候叫号） -->
              <template v-else-if="reg.status === 1 && !isEmergency(reg)">
                <template v-if="canCheckIn(reg)">
                  <van-button size="small" type="primary" @click="checkIn(reg)">签到</van-button>
                </template>
                <van-button size="small" plain @click="cancelReg(reg)">取消</van-button>
              </template>
              <!-- 急诊已缴费，显示候诊状态 -->
              <template v-else-if="reg.status === 1 && isEmergency(reg)">
                <van-button size="small" plain disabled>候诊中</van-button>
              </template>
              <!-- 候诊中 -->
              <template v-else-if="reg.status === 2">
                <van-button size="small" plain disabled>候诊中</van-button>
              </template>
              <!-- 就诊中 -->
              <template v-else-if="reg.status === 3">
                <van-button size="small" plain type="primary" disabled>就诊中</van-button>
              </template>
            </div>
          </div>
        </div>
      </van-tab>
      
      <van-tab title="已完成" name="completed">
        <div class="list-content">
          <van-empty v-if="completedList.length === 0" description="暂无已完成挂号" />
          <div class="reg-card completed" v-for="reg in completedList" :key="reg.regId">
            <div class="card-header">
              <div class="dept-info">
                <span class="dept-name">{{ reg.deptName || '门诊' }}</span>
                <van-tag type="success" size="medium">已完成</van-tag>
              </div>
              <div class="date">{{ formatDate(reg.scheduleDate) }}</div>
            </div>
            <div class="card-body">
              <div class="info-row">
                <van-icon name="manager-o" />
                <span>{{ reg.doctorName || '医生' }}</span>
              </div>
            </div>
            <div class="card-footer">
              <van-button size="small" plain @click="viewVisitRecord(reg)">查看就诊记录</van-button>
            </div>
          </div>
        </div>
      </van-tab>
      
      <van-tab title="已取消" name="cancelled">
        <div class="list-content">
          <van-empty v-if="cancelledList.length === 0" description="暂无已取消挂号" />
          <div class="reg-card cancelled" v-for="reg in cancelledList" :key="reg.regId">
            <div class="card-header">
              <div class="dept-info">
                <span class="dept-name">{{ reg.deptName || '门诊' }}</span>
                <van-tag type="default" size="medium">已取消</van-tag>
              </div>
              <div class="date">{{ formatDate(reg.scheduleDate) }}</div>
            </div>
            <div class="card-body">
              <div class="info-row">
                <van-icon name="manager-o" />
                <span>{{ reg.doctorName || '医生' }}</span>
              </div>
            </div>
          </div>
        </div>
      </van-tab>
    </van-tabs>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { NavBar as VanNavBar, Tabs as VanTabs, Tab as VanTab, Empty as VanEmpty, Tag as VanTag, Button as VanButton, Icon as VanIcon, showToast, showConfirmDialog } from 'vant'
import { registrationApi } from '@/utils/api'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const activeTab = ref('pending')
const registrations = ref([])

// 待就诊：状态0~3
const pendingList = computed(() => 
  registrations.value.filter(r => r.status >= 0 && r.status <= 3)
)

// 已完成：状态4
const completedList = computed(() =>
  registrations.value.filter(r => r.status === 4)
)

// 已取消：状态5
const cancelledList = computed(() =>
  registrations.value.filter(r => r.status === 5)
)

onMounted(() => {
  loadRegistrations()
})

const loadRegistrations = async () => {
  try {
    const patientId = userStore.userInfo.patientId || userStore.userInfo.userId
    registrations.value = await registrationApi.getByPatient(patientId)
  } catch (e) {
    console.error('Failed to load registrations', e)
  }
}

const formatDate = (date) => {
  if (!date) return ''
  return new Date(date).toLocaleDateString('zh-CN', { month: 'long', day: 'numeric', weekday: 'short' })
}

const getShiftText = (shift) => {
  if (shift === 'AM' || shift === '上午') return '上午'
  if (shift === 'PM' || shift === '下午') return '下午'
  if (shift === 'ER' || shift === '急诊(全天)') return '急诊'
  return shift || ''
}

const getStatusText = (status, reg = null) => {
  // 急诊已缴费（状态1）视为候诊中
  if (status === 1 && reg && isEmergency(reg)) {
    return '候诊中'
  }
  const statusMap = {
    0: '待缴费',
    1: '待签到',
    2: '候诊中',
    3: '就诊中',
    4: '已完成',
    5: '已取消'
  }
  return statusMap[status] || '未知'
}

const getStatusTagType = (status, reg = null) => {
  // 急诊已缴费（状态1）使用候诊中的样式
  if (status === 1 && reg && isEmergency(reg)) {
    return 'primary'
  }
  const typeMap = {
    0: 'warning',
    1: 'primary',
    2: 'primary',
    3: 'success',
    4: 'success',
    5: 'default'
  }
  return typeMap[status] || 'default'
}

const canCheckIn = (reg) => {
  // 急诊不需要签到
  if (isEmergency(reg)) return false
  
  const now = new Date()
  const scheduleDate = new Date(reg.scheduleDate)
  
  // 同一天才能签到
  if (scheduleDate.toDateString() !== now.toDateString()) return false
  
  const hour = now.getHours()
  if ((reg.shift === 'AM' || reg.shift === '上午') && hour < 12) return true
  if ((reg.shift === 'PM' || reg.shift === '下午') && hour >= 12) return true
  
  return false
}

// 判断是否是急诊
const isEmergency = (reg) => {
  return reg.shift === 'ER' || reg.shift === '急诊(全天)' || reg.shift === '急诊'
}

const payReg = async (reg) => {
  try {
    await registrationApi.pay(reg.regId)
    showToast('缴费成功')
    loadRegistrations()
  } catch (e) {
    showToast('缴费失败')
  }
}

const checkIn = async (reg) => {
  try {
    await registrationApi.checkIn({ regId: reg.regId })
    showToast('签到成功')
    loadRegistrations()
  } catch (e) {
    showToast(e.message || '签到失败')
  }
}

const cancelReg = async (reg) => {
  try {
    await showConfirmDialog({
      title: '取消挂号',
      message: '确定要取消这个挂号吗？'
    })
    await registrationApi.cancel(reg.regId)
    showToast('已取消')
    loadRegistrations()
  } catch (e) {
    if (e !== 'cancel') {
      showToast('取消失败')
    }
  }
}

const viewVisitRecord = (reg) => {
  // 跳转到就诊记录详情
  router.push(`/patient/records?regId=${reg.regId}`)
}
</script>

<style scoped lang="scss">
.reg-records-page {
  min-height: 100vh;
  background: #f5f7fa;
}

.list-content {
  padding: 12px;
}

.reg-card {
  background: #fff;
  border-radius: 12px;
  margin-bottom: 12px;
  overflow: hidden;
  box-shadow: 0 1px 4px rgba(0,0,0,0.05);
  
  &.completed {
    opacity: 0.85;
  }
  
  &.cancelled {
    opacity: 0.6;
  }
  
  .card-header {
    padding: 14px 16px;
    border-bottom: 1px solid #f5f5f5;
    
    .dept-info {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 4px;
      
      .dept-name {
        font-size: 16px;
        font-weight: 600;
      }
    }
    
    .date {
      font-size: 13px;
      color: #999;
    }
  }
  
  .card-body {
    padding: 12px 16px;
    
    .info-row {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 8px;
      font-size: 14px;
      color: #666;
      
      &:last-child {
        margin-bottom: 0;
      }
      
      .van-icon {
        color: #999;
      }
    }
  }
  
  .card-footer {
    padding: 12px 16px;
    border-top: 1px solid #f5f5f5;
    display: flex;
    gap: 12px;
    justify-content: flex-end;
  }
}
</style>
