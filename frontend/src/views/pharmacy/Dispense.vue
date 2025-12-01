<template>
  <div class="dispense-page">
    <div class="page-header"><h2>发药窗口</h2></div>
    
    <el-table :data="prescriptions" stripe>
      <el-table-column prop="presId" label="处方号" width="80" />
      <el-table-column prop="recordId" label="病历号" width="80" />
      <el-table-column prop="drugId" label="药品ID" width="80" />
      <el-table-column prop="quantity" label="数量" width="80" />
      <el-table-column prop="usageInstruction" label="用法用量" />
      <el-table-column label="操作" width="100">
        <template #default="{ row }">
          <el-button type="primary" @click="dispense(row)" :loading="dispensing === row.presId">
            发药
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { pharmacyApi } from '@/utils/api'

const prescriptions = ref([])
const dispensing = ref(null)

onMounted(() => loadPrescriptions())

const loadPrescriptions = async () => {
  try { prescriptions.value = await pharmacyApi.getPaidPrescriptions() } catch (e) { console.error(e) }
}

const dispense = async (pres) => {
  dispensing.value = pres.presId
  try {
    await pharmacyApi.dispense(pres.presId)
    ElMessage.success('发药成功')
    loadPrescriptions()
  } catch (e) { console.error(e) } finally { dispensing.value = null }
}
</script>

<style scoped lang="scss">
.dispense-page { padding: 20px; .page-header { margin-bottom: 20px; h2 { margin: 0; } } }
</style>
