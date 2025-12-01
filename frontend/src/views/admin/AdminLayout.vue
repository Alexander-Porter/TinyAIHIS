<template>
  <div class="admin-layout">
    <div class="sidebar" v-if="isLoggedIn">
      <div class="logo"><span>⚙️</span> 管理后台</div>
      <el-menu :default-active="activeMenu" @select="handleMenuSelect">
        <el-menu-item index="dashboard"><el-icon><DataAnalysis /></el-icon><span>仪表盘</span></el-menu-item>
        <el-menu-item index="users"><el-icon><User /></el-icon><span>用户管理</span></el-menu-item>
        <el-menu-item index="departments"><el-icon><OfficeBuilding /></el-icon><span>科室管理</span></el-menu-item>
        <el-menu-item index="schedules"><el-icon><Calendar /></el-icon><span>排班管理</span></el-menu-item>
        <el-menu-item index="audit"><el-icon><Document /></el-icon><span>审计日志</span></el-menu-item>
      </el-menu>
      <div class="logout" @click="logout"><el-icon><SwitchButton /></el-icon> 退出登录</div>
    </div>
    <div class="main-content"><router-view /></div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { DataAnalysis, User, OfficeBuilding, Calendar, Document, SwitchButton } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const isLoggedIn = computed(() => userStore.isLoggedIn && route.name !== 'AdminLogin')
const activeMenu = computed(() => {
  const map = { 'Dashboard': 'dashboard', 'Users': 'users', 'Departments': 'departments', 'Schedules': 'schedules', 'Audit': 'audit' }
  return map[route.name] || 'dashboard'
})
const handleMenuSelect = (key) => router.push(`/admin/${key}`)
const logout = () => { userStore.logout(); router.push('/admin/login') }
</script>

<style scoped lang="scss">
.admin-layout {
  display: flex;
  height: 100vh;
  .sidebar {
    width: 220px;
    background: #304156;
    display: flex;
    flex-direction: column;
    .logo { height: 60px; display: flex; align-items: center; justify-content: center; color: #fff; font-size: 18px; font-weight: bold; span { margin-right: 8px; } }
    .el-menu { flex: 1; border: none; background: transparent; .el-menu-item { color: rgba(255,255,255,0.8); &:hover, &.is-active { background: rgba(255,255,255,0.1); color: #fff; } } }
    .logout { padding: 15px 20px; color: rgba(255,255,255,0.6); cursor: pointer; display: flex; align-items: center; gap: 8px; &:hover { color: #fff; } }
  }
  .main-content { flex: 1; overflow: auto; background: #f5f7fa; }
}
</style>
