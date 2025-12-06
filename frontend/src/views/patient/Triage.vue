<template>
  <div class="triage-page">
    <van-nav-bar title="AI智能分诊" left-arrow @click-left="$router.back()" />
    
    <div class="content" v-if="!chatMode">
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
        <van-button type="primary" block @click="startChat">
          🤖 开始AI问诊
        </van-button>
      </div>
    </div>

    <div class="chat-container" v-else>
      <AiTriageChat :initialQuery="initialQuery" @select="handleSelect" />
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { NavBar as VanNavBar, Field as VanField, Button as VanButton, showToast } from 'vant'
import { triageApi } from '@/utils/api'
import { useUserStore } from '@/stores/user'
import AiTriageChat from '@/components/AiTriageChat.vue'

const router = useRouter()
const userStore = useUserStore()

const selectedPart = ref('')
const symptoms = ref('')
const chatMode = ref(false)
const initialQuery = ref('')

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

const startChat = () => {
  let query = ''
  if (selectedPart.value) query += `部位：${selectedPart.value}。`
  if (symptoms.value) query += `症状：${symptoms.value}`
  
  initialQuery.value = query
  chatMode.value = true
}

const handleSelect = (data) => {
  router.push({
    path: '/patient/appointment',
    query: { deptName: data.department }
  })
}
</script>

<style scoped lang="scss">
.triage-page {
  min-height: 100vh;
  background: #f5f6f7;
  display: flex;
  flex-direction: column;
  
  .content {
    flex: 1;
    padding: 16px;
    overflow-y: auto;
  }
  
  .chat-container {
    flex: 1;
    height: 0; // Force flex item to respect height
    padding: 10px;
  }

  .body-section {
    background: #fff;
    border-radius: 12px;
    padding: 16px;
    margin-bottom: 16px;
    
    h3 {
      margin: 0 0 16px 0;
      font-size: 16px;
    }
    
    .body-map {
      position: relative;
      height: 300px;
      background: #e6f7ff;
      border-radius: 8px;
      
      .body-part {
        position: absolute;
        background: #fff;
        padding: 4px 8px;
        border-radius: 12px;
        font-size: 12px;
        box-shadow: 0 2px 4px rgba(0,0,0,0.1);
        cursor: pointer;
        transition: all 0.2s;
        
        &.active {
          background: #1890ff;
          color: #fff;
          transform: scale(1.1) translateX(-50%);
        }
      }
    }
  }
  
  .symptoms-section {
    background: #fff;
    border-radius: 12px;
    padding: 16px;
    margin-bottom: 24px;
    
    h3 {
      margin: 0 0 12px 0;
      font-size: 16px;
    }
  }
}
</style>
