<template>
  <div class="patient-layout">
    <router-view />
    
    <!-- Bottom Navigation -->
    <van-tabbar v-if="showTabbar" v-model="activeTab" @change="onTabChange">
      <van-tabbar-item name="home" icon="home-o">首页</van-tabbar-item>
      <van-tabbar-item name="appointment" icon="calendar-o">挂号</van-tabbar-item>
      <van-tabbar-item name="payment" icon="card">缴费</van-tabbar-item>
      <van-tabbar-item name="records" icon="notes-o">病历</van-tabbar-item>
    </van-tabbar>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Tabbar as VanTabbar, TabbarItem as VanTabbarItem } from 'vant'
import 'vant/lib/index.css'

const route = useRoute()
const router = useRouter()
const activeTab = ref('home')

const hideTabbarRoutes = ['PatientLogin', 'PatientRegister', 'Triage']
const showTabbar = computed(() => !hideTabbarRoutes.includes(route.name))

watch(() => route.name, (name) => {
  const tabMap = {
    'PatientHome': 'home',
    'Appointment': 'appointment',
    'Payment': 'payment',
    'Records': 'records',
    'Reports': 'records',
  }
  if (tabMap[name]) {
    activeTab.value = tabMap[name]
  }
})

const onTabChange = (name) => {
  const routeMap = {
    'home': '/patient/home',
    'appointment': '/patient/appointment',
    'payment': '/patient/payment',
    'records': '/patient/records',
  }
  router.push(routeMap[name])
}
</script>

<style scoped lang="scss">
.patient-layout {
  min-height: 100vh;
  background: #f5f7fa;
  padding-bottom: 50px;
}
</style>
