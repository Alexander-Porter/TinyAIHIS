<template>
  <div class="records-page">
    <van-nav-bar title="就诊记录" left-arrow @click-left="$router.back()" />
    
    <div class="content">
      <van-empty v-if="records.length === 0" description="暂无就诊记录" />
      
      <div class="record-item" v-for="record in records" :key="record.recordId" @click="showDetail(record)">
        <div class="header">
          <div class="date">{{ formatDate(record.createTime) }}</div>
        </div>
        <div class="diagnosis">
          <span class="label">诊断：</span>{{ record.diagnosis || '暂无' }}
        </div>
        <div class="symptom">
          <span class="label">主诉：</span>{{ record.symptom || '暂无' }}
        </div>
      </div>
    </div>
    
    <!-- Detail Popup -->
    <van-popup v-model:show="showPopup" position="bottom" :style="{ height: '70%' }" round>
      <div class="detail-popup" v-if="currentRecord">
        <div class="popup-header">
          <div class="title">就诊详情</div>
          <van-icon name="cross" @click="showPopup = false" />
        </div>
        <div class="popup-content">
          <div class="section">
            <div class="section-title">主诉</div>
            <div class="section-content">{{ currentRecord.symptom || '暂无' }}</div>
          </div>
          <div class="section">
            <div class="section-title">诊断</div>
            <div class="section-content">{{ currentRecord.diagnosis || '暂无' }}</div>
          </div>
          <div class="section">
            <div class="section-title">病历详情</div>
            <div class="section-content">{{ currentRecord.content || '暂无' }}</div>
          </div>
        </div>
      </div>
    </van-popup>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { NavBar as VanNavBar, Empty as VanEmpty, Popup as VanPopup, Icon as VanIcon } from 'vant'
import { emrApi } from '@/utils/api'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const records = ref([])
const showPopup = ref(false)
const currentRecord = ref(null)

onMounted(async () => {
  const patientId = userStore.userInfo.patientId || userStore.userInfo.userId
  
  try {
    records.value = await emrApi.getByPatient(patientId)
  } catch (e) {
    console.error('Failed to load records', e)
  }
})

const formatDate = (time) => {
  if (!time) return ''
  return new Date(time).toLocaleDateString('zh-CN')
}

const showDetail = (record) => {
  currentRecord.value = record
  showPopup.value = true
}
</script>

<style scoped lang="scss">
.records-page {
  min-height: 100vh;
  background: #f5f7fa;
  
  .content {
    padding: 15px;
  }
  
  .record-item {
    background: #fff;
    border-radius: 12px;
    padding: 15px;
    margin-bottom: 10px;
    cursor: pointer;
    
    &:active {
      background: #f5f5f5;
    }
    
    .header {
      margin-bottom: 10px;
      
      .date {
        font-size: 14px;
        color: #999;
      }
    }
    
    .diagnosis, .symptom {
      font-size: 14px;
      margin-bottom: 5px;
      
      .label {
        color: #999;
      }
    }
  }
  
  .detail-popup {
    height: 100%;
    display: flex;
    flex-direction: column;
    
    .popup-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 15px;
      border-bottom: 1px solid #eee;
      
      .title {
        font-size: 16px;
        font-weight: 500;
      }
    }
    
    .popup-content {
      flex: 1;
      overflow: auto;
      padding: 15px;
      
      .section {
        margin-bottom: 20px;
        
        .section-title {
          font-size: 14px;
          color: #999;
          margin-bottom: 8px;
        }
        
        .section-content {
          font-size: 15px;
          line-height: 1.6;
          white-space: pre-wrap;
        }
      }
    }
  }
}
</style>
