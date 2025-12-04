<template>
  <div class="queue-screen">
    <div class="header">
      🏥 {{ deptName }} 候诊大屏
    </div>
    
    <div class="content">
      <div class="current-call">
        <div class="label">当前叫号</div>
        <div class="number" v-if="currentCall">{{ currentCall.queueNumber }}</div>
        <div class="number" v-else>--</div>
        <div class="patient-name" v-if="currentCall">{{ maskName(currentCall.patientName) }}</div>
        <div class="room" v-if="currentCall">请到 {{ currentCall.roomNumber || '诊室' }} 就诊</div>
      </div>
      
      <div class="waiting-list">
        <div class="title">候诊队列</div>
        <div class="list">
          <div class="item" v-for="item in waitingList" :key="item.regId">
            <span class="num">{{ item.queueNumber }}号</span>
            <span class="name">{{ maskName(item.patientName) }}</span>
          </div>
          <div class="empty" v-if="waitingList.length === 0">
            暂无候诊患者
          </div>
        </div>
      </div>
    </div>
    
    <div class="footer">
      <div class="marquee">
        温馨提示：请保持安静，维护就医秩序。听到叫号后请及时就诊。就诊完毕后请到缴费窗口缴费。祝您早日康复！
      </div>
    </div>
    
    <!-- Hidden audio for TTS -->
    <audio ref="audioRef" style="display: none"></audio>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { queueApi, scheduleApi } from '@/utils/api'
import SockJS from 'sockjs-client/dist/sockjs'
import { Client } from '@stomp/stompjs'

const route = useRoute()
const deptId = route.params.deptId

const deptName = ref('门诊')
const currentCall = ref(null)
const waitingList = ref([])
const audioRef = ref(null)
let stompClient = null

onMounted(async () => {
  // Load department name
  try {
    const depts = await scheduleApi.getDepartments()
    const dept = depts.find(d => d.deptId == deptId)
    if (dept) deptName.value = dept.deptName
  } catch (e) {
    console.error('Failed to load department', e)
  }
  
  // Load initial queue
  loadQueue()
  
  // Connect WebSocket
  connectWebSocket()
})

onUnmounted(() => {
  if (stompClient) {
    stompClient.deactivate()
  }
})

const loadQueue = async () => {
  try {
    const data = await queueApi.getInfo(deptId)
    if (data) {
      currentCall.value = data.current
      waitingList.value = data.waiting || []
    }
  } catch (e) {
    console.error('Failed to load queue', e)
  }
}

const connectWebSocket = () => {
  try {
    stompClient = new Client({
      webSocketFactory: () => new SockJS('/ws'),
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
    })

    stompClient.onConnect = () => {
      console.log('WebSocket connected')
      
      // Subscribe to queue updates
      stompClient.subscribe(`/topic/queue/${deptId}`, (message) => {
        const data = JSON.parse(message.body)
        handleQueueUpdate(data)
      })
    }

    stompClient.onStompError = (frame) => {
      console.error('STOMP error', frame)
    }

    stompClient.activate()
  } catch (e) {
    console.error('WebSocket connection failed', e)
  }
}

const handleQueueUpdate = (data) => {
  if (data.type === 'CALL') {
    currentCall.value = data.patient
    
    // Update waiting list
    if (data.waiting) {
      waitingList.value = data.waiting
    }
    
    // Play TTS
    speakCall(data.patient.queueNumber)
  }
}

// TTS using Web Speech API
const speakCall = (queueNumber) => {
  if ('speechSynthesis' in window) {
    const utterance = new SpeechSynthesisUtterance()
    utterance.text = `请 ${queueNumber} 号患者到诊室就诊`
    utterance.lang = 'zh-CN'
    utterance.rate = 0.9
    
    // Clear any pending speech
    window.speechSynthesis.cancel()
    
    // Speak twice for emphasis
    window.speechSynthesis.speak(utterance)
    
    setTimeout(() => {
      const utterance2 = new SpeechSynthesisUtterance()
      utterance2.text = `请 ${queueNumber} 号患者到诊室就诊`
      utterance2.lang = 'zh-CN'
      utterance2.rate = 0.9
      window.speechSynthesis.speak(utterance2)
    }, 3000)
  }
}

const maskName = (name) => {
  if (!name) return '***'
  if (name.length <= 1) return '*'
  if (name.length === 2) return name[0] + '*'
  return name[0] + '*'.repeat(name.length - 2) + name[name.length - 1]
}
</script>

<style scoped lang="scss">
.queue-screen {
  height: 100vh;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
  color: #fff;
  display: flex;
  flex-direction: column;
  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif;
  
  .header {
    height: 80px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 32px;
    font-weight: bold;
    background: rgba(255, 255, 255, 0.1);
    text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
  }
  
  .content {
    flex: 1;
    display: flex;
    padding: 30px;
    gap: 30px;
  }
  
  .current-call {
    flex: 2;
    background: rgba(255, 255, 255, 0.05);
    border-radius: 20px;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    border: 2px solid rgba(103, 194, 58, 0.5);
    
    .label {
      font-size: 28px;
      color: #67c23a;
      margin-bottom: 30px;
      text-transform: uppercase;
      letter-spacing: 4px;
    }
    
    .number {
      font-size: 180px;
      font-weight: bold;
      color: #f0f0f0;
      text-shadow: 0 4px 8px rgba(0, 0, 0, 0.3);
      animation: pulse 2s ease-in-out infinite;
    }
    
    .patient-name {
      font-size: 42px;
      margin-top: 30px;
      color: #fff;
    }
    
    .room {
      font-size: 32px;
      color: #e6a23c;
      margin-top: 20px;
    }
  }
  
  .waiting-list {
    flex: 1;
    background: rgba(255, 255, 255, 0.05);
    border-radius: 20px;
    padding: 25px;
    display: flex;
    flex-direction: column;
    
    .title {
      font-size: 26px;
      margin-bottom: 25px;
      text-align: center;
      color: #409eff;
    }
    
    .list {
      flex: 1;
      overflow: auto;
      
      .item {
        padding: 18px 20px;
        border-bottom: 1px solid rgba(255, 255, 255, 0.1);
        display: flex;
        justify-content: space-between;
        font-size: 22px;
        
        .num {
          color: #e6a23c;
          font-weight: 500;
        }
        
        .name {
          color: rgba(255, 255, 255, 0.8);
        }
      }
      
      .empty {
        text-align: center;
        color: rgba(255, 255, 255, 0.5);
        padding: 40px 0;
        font-size: 18px;
      }
    }
  }
  
  .footer {
    height: 60px;
    background: rgba(255, 255, 255, 0.1);
    display: flex;
    align-items: center;
    padding: 0 30px;
    font-size: 20px;
    overflow: hidden;
    
    .marquee {
      white-space: nowrap;
      animation: marquee 30s linear infinite;
      color: rgba(255, 255, 255, 0.8);
    }
  }
}

@keyframes pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.02); }
}

@keyframes marquee {
  0% { transform: translateX(100vw); }
  100% { transform: translateX(-100%); }
}
</style>
