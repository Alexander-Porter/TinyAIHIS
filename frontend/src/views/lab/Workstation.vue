<template>
  <div class="lab-workstation">
    <div class="page-header"><h2>检验工作台</h2></div>
    
    <el-table :data="orders" stripe>
      <el-table-column prop="orderId" label="单号" width="80" />
      <el-table-column prop="itemName" label="检查项目" />
      <el-table-column prop="price" label="价格">
        <template #default="{ row }">¥{{ row.price }}</template>
      </el-table-column>
      <el-table-column prop="status" label="状态">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'warning' : 'success'">
            {{ row.status === 1 ? '待检验' : '已完成' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100">
        <template #default="{ row }">
          <el-button type="primary" text @click="showResultDialog(row)" v-if="row.status === 1">
            录入结果
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    
    <!-- Result Dialog -->
    <el-dialog v-model="dialogVisible" title="录入检验结果" width="600px">
      <el-form :model="resultForm" label-width="100px">
        <el-form-item label="检查项目">{{ currentOrder?.itemName }}</el-form-item>
        <el-form-item label="检验结果">
          <el-input v-model="resultForm.resultText" type="textarea" rows="6" placeholder="请输入检验结果" />
        </el-form-item>
        <el-form-item label="图片链接">
          <el-input v-model="resultForm.resultImages" placeholder="图片URL，多个用逗号分隔" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitResult" :loading="submitting">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { labApi } from '@/utils/api'

const orders = ref([])
const dialogVisible = ref(false)
const currentOrder = ref(null)
const submitting = ref(false)
const resultForm = reactive({ resultText: '', resultImages: '' })

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
    ElMessage.success('结果已提交')
    dialogVisible.value = false
    loadOrders()
  } catch (e) { console.error(e) } finally { submitting.value = false }
}
</script>

<style scoped lang="scss">
.lab-workstation { padding: 20px; .page-header { margin-bottom: 20px; h2 { margin: 0; } } }
</style>
