<template>
  <div class="inventory-page">
    <div class="page-header">
      <h2>库存管理</h2>
      <a-button type="primary" @click="showAddDialog">新增药品</a-button>
    </div>
    
    <a-table :dataSource="drugs" :columns="columns" rowKey="drugId">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'price'">
          ¥{{ record.price }}
        </template>
        <template v-if="column.key === 'stockQuantity'">
          <a-tag :color="record.stockQuantity < 100 ? 'error' : 'success'">{{ record.stockQuantity }}</a-tag>
        </template>
        <template v-if="column.key === 'action'">
          <a-button type="link" @click="showStockDialog(record)">入库</a-button>
        </template>
      </template>
    </a-table>
    
    <!-- Add Drug Dialog -->
    <a-modal v-model:open="addDialogVisible" title="新增药品" @ok="addDrug">
      <a-form :model="drugForm" :label-col="{ span: 4 }" :wrapper-col="{ span: 20 }">
        <a-form-item label="名称"><a-input v-model:value="drugForm.name" /></a-form-item>
        <a-form-item label="规格"><a-input v-model:value="drugForm.spec" /></a-form-item>
        <a-form-item label="单价"><a-input-number v-model:value="drugForm.price" :min="0" :precision="2" style="width: 100%" /></a-form-item>
        <a-form-item label="库存"><a-input-number v-model:value="drugForm.stockQuantity" :min="0" style="width: 100%" /></a-form-item>
        <a-form-item label="单位"><a-input v-model:value="drugForm.unit" /></a-form-item>
        <a-form-item label="厂家"><a-input v-model:value="drugForm.manufacturer" /></a-form-item>
      </a-form>
    </a-modal>
    
    <!-- Stock Dialog -->
    <a-modal v-model:open="stockDialogVisible" title="入库" @ok="updateStock">
      <a-form :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
        <a-form-item label="药品">{{ currentDrug?.name }}</a-form-item>
        <a-form-item label="当前库存">{{ currentDrug?.stockQuantity }}</a-form-item>
        <a-form-item label="入库数量"><a-input-number v-model:value="stockQuantity" :min="1" style="width: 100%" /></a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { pharmacyApi } from '@/utils/api'

const drugs = ref([])
const addDialogVisible = ref(false)
const stockDialogVisible = ref(false)
const currentDrug = ref(null)
const stockQuantity = ref(100)

const drugForm = reactive({ name: '', spec: '', price: 0, stockQuantity: 0, unit: '', manufacturer: '' })

const columns = [
  { title: 'ID', dataIndex: 'drugId', key: 'drugId', width: 60 },
  { title: '药品名称', dataIndex: 'name', key: 'name' },
  { title: '规格', dataIndex: 'spec', key: 'spec' },
  { title: '单价', dataIndex: 'price', key: 'price' },
  { title: '库存', dataIndex: 'stockQuantity', key: 'stockQuantity' },
  { title: '单位', dataIndex: 'unit', key: 'unit', width: 80 },
  { title: '操作', key: 'action', width: 100 }
]

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
    message.success('添加成功')
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
    message.success('入库成功')
    stockDialogVisible.value = false
    loadDrugs()
  } catch (e) { console.error(e) }
}
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
</style>
