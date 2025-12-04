<template>
  <div class="dashboard">
    <h2>仪表盘</h2>
    <div class="stats-grid">
      <div class="stat-card" @click="$router.push('/admin/query?type=registration')">
        <div class="icon">👥</div>
        <div class="info">
          <div class="value">{{ stats.todayRegistrations }}</div>
          <div class="label">今日挂号</div>
        </div>
      </div>
      <div class="stat-card" @click="$router.push('/admin/users?role=DOCTOR')">
        <div class="icon">👨‍⚕️</div>
        <div class="info">
          <div class="value">{{ stats.doctors }}</div>
          <div class="label">医生总数</div>
        </div>
      </div>
      <div class="stat-card" @click="$router.push('/admin/query?type=prescription')">
        <div class="icon">💊</div>
        <div class="info">
          <div class="value">{{ stats.todayPrescriptions }}</div>
          <div class="label">今日处方</div>
        </div>
      </div>
      <div class="stat-card" @click="$router.push('/admin/query?type=lab')">
        <div class="icon">🔬</div>
        <div class="info">
          <div class="value">{{ stats.todayLabOrders }}</div>
          <div class="label">今日检验</div>
        </div>
      </div>
    </div>
    
    <div class="quick-actions">
      <h3>快捷操作</h3>
      <a-space>
        <a-button @click="$router.push('/admin/users')">用户管理</a-button>
        <a-button @click="$router.push('/admin/schedules')">排班管理</a-button>
        <a-button @click="$router.push('/admin/departments')">科室管理</a-button>
        <a-button type="primary" @click="$router.push('/admin/query')">数据查询</a-button>
      </a-space>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { adminApi } from '@/utils/api'

const stats = ref({
  todayRegistrations: 0,
  doctors: 0,
  todayPrescriptions: 0,
  todayLabOrders: 0
})

onMounted(async () => {
  try {
    const data = await adminApi.getDashboardStats()
    stats.value = data
  } catch (e) {
    console.error('Failed to load stats', e)
  }
})
</script>

<style scoped lang="scss">
.dashboard {
  padding: 20px;
  
  h2 { margin: 0 0 20px; }
  
  .stats-grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 20px;
    margin-bottom: 30px;
    
    .stat-card {
      background: #fff;
      border-radius: 12px;
      padding: 20px;
      display: flex;
      align-items: center;
      gap: 15px;
      
      .icon {
        font-size: 40px;
        width: 60px;
        height: 60px;
        display: flex;
        align-items: center;
        justify-content: center;
        background: #f5f7fa;
        border-radius: 12px;
      }
      
      .info {
        .value {
          font-size: 28px;
          font-weight: bold;
          color: #333;
        }
        .label {
          font-size: 14px;
          color: #999;
        }
      }
    }
  }
  
  .quick-actions {
    background: #fff;
    border-radius: 12px;
    padding: 20px;
    
    h3 { margin: 0 0 15px; font-size: 16px; }
  }
}
</style>
