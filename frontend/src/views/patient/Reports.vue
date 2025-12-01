<template>
  <div class="reports-page">
    <van-nav-bar title="检查报告" left-arrow @click-left="$router.back()" />
    
    <div class="content">
      <van-empty v-if="reports.length === 0" description="暂无检查报告" />
      
      <div class="report-item" v-for="report in reports" :key="report.orderId">
        <div class="header">
          <div class="title">{{ report.itemName }}</div>
          <div class="status" :class="getStatusClass(report.status)">
            {{ getStatusText(report.status) }}
          </div>
        </div>
        <div class="time">{{ formatTime(report.createTime) }}</div>
        
        <div class="result" v-if="report.status === 2 && report.resultText">
          <div class="result-title">检查结果</div>
          <div class="result-content">{{ report.resultText }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { NavBar as VanNavBar, Empty as VanEmpty } from 'vant'
import { labApi } from '@/utils/api'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const reports = ref([])

onMounted(async () => {
  const patientId = userStore.userInfo.patientId || userStore.userInfo.userId
  
  try {
    reports.value = await labApi.getByPatient(patientId)
  } catch (e) {
    console.error('Failed to load reports', e)
  }
})

const getStatusText = (status) => {
  const map = { 0: '待缴费', 1: '检验中', 2: '已出结果' }
  return map[status] || '未知'
}

const getStatusClass = (status) => {
  const map = { 0: 'warning', 1: 'info', 2: 'success' }
  return map[status] || ''
}

const formatTime = (time) => {
  if (!time) return ''
  return new Date(time).toLocaleString('zh-CN')
}
</script>

<style scoped lang="scss">
.reports-page {
  min-height: 100vh;
  background: #f5f7fa;
  
  .content {
    padding: 15px;
  }
  
  .report-item {
    background: #fff;
    border-radius: 12px;
    padding: 15px;
    margin-bottom: 10px;
    
    .header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      
      .title {
        font-weight: 500;
        font-size: 16px;
      }
      
      .status {
        font-size: 13px;
        
        &.warning { color: #e6a23c; }
        &.info { color: #409eff; }
        &.success { color: #67c23a; }
      }
    }
    
    .time {
      font-size: 13px;
      color: #999;
      margin-top: 5px;
    }
    
    .result {
      margin-top: 15px;
      padding-top: 15px;
      border-top: 1px solid #eee;
      
      .result-title {
        font-size: 14px;
        font-weight: 500;
        margin-bottom: 10px;
      }
      
      .result-content {
        font-size: 14px;
        color: #333;
        line-height: 1.6;
        white-space: pre-wrap;
      }
    }
  }
}
</style>
