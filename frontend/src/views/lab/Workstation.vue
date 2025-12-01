<template>
  <div class="lab-workstation">
    <div class="page-header"><h2>检验工作台</h2></div>
    
    <a-table :dataSource="orders" :columns="columns" rowKey="orderId">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'price'">
          {{ record.price }}
        </template>
        <template v-if="column.key === 'status'">
          <a-tag :color="record.status === 1 ? 'warning' : 'success'">
            {{ record.status === 1 ? '待检验' : '已完成' }}
          </a-tag>
        </template>
        <template v-if="column.key === 'action'">
          <a-button type="link" @click="showResultDialog(record)" v-if="record.status === 1">
            录入结果
          </a-button>
        </template>
      </template>
    </a-table>
    
    <!-- Result Dialog -->
    <a-modal v-model:open="dialogVisible" title="录入检验结果" @ok="submitResult" :confirmLoading="submitting">
      <a-form :model="resultForm" :label-col="{ span: 4 }" :wrapper-col="{ span: 20 }">
        <a-form-item label="检查项目">{{ currentOrder?.itemName }}</a-form-item>
        <a-form-item label="检验结果">
          <a-textarea v-model:value="resultForm.resultText" :rows="6" placeholder="请输入检验结果" />
        </a-form-item>
        <a-form-item label="图片链接">
          <a-input v-model:value="resultForm.resultImages" placeholder="图片URL，多个用逗号分隔" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { labApi } from '@/utils/api'

const orders = ref([])
const dialogVisible = ref(false)
const currentOrder = ref(null)
const submitting = ref(false)
const resultForm = reactive({ resultText: '', resultImages: '' })

const columns = [
  { title: '单号', dataIndex: 'orderId', key: 'orderId', width: 80 },
  { title: '检查项目', dataIndex: 'itemName', key: 'itemName' },
  { title: '价格', dataIndex: 'price', key: 'price' },
  { title: '状态', dataIndex: 'status', key: 'status' },
  { title: '操作', key: 'action', width: 100 }
]

onMounted(() => loadOrders())

const loadOrders = async () => {
  try { orders.value = await labApi.getPending() } catch (e) { console.error(e) }
}

const showResultDialog = (order) => {
  currentOrder.value = order
  resultForm.resultText = ''
  resultForm.resultImages = ''
  dialogVisible.value = true
}

const submitResult = async () => {
  submitting.value = true
  try {
    await labApi.submitResult({
      orderId: currentOrder.value.orderId,
      resultText: resultForm.resultText,
      resultImages: resultForm.resultImages
    })
    message.success('结果已提交')
    dialogVisible.value = false
    loadOrders()
  } catch (e) { console.error(e) } finally { submitting.value = false }
}
</script>

<style scoped lang="scss">
.lab-workstation { padding: 20px; .page-header { margin-bottom: 20px; h2 { margin: 0; } } }
</style>
