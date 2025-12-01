<template>
  <div class="payment-page">
    <van-nav-bar title="门诊缴费" left-arrow @click-left="$router.back()" />
    
    <van-tabs v-model:active="activeTab">
      <van-tab title="待缴费">
        <div class="list">
          <van-empty v-if="pendingItems.length === 0" description="暂无待缴费项目" />
          
          <div class="pay-item" v-for="item in pendingItems" :key="item.id">
            <div class="info">
              <div class="type">{{ item.type }}</div>
              <div class="desc">{{ item.desc }}</div>
            </div>
            <div class="amount">¥{{ item.amount }}</div>
            <van-button size="small" type="primary" @click="pay(item)">缴费</van-button>
          </div>
        </div>
      </van-tab>
      
      <van-tab title="已缴费">
        <div class="list">
          <van-empty v-if="paidItems.length === 0" description="暂无已缴费项目" />
          
          <div class="pay-item paid" v-for="item in paidItems" :key="item.id">
            <div class="info">
              <div class="type">{{ item.type }}</div>
              <div class="desc">{{ item.desc }}</div>
            </div>
            <div class="amount">¥{{ item.amount }}</div>
            <div class="status">已缴费</div>
          </div>
        </div>
      </van-tab>
    </van-tabs>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { NavBar as VanNavBar, Tabs as VanTabs, Tab as VanTab, Button as VanButton, Empty as VanEmpty, showToast, showLoadingToast, closeToast } from 'vant'
import { registrationApi, paymentApi, labApi, emrApi } from '@/utils/api'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const userStore = useUserStore()
const activeTab = ref(0)

const pendingItems = ref([])
const paidItems = ref([])

onMounted(() => {
  loadPaymentItems()
})

const loadPaymentItems = async () => {
  const patientId = userStore.userInfo.patientId || userStore.userInfo.userId
  
  try {
    // Load registrations
    const regs = await registrationApi.getByPatient(patientId)
    
    for (const reg of regs) {
      if (reg.status === 0) {
        pendingItems.value.push({
          id: `reg-${reg.regId}`,
          type: '挂号费',
          desc: `挂号单 #${reg.regId}`,
          amount: reg.fee || 50,
          payType: 'registration',
          payId: reg.regId
        })
      } else if (reg.status >= 1) {
        paidItems.value.push({
          id: `reg-${reg.regId}`,
          type: '挂号费',
          desc: `挂号单 #${reg.regId}`,
          amount: reg.fee || 50
        })
      }
      
      // Load prescriptions and lab orders for each registration
      try {
        const record = await emrApi.getByReg(reg.regId)
        if (record) {
          const prescriptions = await emrApi.getPrescriptions(record.recordId)
          const labOrders = await emrApi.getLabOrders(record.recordId)
          
          for (const pres of prescriptions) {
            if (pres.status === 0) {
              pendingItems.value.push({
                id: `pres-${pres.presId}`,
                type: '药费',
                desc: `${pres.drugName || '药品'} x${pres.quantity}`,
                amount: pres.totalPrice || 0,
                payType: 'prescription',
                payId: record.recordId
              })
            } else {
              paidItems.value.push({
                id: `pres-${pres.presId}`,
                type: '药费',
                desc: `${pres.drugName || '药品'} x${pres.quantity}`,
                amount: pres.totalPrice || 0
              })
            }
          }
          
          for (const order of labOrders) {
            if (order.status === 0) {
              pendingItems.value.push({
                id: `lab-${order.orderId}`,
                type: '检查费',
                desc: order.itemName,
                amount: order.price || 100,
                payType: 'lab',
                payId: order.orderId
              })
            } else {
              paidItems.value.push({
                id: `lab-${order.orderId}`,
                type: '检查费',
                desc: order.itemName,
                amount: order.price || 100
              })
            }
          }
        }
      } catch (e) {
        // No record yet
      }
    }
  } catch (e) {
    console.error('Failed to load payment items', e)
  }
}

const pay = async (item) => {
  showLoadingToast({ message: '缴费中...', forbidClick: true })
  
  try {
    if (item.payType === 'registration') {
      await paymentApi.payRegistration(item.payId)
    } else if (item.payType === 'prescription') {
      await paymentApi.payPrescriptions(item.payId)
    } else if (item.payType === 'lab') {
      await paymentApi.payLabOrder(item.payId)
    }
    
    closeToast()
    showToast('缴费成功')
    
    // Move item to paid list
    const idx = pendingItems.value.findIndex(i => i.id === item.id)
    if (idx > -1) {
      paidItems.value.push(pendingItems.value[idx])
      pendingItems.value.splice(idx, 1)
    }
  } catch (e) {
    closeToast()
    console.error('Payment failed', e)
  }
}
</script>

<style scoped lang="scss">
.payment-page {
  min-height: 100vh;
  background: #f5f7fa;
  
  .list {
    padding: 15px;
  }
  
  .pay-item {
    background: #fff;
    border-radius: 8px;
    padding: 15px;
    margin-bottom: 10px;
    display: flex;
    align-items: center;
    gap: 10px;
    
    .info {
      flex: 1;
      
      .type {
        font-weight: 500;
        margin-bottom: 5px;
      }
      
      .desc {
        font-size: 13px;
        color: #666;
      }
    }
    
    .amount {
      font-size: 18px;
      font-weight: 500;
      color: #f56c6c;
    }
    
    &.paid {
      opacity: 0.7;
      
      .status {
        color: #67c23a;
        font-size: 13px;
      }
    }
  }
}
</style>
