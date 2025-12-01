<template>
  <div class="triage-page">
    <van-nav-bar title="AI智能分诊" left-arrow @click-left="$router.back()" />
    
    <div class="content">
      <!-- Body Map -->
      <div class="body-section">
        <h3>请点击不适部位</h3>
        <div class="body-map">
          <div class="body-part" 
               v-for="part in bodyParts" 
               :key="part.name"
               :class="{ active: selectedPart === part.name }"
               :style="part.style"
               @click="selectPart(part.name)">
            {{ part.label }}
          </div>
        </div>
      </div>
      
      <!-- Symptoms Input -->
      <div class="symptoms-section">
        <h3>描述您的症状</h3>
        <van-field
          v-model="symptoms"
          rows="3"
          autosize
          type="textarea"
          placeholder="请详细描述您的不适症状，例如：咳嗽两天，伴有发热..."
        />
      </div>
      
      <!-- Submit Button -->
      <div class="submit-section">
        <van-button type="primary" block :loading="loading" @click="doTriage">
          🤖 AI分析推荐
        </van-button>
      </div>
      
      <!-- Result -->
      <div class="result-section" v-if="result">
        <div class="result-card">
          <div class="title">🏥 推荐就诊科室</div>
          <div class="dept-name">{{ result.deptName }}</div>
          <div class="reason">{{ result.reason }}</div>
          <van-button type="success" block @click="goAppointment">
            立即挂号
          </van-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { NavBar as VanNavBar, Field as VanField, Button as VanButton, showToast } from 'vant'
import { triageApi } from '@/utils/api'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const selectedPart = ref('')
const symptoms = ref('')
const loading = ref(false)
const result = ref(null)

const bodyParts = [
  { name: '头部', label: '🧠 头部', style: { top: '5%', left: '50%', transform: 'translateX(-50%)' } },
  { name: '眼睛', label: '👁️ 眼睛', style: { top: '10%', left: '35%' } },
  { name: '耳朵', label: '👂 耳朵', style: { top: '10%', right: '35%' } },
  { name: '咽喉', label: '🗣️ 咽喉', style: { top: '18%', left: '50%', transform: 'translateX(-50%)' } },
  { name: '胸部', label: '💗 胸部', style: { top: '28%', left: '50%', transform: 'translateX(-50%)' } },
  { name: '腹部', label: '🫃 腹部', style: { top: '42%', left: '50%', transform: 'translateX(-50%)' } },
  { name: '手臂', label: '💪 手臂', style: { top: '35%', left: '20%' } },
  { name: '腰部', label: '🔙 腰部', style: { top: '55%', left: '50%', transform: 'translateX(-50%)' } },
  { name: '腿部', label: '🦵 腿部', style: { top: '70%', left: '50%', transform: 'translateX(-50%)' } },
]

const selectPart = (name) => {
  selectedPart.value = name
}

const doTriage = async () => {
  if (!selectedPart.value && !symptoms.value) {
    showToast('请选择部位或描述症状')
    return
  }
  
  loading.value = true
  try {
    result.value = await triageApi.recommend({
      bodyPart: selectedPart.value,
      description: symptoms.value
    })
  } catch (e) {
    console.error('Triage failed', e)
  } finally {
    loading.value = false
  }
}

const goAppointment = () => {
  if (!userStore.isLoggedIn) {
    showToast('请先登录')
    router.push('/patient/login')
    return
  }
  router.push({
    path: '/patient/appointment',
    query: { deptId: result.value.deptId }
  })
}
</script>

<style scoped lang="scss">
.triage-page {
  min-height: 100vh;
  background: #f5f7fa;
  
  .content {
    padding: 15px;
  }
  
  h3 {
    font-size: 16px;
    margin: 0 0 10px;
    color: #333;
  }
  
  .body-section {
    background: #fff;
    border-radius: 12px;
    padding: 15px;
    margin-bottom: 15px;
    
    .body-map {
      position: relative;
      height: 350px;
      background: linear-gradient(180deg, #e8f4fd 0%, #fff 100%);
      border-radius: 8px;
      
      .body-part {
        position: absolute;
        padding: 8px 12px;
        background: rgba(64, 158, 255, 0.1);
        border: 2px solid #409eff;
        border-radius: 20px;
        font-size: 12px;
        cursor: pointer;
        transition: all 0.2s;
        white-space: nowrap;
        
        &:hover, &.active {
          background: #409eff;
          color: #fff;
          transform: scale(1.1);
        }
      }
    }
  }
  
  .symptoms-section {
    background: #fff;
    border-radius: 12px;
    padding: 15px;
    margin-bottom: 15px;
  }
  
  .submit-section {
    margin-bottom: 15px;
  }
  
  .result-section {
    .result-card {
      background: linear-gradient(135deg, #67c23a 0%, #529b2e 100%);
      border-radius: 12px;
      padding: 20px;
      color: #fff;
      text-align: center;
      
      .title {
        font-size: 14px;
        opacity: 0.9;
        margin-bottom: 10px;
      }
      
      .dept-name {
        font-size: 28px;
        font-weight: bold;
        margin-bottom: 10px;
      }
      
      .reason {
        font-size: 14px;
        opacity: 0.9;
        margin-bottom: 20px;
      }
    }
  }
}
</style>
