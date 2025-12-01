<template>
  <div class="inventory-page">
    <div class="page-header">
      <h2>库存管理</h2>
      <el-button type="primary" @click="showAddDialog">新增药品</el-button>
    </div>
    
    <el-table :data="drugs" stripe>
      <el-table-column prop="drugId" label="ID" width="60" />
      <el-table-column prop="name" label="药品名称" />
      <el-table-column prop="spec" label="规格" />
      <el-table-column prop="price" label="单价">
        <template #default="{ row }">¥{{ row.price }}</template>
      </el-table-column>
      <el-table-column prop="stockQuantity" label="库存">
        <template #default="{ row }">
          <el-tag :type="row.stockQuantity < 100 ? 'danger' : 'success'">{{ row.stockQuantity }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="unit" label="单位" width="80" />
      <el-table-column label="操作" width="100">
        <template #default="{ row }">
          <el-button type="primary" text @click="showStockDialog(row)">入库</el-button>
        </template>
      </el-table-column>
    </el-table>
    
    <!-- Add Drug Dialog -->
    <el-dialog v-model="addDialogVisible" title="新增药品" width="500px">
      <el-form :model="drugForm" label-width="80px">
        <el-form-item label="名称"><el-input v-model="drugForm.name" /></el-form-item>
        <el-form-item label="规格"><el-input v-model="drugForm.spec" /></el-form-item>
        <el-form-item label="单价"><el-input-number v-model="drugForm.price" :min="0" :precision="2" /></el-form-item>
        <el-form-item label="库存"><el-input-number v-model="drugForm.stockQuantity" :min="0" /></el-form-item>
        <el-form-item label="单位"><el-input v-model="drugForm.unit" /></el-form-item>
        <el-form-item label="厂家"><el-input v-model="drugForm.manufacturer" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="addDrug">保存</el-button>
      </template>
    </el-dialog>
    
    <!-- Stock Dialog -->
    <el-dialog v-model="stockDialogVisible" title="入库" width="400px">
      <el-form label-width="80px">
        <el-form-item label="药品">{{ currentDrug?.name }}</el-form-item>
        <el-form-item label="当前库存">{{ currentDrug?.stockQuantity }}</el-form-item>
        <el-form-item label="入库数量"><el-input-number v-model="stockQuantity" :min="1" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="stockDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="updateStock">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { pharmacyApi } from '@/utils/api'

const drugs = ref([])
const addDialogVisible = ref(false)
const stockDialogVisible = ref(false)
const currentDrug = ref(null)
const stockQuantity = ref(100)

const drugForm = reactive({ name: '', spec: '', price: 0, stockQuantity: 0, unit: '', manufacturer: '' })

onMounted(() => loadDrugs())

const loadDrugs = async () => {
  try { drugs.value = await pharmacyApi.getDrugs() } catch (e) { console.error(e) }
}

const showAddDialog = () => {
  Object.assign(drugForm, { name: '', spec: '', price: 0, stockQuantity: 0, unit: '', manufacturer: '' })
  addDialogVisible.value = true
}

const addDrug = async () => {
  try {
    await pharmacyApi.addDrug(drugForm)
    ElMessage.success('添加成功')
    addDialogVisible.value = false
    loadDrugs()
  } catch (e) { console.error(e) }
}

const showStockDialog = (drug) => {
  currentDrug.value = drug
  stockQuantity.value = 100
  stockDialogVisible.value = true
}

const updateStock = async () => {
  try {
    await pharmacyApi.updateStock(currentDrug.value.drugId, stockQuantity.value)
    ElMessage.success('入库成功')
    stockDialogVisible.value = false
    loadDrugs()
  } catch (e) { console.error(e) }
}
</script>

<style scoped lang="scss">
.inventory-page { padding: 20px; .page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; h2 { margin: 0; } } }
</style>
