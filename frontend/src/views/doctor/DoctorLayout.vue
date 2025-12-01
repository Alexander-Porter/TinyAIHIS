<template>
  <div class="doctor-layout">
    <div class="sidebar" v-if="isLoggedIn">
      <div class="logo">
        <span>🏥</span> TinyHIS
      </div>
      <div class="user-info">
        <div class="name">{{ userStore.userInfo.realName }}</div>
        <div class="role">{{ userStore.userInfo.role === 'CHIEF' ? '科室主任' : '医生' }}</div>
      </div>
      <el-menu :default-active="activeMenu" @select="handleMenuSelect">
        <el-menu-item index="workstation">
          <el-icon><Monitor /></el-icon>
          <span>接诊工作台</span>
        </el-menu-item>
        <el-menu-item index="templates" v-if="userStore.userInfo.role === 'CHIEF'">
          <el-icon><Document /></el-icon>
          <span>模板管理</span>
        </el-menu-item>
      </el-menu>
      <div class="logout" @click="logout">
        <el-icon><SwitchButton /></el-icon> 退出登录
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
import { Monitor, Document, SwitchButton } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const isLoggedIn = computed(() => userStore.isLoggedIn && route.name !== 'DoctorLogin')
const activeMenu = computed(() => {
  if (route.name === 'Workstation') return 'workstation'
  if (route.name === 'Templates') return 'templates'
  return 'workstation'
})

const handleMenuSelect = (key) => {
  router.push(`/doctor/${key}`)
}

const logout = () => {
  userStore.logout()
  router.push('/doctor/login')
}
</script>

<style scoped lang="scss">
.doctor-layout {
  display: flex;
  height: 100vh;
  
  .sidebar {
    width: 220px;
    background: #304156;
    display: flex;
    flex-direction: column;
    
    .logo {
      height: 60px;
      display: flex;
      align-items: center;
      justify-content: center;
      color: #fff;
      font-size: 20px;
      font-weight: bold;
      
      span {
        margin-right: 8px;
      }
    }
    
    .user-info {
      padding: 15px 20px;
      border-bottom: 1px solid rgba(255,255,255,0.1);
      color: #fff;
      
      .name {
        font-size: 16px;
        font-weight: 500;
      }
      
      .role {
        font-size: 12px;
        opacity: 0.7;
        margin-top: 5px;
      }
    }
    
    .el-menu {
      flex: 1;
      border: none;
      background: transparent;
      
      .el-menu-item {
        color: rgba(255,255,255,0.8);
        
        &:hover, &.is-active {
          background: rgba(255,255,255,0.1);
          color: #fff;
        }
      }
    }
    
    .logout {
      padding: 15px 20px;
      color: rgba(255,255,255,0.6);
      cursor: pointer;
      display: flex;
      align-items: center;
      gap: 8px;
      
      &:hover {
        color: #fff;
      }
    }
  }
  
  .main-content {
    flex: 1;
    overflow: auto;
    background: #f5f7fa;
  }
}
</style>
