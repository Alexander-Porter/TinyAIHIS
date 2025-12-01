<template>
  <div class="dashboard">
    <h2>仪表盘</h2>
    <div class="stats-grid">
      <div class="stat-card">
        <div class="icon">👥</div>
        <div class="info">
          <div class="value">{{ stats.patients }}</div>
          <div class="label">今日挂号</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="icon">👨‍⚕️</div>
        <div class="info">
          <div class="value">{{ stats.doctors }}</div>
          <div class="label">在岗医生</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="icon">💊</div>
        <div class="info">
          <div class="value">{{ stats.prescriptions }}</div>
          <div class="label">处方数</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="icon">🔬</div>
        <div class="info">
          <div class="value">{{ stats.labOrders }}</div>
          <div class="label">检验单</div>
        </div>
      </div>
    </div>
    
    <div class="quick-actions">
      <h3>快捷操作</h3>
      <el-button-group>
        <el-button @click="$router.push('/admin/users')">用户管理</el-button>
        <el-button @click="$router.push('/admin/schedules')">排班管理</el-button>
        <el-button @click="$router.push('/admin/departments')">科室管理</el-button>
      </el-button-group>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'

const stats = ref({
  patients: 128,
  doctors: 24,
  prescriptions: 356,
  labOrders: 89
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
