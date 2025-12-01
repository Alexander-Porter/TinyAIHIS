<template>
  <div class="lab-layout">
    <div class="sidebar" v-if="isLoggedIn">
      <div class="logo">
        <div class="logo-icon"><ExperimentOutlined /></div>
        <span>检验科</span>
      </div>
      <a-menu :selectedKeys="['workstation']" mode="inline">
        <a-menu-item key="workstation">
          <template #icon><DesktopOutlined /></template>
          <span>检验工作台</span>
        </a-menu-item>
      </a-menu>
      <div class="logout" @click="logout">
        <LogoutOutlined /> 退出登录
      </div>
    </div>
    <div class="main-content">
      <router-view />
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { DesktopOutlined, LogoutOutlined, ExperimentOutlined } from '@ant-design/icons-vue'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const isLoggedIn = computed(() => userStore.isLoggedIn && route.name !== 'LabLogin')

const logout = () => {
  userStore.logout()
  router.push('/lab/login')
}
</script>

<style scoped lang="scss">
.lab-layout {
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
      
      &:hover {
        color: var(--error-color);
      }
    }
  }
  
  .main-content {
    flex: 1;
    overflow: auto;
    background: var(--bg-body);
  }
}
</style>
