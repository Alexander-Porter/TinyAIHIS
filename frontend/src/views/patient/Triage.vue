<template>
  <div class="triage-page">
    <van-nav-bar title="AI智能分诊" left-arrow @click-left="$router.back()" />
    
    <div class="chat-container">
      <AiTriageChat :initialQuery="initialQuery" @select="handleSelect" />
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { NavBar as VanNavBar } from 'vant'
import AiTriageChat from '@/components/AiTriageChat.vue'

const router = useRouter()
const initialQuery = ref('')

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
