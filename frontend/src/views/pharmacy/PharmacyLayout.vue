<template>
  <div class="pharmacy-layout">
    <div class="sidebar" v-if="isLoggedIn">
      <div class="logo"><span>💊</span> 药房</div>
      <el-menu :default-active="activeMenu" @select="handleMenuSelect">
        <el-menu-item index="dispense"><el-icon><Tickets /></el-icon><span>发药窗口</span></el-menu-item>
        <el-menu-item index="inventory"><el-icon><Box /></el-icon><span>库存管理</span></el-menu-item>
      </el-menu>
      <div class="logout" @click="logout"><el-icon><SwitchButton /></el-icon> 退出登录</div>
    </div>
    <div class="main-content"><router-view /></div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Tickets, Box, SwitchButton } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const isLoggedIn = computed(() => userStore.isLoggedIn && route.name !== 'PharmacyLogin')
const activeMenu = computed(() => route.name === 'Inventory' ? 'inventory' : 'dispense')
const handleMenuSelect = (key) => router.push(`/pharmacy/${key}`)
const logout = () => { userStore.logout(); router.push('/pharmacy/login') }
</script>

<style scoped lang="scss">
.pharmacy-layout {
  display: flex;
  height: 100vh;
  .sidebar {
    width: 220px;
    background: #304156;
    display: flex;
    flex-direction: column;
    .logo { height: 60px; display: flex; align-items: center; justify-content: center; color: #fff; font-size: 20px; font-weight: bold; span { margin-right: 8px; } }
    .el-menu { flex: 1; border: none; background: transparent; .el-menu-item { color: rgba(255,255,255,0.8); &:hover, &.is-active { background: rgba(255,255,255,0.1); color: #fff; } } }
    .logout { padding: 15px 20px; color: rgba(255,255,255,0.6); cursor: pointer; display: flex; align-items: center; gap: 8px; &:hover { color: #fff; } }
  }
  .main-content { flex: 1; overflow: auto; background: #f5f7fa; }
}
</style>
