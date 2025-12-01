<template>
  <div class="audit-page">
    <div class="page-header"><h2>审计日志</h2></div>
    
    <div class="filters">
      <el-date-picker v-model="dateRange" type="daterange" start-placeholder="开始日期" end-placeholder="结束日期" />
      <el-select v-model="filterType" placeholder="操作类型" clearable style="width: 150px">
        <el-option label="登录" value="LOGIN" />
        <el-option label="挂号" value="REGISTRATION" />
        <el-option label="病历" value="EMR" />
        <el-option label="处方" value="PRESCRIPTION" />
        <el-option label="缴费" value="PAYMENT" />
      </el-select>
      <el-button type="primary" @click="search">查询</el-button>
    </div>
    
    <el-table :data="logs" stripe>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="time" label="时间" width="160" />
      <el-table-column prop="user" label="操作人" width="100" />
      <el-table-column prop="type" label="类型" width="100" />
      <el-table-column prop="action" label="操作" />
      <el-table-column prop="ip" label="IP地址" width="120" />
    </el-table>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const dateRange = ref(null)
const filterType = ref('')
const logs = ref([
  { id: 1, time: '2024-12-01 09:00:00', user: 'admin', type: 'LOGIN', action: '管理员登录系统', ip: '192.168.1.1' },
  { id: 2, time: '2024-12-01 09:15:23', user: 'doctor1', type: 'EMR', action: '创建病历 #123', ip: '192.168.1.2' },
  { id: 3, time: '2024-12-01 09:30:45', user: 'patient', type: 'REGISTRATION', action: '患者挂号 #456', ip: '192.168.1.3' },
  { id: 4, time: '2024-12-01 10:00:00', user: 'pharmacy', type: 'PRESCRIPTION', action: '发药处方 #789', ip: '192.168.1.4' },
])

const search = () => {
  // Implement search
}
</script>

<style scoped lang="scss">
.audit-page { padding: 20px; .page-header { margin-bottom: 20px; h2 { margin: 0; } } .filters { display: flex; gap: 10px; margin-bottom: 20px; } }
</style>
