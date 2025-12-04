<template>
  <div class="reports-page">
    <van-nav-bar title="检查报告" left-arrow @click-left="$router.back()" />
    
    <div class="content">
      <van-empty v-if="reports.length === 0" description="暂无检查报告" />
      
      <div class="report-item" v-for="report in reports" :key="report.orderId" @click="viewReport(report)">
        <div class="header">
          <div class="title">{{ report.itemName }}</div>
          <div class="status" :class="getStatusClass(report.status)">
            {{ getStatusText(report.status) }}
          </div>
        </div>
        <div class="time">{{ formatTime(report.createTime) }}</div>
        <div class="hint" v-if="report.status === 2">点击查看详细报告 ></div>
      </div>
    </div>
    
    <!-- Report Detail Popup -->
    <van-popup v-model:show="showDetail" position="bottom" :style="{ height: '90%' }" round>
      <div class="report-detail" v-if="currentReport">
        <div class="detail-header">
          <div class="title">{{ currentReport.itemName }}</div>
          <van-icon name="cross" @click="showDetail = false" />
        </div>
        <div class="detail-meta">
          <span>报告时间：{{ formatTime(currentReport.resultTime) }}</span>
        </div>
        <div class="detail-content" v-html="currentReport.resultText"></div>
      </div>
    </van-popup>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { NavBar as VanNavBar, Empty as VanEmpty, Popup as VanPopup, Icon as VanIcon } from 'vant'
import { labApi } from '@/utils/api'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const reports = ref([])
const showDetail = ref(false)
const currentReport = ref(null)

onMounted(async () => {
  const patientId = userStore.userInfo.patientId || userStore.userInfo.userId
  
  try {
    reports.value = await labApi.getByPatient(patientId)
  } catch (e) {
    console.error('Failed to load reports', e)
  }
})

const viewReport = (report) => {
  if (report.status === 2 && report.resultText) {
    currentReport.value = report
    showDetail.value = true
  }
}

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
    cursor: pointer;
    
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
    
    .hint {
      margin-top: 10px;
      font-size: 13px;
      color: #409eff;
    }
  }
  
  .report-detail {
    height: 100%;
    display: flex;
    flex-direction: column;
    
    .detail-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 16px;
      border-bottom: 1px solid #eee;
      
      .title {
        font-size: 18px;
        font-weight: 600;
      }
    }
    
    .detail-meta {
      padding: 12px 16px;
      font-size: 13px;
      color: #666;
      background: #f5f7fa;
    }
    
    .detail-content {
      flex: 1;
      padding: 16px;
      overflow: auto;
      
      :deep(table) {
        width: 100%;
        border-collapse: collapse;
        
        th, td {
          border: 1px solid #ddd;
          padding: 8px 12px;
          text-align: left;
        }
        
        th {
          background: #f5f7fa;
          font-weight: 500;
        }
      }
      
      :deep(p) {
        margin: 10px 0;
        line-height: 1.6;
      }
      
      :deep(strong) {
        font-weight: 600;
      }
    }
  }
}
</style>
