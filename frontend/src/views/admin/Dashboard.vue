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
    
    <div class="charts-section">
      <!-- Trend Chart -->
      <div class="chart-card">
        <h3>近7天挂号趋势</h3>
        <div class="chart-container" v-if="stats.flowStats && stats.flowStats.length">
          <div class="bar-group" v-for="day in stats.flowStats" :key="day.date">
            <div class="bar" :style="{ height: getBarHeight(day.count) + '%' }" :data-count="day.count"></div>
            <span class="date">{{ formatDate(day.date) }}</span>
          </div>
        </div>
        <a-empty v-else description="暂无数据" />
      </div>

      <!-- Top Doctors -->
      <div class="chart-card">
        <h3>热门医生 TOP 5</h3>
        <div class="top-list" v-if="stats.topDoctors && stats.topDoctors.length">
          <div class="top-item" v-for="(doc, idx) in stats.topDoctors" :key="doc.doctorId">
            <div class="rank" :class="{ 'top-3': idx < 3 }">{{ idx + 1 }}</div>
            <span class="name">{{ doc.name }}</span>
            <div class="bar-wrapper">
              <div class="bar" :style="{ width: getDocBarWidth(doc.count) + '%' }"></div>
            </div>
            <span class="count">{{ doc.count }}</span>
          </div>
        </div>
        <a-empty v-else description="暂无数据" />
      </div>
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
  todayLabOrders: 0,
  flowStats: [],
  topDoctors: []
})

const getBarHeight = (count) => {
  if (!stats.value.flowStats.length) return 0
  const max = Math.max(...stats.value.flowStats.map(d => d.count)) || 1
  return (count / max) * 100
}

const getDocBarWidth = (count) => {
  if (!stats.value.topDoctors.length) return 0
  const max = Math.max(...stats.value.topDoctors.map(d => d.count)) || 1
  return (count / max) * 100
}

const formatDate = (dateStr) => {
  return dateStr.slice(5) // MM-DD
}

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
  
  .charts-section {
    display: grid;
    grid-template-columns: 2fr 1fr;
    gap: 20px;

    .chart-card {
      background: #fff;
      border-radius: 12px;
      padding: 20px;
      height: 100%;

      h3 { margin: 0 0 20px; font-size: 16px; font-weight: 600; }

      .chart-container {
        height: 250px;
        display: flex;
        align-items: flex-end;
        justify-content: space-around;
        padding-bottom: 20px;
        border-bottom: 1px solid #f0f0f0;

        .bar-group {
          display: flex;
          flex-direction: column;
          align-items: center;
          gap: 8px;
          height: 100%;
          justify-content: flex-end;
          width: 40px;

          .bar {
            width: 12px;
            background: linear-gradient(to top, #1890ff, #69c0ff);
            border-radius: 6px 6px 0 0;
            transition: height 0.3s ease;
            min-height: 4px;
            position: relative;
            
            &:hover::after {
              content: attr(data-count);
              position: absolute;
              top: -25px;
              left: 50%;
              transform: translateX(-50%);
              background: rgba(0,0,0,0.7);
              color: white;
              padding: 2px 6px;
              border-radius: 4px;
              font-size: 12px;
            }
          }
          .date { font-size: 12px; color: #999; transform: rotate(-45deg); margin-top: 10px; }
        }
      }

      .top-list {
        display: flex;
        flex-direction: column;
        gap: 15px;

        .top-item {
          display: flex;
          align-items: center;
          gap: 10px;

          .rank {
            width: 24px;
            height: 24px;
            background: #f0f0f0;
            border-radius: 50%;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 12px;
            font-weight: bold;
            color: #666;
            
            &.top-3 { background: #fff1f0; color: #cf1322; }
          }
          .name { width: 60px; font-weight: 500; }
          .bar-wrapper {
            flex: 1;
            height: 8px;
            background: #f5f5f5;
            border-radius: 4px;
            overflow: hidden;

            .bar {
              height: 100%;
              background: #52c41a;
              border-radius: 4px;
            }
          }
          .count { width: 40px; text-align: right; color: #999; font-size: 12px; }
        }
      }
    }
  }
}
</style>
