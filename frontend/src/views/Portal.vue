<template>
  <div class="portal">
    <div class="portal-container">
      <div class="header">
        <div class="logo-icon">
          <MedicineBoxOutlined style="font-size: 1em;"/>
        </div>
        <h1>TinyAIHIS</h1>
        <p>智慧医疗  极简体验</p>
      </div>
      
      <div class="entries-grid">
        <div class="entry-card" @click="router.push('/patient')">
          <div class="icon-wrapper patient">
            <UserOutlined style="font-size: 1em;"/>
          </div>
          <div class="card-content">
            <div class="title">患者服务</div>
            <div class="desc">预约挂号 / 缴费 / 报告查询</div>
          </div>
          <div class="arrow">
            <ArrowRightOutlined style="font-size: 1em;"/>
          </div>
        </div>
        
        <div class="entry-card" @click="router.push('/doctor/login')">
          <div class="icon-wrapper doctor">
            <MedicineBoxOutlined style="font-size: 1em;"/>
          </div>
          <div class="card-content">
            <div class="title">员工登录</div>
            <div class="desc">医生 / 药房 / 检验 / 管理员</div>
          </div>
          <div class="arrow">
            <ArrowRightOutlined style="font-size: 1em;"/>
          </div>
        </div>
        
        <div class="entry-card" @click="showScreenSelector = true">
          <div class="icon-wrapper screen">
            <DesktopOutlined style="font-size: 1em;"/>
          </div>
          <div class="card-content">
            <div class="title">候诊大屏</div>
            <div class="desc">科室队列 / 语音叫号 / 公示</div>
          </div>
          <div class="arrow">
            <ArrowRightOutlined style="font-size: 1em;"/>
          </div>
        </div>
      </div>
    </div>
    
    <!-- Screen Selector Dialog -->
    <a-modal 
      v-model:open="showScreenSelector" 
      title="选择科室" 
      width="400px"
      class="custom-dialog"
      centered
    >
      <a-select v-model:value="selectedDept" placeholder="请选择科室" style="width: 100%" size="large">
        <a-select-option v-for="dept in departments" :key="dept.deptId" :value="dept.deptId">{{ dept.deptName }}</a-select-option>
      </a-select>
      <template #footer>
        <div class="dialog-footer">
          <a-button @click="showScreenSelector = false">取消</a-button>
          <a-button type="primary" @click="openScreen">开启大屏</a-button>
        </div>
      </template>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { scheduleApi } from '@/utils/api'
import { 
  UserOutlined, MedicineBoxOutlined, ExperimentOutlined, ShopOutlined, SettingOutlined, DesktopOutlined, ArrowRightOutlined 
} from '@ant-design/icons-vue'

const router = useRouter()
const showScreenSelector = ref(false)
const selectedDept = ref(undefined)
const departments = ref([])

onMounted(async () => {
  try {
    departments.value = await scheduleApi.getDepartments()
  } catch (error) {
    console.error('Failed to load departments:', error)
  }
})

const openScreen = () => {
  if (selectedDept.value) {
    router.push(`/screen/${selectedDept.value}`)
  }
}
</script>

<style scoped lang="scss">
.portal {
  min-height: 100vh;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e7eb 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  
  .portal-container {
    width: 100%;
    max-width: 1200px;
    
    .header {
      text-align: center;
      margin-bottom: 60px;
      
      .logo-icon {
        width: 64px;
        height: 64px;
        background: var(--primary-color);
        color: #fff;
        border-radius: 16px;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 32px;
        margin: 0 auto 24px;
        box-shadow: 0 10px 20px rgba(0, 102, 204, 0.2);
      }
      
      h1 {
        font-size: 2.5rem;
        color: var(--text-primary);
        margin-bottom: 12px;
        font-weight: 700;
        letter-spacing: -0.5px;
      }
      
      p {
        font-size: 1.125rem;
        color: var(--text-secondary);
        font-weight: 400;
      }
    }
    
    .entries-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(340px, 1fr));
      gap: 24px;
      
      .entry-card {
        background: #fff;
        border-radius: 16px;
        padding: 32px;
        display: flex;
        align-items: center;
        cursor: pointer;
        transition: all 0.3s ease;
        border: 1px solid rgba(0,0,0,0.04);
        box-shadow: 0 4px 6px rgba(0,0,0,0.02);
        
        &:hover {
          transform: translateY(-4px);
          box-shadow: 0 20px 40px rgba(0,0,0,0.08);
          border-color: transparent;
          
          .arrow {
            opacity: 1;
            transform: translateX(0);
          }
        }
        
        .icon-wrapper {
          width: 64px;
          height: 64px;
          border-radius: 16px;
          display: flex;
          align-items: center;
          justify-content: center;
          font-size: 28px;
          margin-right: 24px;
          flex-shrink: 0;
          
          &.patient { background: rgba(0, 102, 204, 0.1); color: var(--primary-color); }
          &.doctor { background: rgba(16, 185, 129, 0.1); color: var(--success-color); }
          &.lab { background: rgba(245, 158, 11, 0.1); color: var(--warning-color); }
          &.pharmacy { background: rgba(139, 92, 246, 0.1); color: var(--accent-color); }
          &.admin { background: rgba(107, 114, 128, 0.1); color: var(--text-secondary); }
          &.screen { background: rgba(236, 72, 153, 0.1); color: #ec4899; }
        }
        
        .card-content {
          flex: 1;
          
          .title {
            font-size: 1.25rem;
            font-weight: 600;
            color: var(--text-primary);
            margin-bottom: 6px;
          }
          
          .desc {
            font-size: 0.875rem;
            color: var(--text-secondary);
          }
        }
        
        .arrow {
          color: var(--text-placeholder);
          font-size: 20px;
          opacity: 0;
          transform: translateX(-10px);
          transition: all 0.3s ease;
        }
      }
    }
  }
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 24px;
}
</style>
