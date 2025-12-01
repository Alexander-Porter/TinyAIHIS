<template>
  <div class="dispense-page">
    <div class="page-header"><h2>发药窗口</h2></div>
    
    <a-table :dataSource="prescriptions" :columns="columns" rowKey="presId">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'action'">
          <a-button type="primary" @click="dispense(record)" :loading="dispensing === record.presId">
            发药
          </a-button>
        </template>
      </template>
    </a-table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { pharmacyApi } from '@/utils/api'

const prescriptions = ref([])
const dispensing = ref(null)

const columns = [
  { title: '处方号', dataIndex: 'presId', key: 'presId', width: 80 },
  { title: '病历号', dataIndex: 'recordId', key: 'recordId', width: 80 },
  { title: '药品ID', dataIndex: 'drugId', key: 'drugId', width: 80 },
  { title: '数量', dataIndex: 'quantity', key: 'quantity', width: 80 },
  { title: '用法用量', dataIndex: 'usageInstruction', key: 'usageInstruction' },
  { title: '操作', key: 'action', width: 100 }
]

onMounted(() => loadPrescriptions())

const loadPrescriptions = async () => {
  try { prescriptions.value = await pharmacyApi.getPaidPrescriptions() } catch (e) { console.error(e) }
}

const dispense = async (pres) => {
  dispensing.value = pres.presId
  try {
    await pharmacyApi.dispense(pres.presId)
    message.success('发药成功')
    loadPrescriptions()
  } catch (e) { console.error(e) } finally { dispensing.value = null }
}
</script>

<style scoped lang="scss">
.dispense-page { padding: 20px; .page-header { margin-bottom: 20px; h2 { margin: 0; } } }
</style>
