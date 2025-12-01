<template>
  <div class="admin-layout">
    <div class="sidebar" v-if="isLoggedIn">
      <div class="logo">
        <div class="logo-icon"><SettingOutlined /></div>
        <span>管理后台</span>
      </div>
      <a-menu :selectedKeys="[activeMenu]" mode="inline" @click="handleMenuSelect">
        <a-menu-item key="dashboard"><template #icon><DashboardOutlined /></template><span>仪表盘</span></a-menu-item>
        <a-menu-item key="users"><template #icon><UserOutlined /></template><span>用户管理</span></a-menu-item>
        <a-menu-item key="departments"><template #icon><BankOutlined /></template><span>科室管理</span></a-menu-item>
        <a-menu-item key="schedules"><template #icon><CalendarOutlined /></template><span>排班管理</span></a-menu-item>
        <a-menu-item key="audit"><template #icon><FileTextOutlined /></template><span>审计日志</span></a-menu-item>
      </a-menu>
      <div class="logout" @click="logout"><LogoutOutlined /> 退出登录</div>
    </div>
    <div class="main-content"><router-view /></div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { DashboardOutlined, UserOutlined, BankOutlined, CalendarOutlined, FileTextOutlined, LogoutOutlined, SettingOutlined } from '@ant-design/icons-vue'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const isLoggedIn = computed(() => userStore.isLoggedIn && route.name !== 'AdminLogin')
const activeMenu = computed(() => {
  const map = { 'Dashboard': 'dashboard', 'Users': 'users', 'Departments': 'departments', 'Schedules': 'schedules', 'Audit': 'audit' }
  return map[route.name] || 'dashboard'
})
const handleMenuSelect = ({ key }) => router.push(/admin/)
const logout = () => { userStore.logout(); router.push('/admin/login') }
</script>

<style scoped lang="scss">
.admin-layout {
  display: flex;
  height: 100vh;
  background-color: var(--bg-body);

  .sidebar {
    width: 260px;
    background: var(--bg-surface);
    border-right: 1px solid var(--border-color);
    display: flex;
    flex-direction: column;
    box-shadow: var(--shadow-sm);
    z-index: 10;

    .logo { 
      height: 80px; 
      display: flex; 
      align-items: center; 
      padding: 0 24px;
      color: var(--text-primary); 
      font-size: 1.125rem; 
      font-weight: 600; 
      border-bottom: 1px solid var(--border-color);
      
      .logo-icon {
        width: 32px;
        height: 32px;
        background: var(--primary-color);
        color: #fff;
        border-radius: 8px;
        display: flex;
        align-items: center;
        justify-content: center;
        margin-right: 12px;
      }
    }

    :deep(.ant-menu) { 
      flex: 1; 
      border: none; 
      background: transparent; 
      padding: 16px 0;
      
      .ant-menu-item { 
        height: 50px;
        line-height: 50px;
        margin: 4px 16px;
        border-radius: var(--radius-md);
        color: var(--text-secondary); 
        
        &:hover { 
          background: var(--bg-body); 
          color: var(--text-primary); 
        }
        
        &.ant-menu-item-selected { 
          background: rgba(0, 102, 204, 0.1); 
          color: var(--accent-color); 
          font-weight: 500;
        }
      } 
    }

    .logout { 
      padding: 20px; 
      cursor: pointer; 
      color: var(--text-secondary); 
      border-top: 1px solid var(--border-color);
      display: flex;
      align-items: center;
      gap: 10px;
      
      &:hover { color: var(--error-color); } 
    }
  }

  .main-content { 
    flex: 1; 
    overflow: auto; 
    background: var(--bg-body);
  }
}
</style>
