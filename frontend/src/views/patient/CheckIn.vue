<template>
  <div class="checkin-page">
    <van-nav-bar title="到院签到" left-arrow @click-left="$router.back()" />
    
    <div class="content">
      <div class="location-section">
        <div class="icon">📍</div>
        <div class="status" v-if="locationStatus === 'pending'">正在获取位置...</div>
        <div class="status" v-else-if="locationStatus === 'success'">位置已获取</div>
        <div class="status error" v-else-if="locationStatus === 'error'">无法获取位置</div>
      </div>
      
      <div class="reg-list">
        <div class="list-title">待签到挂号</div>
        <van-empty v-if="pendingRegs.length === 0" description="暂无待签到挂号" />
        
        <div class="reg-item" v-for="reg in pendingRegs" :key="reg.regId">
          <div class="info">
            <div class="dept">挂号单 #{{ reg.regId }}</div>
            <div class="time">排队号: {{ reg.queueNumber }}</div>
          </div>
          <van-button size="small" type="primary" :loading="checkingIn === reg.regId" @click="doCheckIn(reg)">
            签到
          </van-button>
        </div>
      </div>
      
      <div class="tips">
        <div class="tip-title">签到说明</div>
        <ul>
          <li>请在距离医院500米范围内进行签到</li>
          <li>签到后请前往相应科室候诊</li>
          <li>请注意关注叫号信息</li>
        </ul>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { NavBar as VanNavBar, Button as VanButton, Empty as VanEmpty, showToast } from 'vant'
import { registrationApi } from '@/utils/api'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const locationStatus = ref('pending')
const location = ref({ lat: 0, lng: 0 })
const pendingRegs = ref([])
const checkingIn = ref(null)

onMounted(() => {
  loadPendingRegs()
  getLocation()
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

const getLocation = () => {
  if (navigator.geolocation) {
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        location.value = {
          lat: pos.coords.latitude,
          lng: pos.coords.longitude
        }
        locationStatus.value = 'success'
      },
      (err) => {
        console.error('Location error', err)
        locationStatus.value = 'error'
        // Use mock location for testing
        location.value = { lat: 39.9042, lng: 116.4074 }
      }
    )
  } else {
    locationStatus.value = 'error'
  }
}

const doCheckIn = async (reg) => {
  if (locationStatus.value === 'pending') {
    showToast('正在获取位置，请稍候')
    return
  }
  
  checkingIn.value = reg.regId
  
  try {
    await registrationApi.checkIn({
      regId: reg.regId,
      latitude: location.value.lat,
      longitude: location.value.lng
    })
    
    showToast('签到成功')
    
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
  background: #f5f7fa;
  
  .content {
    padding: 15px;
  }
  
  .location-section {
    background: #fff;
    border-radius: 12px;
    padding: 30px;
    text-align: center;
    margin-bottom: 15px;
    
    .icon {
      font-size: 48px;
      margin-bottom: 10px;
    }
    
    .status {
      color: #67c23a;
      
      &.error {
        color: #f56c6c;
      }
    }
  }
  
  .reg-list {
    background: #fff;
    border-radius: 12px;
    padding: 15px;
    margin-bottom: 15px;
    
    .list-title {
      font-size: 16px;
      font-weight: 500;
      margin-bottom: 15px;
    }
    
    .reg-item {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 15px;
      background: #f5f7fa;
      border-radius: 8px;
      margin-bottom: 10px;
      
      .info {
        .dept {
          font-weight: 500;
        }
        .time {
          font-size: 13px;
          color: #666;
          margin-top: 5px;
        }
      }
    }
  }
  
  .tips {
    background: #fff;
    border-radius: 12px;
    padding: 15px;
    
    .tip-title {
      font-size: 14px;
      font-weight: 500;
      margin-bottom: 10px;
    }
    
    ul {
      margin: 0;
      padding-left: 20px;
      
      li {
        font-size: 13px;
        color: #666;
        line-height: 1.8;
      }
    }
  }
}
</style>
