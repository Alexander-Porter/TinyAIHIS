<template>
  <div class="portal">
    <div class="header">
      <h1>🏥 TinyHIS</h1>
      <p>智慧医院信息系统</p>
    </div>
    
    <div class="entries">
      <div class="entry-card" @click="$router.push('/patient')">
        <div class="icon">👤</div>
        <div class="title">患者端</div>
        <div class="desc">预约挂号、缴费、签到</div>
      </div>
      
      <div class="entry-card" @click="$router.push('/doctor/login')">
        <div class="icon">👨‍⚕️</div>
        <div class="title">医生工作站</div>
        <div class="desc">接诊、病历、处方</div>
      </div>
      
      <div class="entry-card" @click="$router.push('/lab/login')">
        <div class="icon">🔬</div>
        <div class="title">检验科</div>
        <div class="desc">检验报告录入</div>
      </div>
      
      <div class="entry-card" @click="$router.push('/pharmacy/login')">
        <div class="icon">💊</div>
        <div class="title">药房</div>
        <div class="desc">发药、库存管理</div>
      </div>
      
      <div class="entry-card" @click="$router.push('/admin/login')">
        <div class="icon">⚙️</div>
        <div class="title">管理后台</div>
        <div class="desc">系统管理、排班、审计</div>
      </div>
      
      <div class="entry-card" @click="showScreenSelector = true">
        <div class="icon">📺</div>
        <div class="title">叫号大屏</div>
        <div class="desc">科室候诊显示</div>
      </div>
    </div>
    
    <!-- Screen Selector Dialog -->
    <el-dialog v-model="showScreenSelector" title="选择科室" width="400px">
      <el-select v-model="selectedDept" placeholder="请选择科室" style="width: 100%">
        <el-option v-for="dept in departments" :key="dept.deptId" :label="dept.deptName" :value="dept.deptId" />
      </el-select>
      <template #footer>
        <el-button @click="showScreenSelector = false">取消</el-button>
        <el-button type="primary" @click="openScreen">打开大屏</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { scheduleApi } from '@/utils/api'

const router = useRouter()
const showScreenSelector = ref(false)
const selectedDept = ref(null)
const departments = ref([])

onMounted(async () => {
  try {
    departments.value = await scheduleApi.getDepartments()
  } catch (e) {
    console.error('Failed to load departments', e)
  }
})

const openScreen = () => {
  if (selectedDept.value) {
    window.open(`/screen/${selectedDept.value}`, '_blank')
    showScreenSelector.value = false
  }
}
</script>

<style scoped lang="scss">
.portal {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 40px 20px;
  
  .header {
    text-align: center;
    color: #fff;
    margin-bottom: 40px;
    
    h1 {
      font-size: 48px;
      margin: 0 0 10px;
    }
    
    p {
      font-size: 18px;
      opacity: 0.9;
    }
  }
  
  .entries {
    max-width: 1000px;
    margin: 0 auto;
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
    gap: 20px;
  }
  
  .entry-card {
    background: rgba(255, 255, 255, 0.95);
    border-radius: 16px;
    padding: 30px;
    text-align: center;
    cursor: pointer;
    transition: all 0.3s;
    
    &:hover {
      transform: translateY(-5px);
      box-shadow: 0 10px 30px rgba(0, 0, 0, 0.2);
    }
    
    .icon {
      font-size: 48px;
      margin-bottom: 15px;
    }
    
    .title {
      font-size: 20px;
      font-weight: bold;
      color: #333;
      margin-bottom: 8px;
    }
    
    .desc {
      font-size: 14px;
      color: #666;
    }
  }
}
</style>
