<template>
  <div class="pharmacy-layout">
    <!-- Top Navigation Bar -->
    <div class="top-nav" v-if="isLoggedIn">
      <div class="nav-left">
        <div class="logo">
          <ShopOutlined />
          <span>药房工作站</span>
        </div>
        <div class="nav-menu">
          <div class="nav-item" :class="{ active: activeMenu === 'dispense' }" @click="router.push('/pharmacy/dispense')">
            <FileTextOutlined /> 发药窗口
          </div>
          <div class="nav-item" :class="{ active: activeMenu === 'inventory' }" @click="router.push('/pharmacy/inventory')">
            <InboxOutlined /> 库存管理
          </div>
        </div>
      </div>
      <div class="nav-right">
        <div class="user-info">
          <UserOutlined />
          <span>{{ userStore.userInfo.realName }}</span>
        </div>
        <a-button type="text" @click="logout"><LogoutOutlined /> 退出</a-button>
      </div>
    </div>
    <div class="main-content" :class="{ 'with-nav': isLoggedIn }"><router-view /></div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { FileTextOutlined, InboxOutlined, LogoutOutlined, ShopOutlined, UserOutlined } from '@ant-design/icons-vue'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const isLoggedIn = computed(() => userStore.isLoggedIn && route.name !== 'PharmacyLogin')
const activeMenu = computed(() => route.name === 'Inventory' ? 'inventory' : 'dispense')
const logout = () => { userStore.logout(); router.push('/pharmacy/login') }
</script>

<style scoped lang="scss">
.pharmacy-layout {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background-color: var(--bg-body);

  .top-nav {
    height: 56px;
    background: var(--bg-surface);
    border-bottom: 1px solid var(--border-color);
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0 24px;
    box-shadow: var(--shadow-sm);
    z-index: 100;
    flex-shrink: 0;

    .nav-left {
      display: flex;
      align-items: center;
      gap: 32px;

      .logo {
        display: flex;
        align-items: center;
        gap: 10px;
        font-size: 1.1rem;
        font-weight: 600;
        color: var(--primary-color);
      }

      .nav-menu {
        display: flex;
        gap: 8px;

        .nav-item {
          padding: 8px 16px;
          border-radius: var(--radius-md);
          cursor: pointer;
          color: var(--text-secondary);
          display: flex;
          align-items: center;
          gap: 6px;
          font-size: 0.9rem;
          transition: all 0.2s;

          &:hover {
            background: var(--bg-body);
            color: var(--text-primary);
          }

          &.active {
            background: rgba(0, 102, 204, 0.1);
            color: var(--accent-color);
            font-weight: 500;
          }
        }
      }
    }

    .nav-right {
      display: flex;
      align-items: center;
      gap: 16px;

      .user-info {
        display: flex;
        align-items: center;
        gap: 8px;
        color: var(--text-secondary);
        font-size: 0.9rem;
      }
    }
  }

  .main-content {
    flex: 1;
    overflow: auto;

    &.with-nav {
      height: calc(100vh - 56px);
    }
  }
}
</style>
