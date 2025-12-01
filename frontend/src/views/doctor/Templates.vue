<template>
  <div class="templates-page">
    <div class="page-header">
      <h2>模板管理</h2>
      <a-button type="primary" @click="showAddDialog">新增模板</a-button>
    </div>
    
    <a-table :dataSource="templates" :columns="columns" rowKey="tplId">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'type'">
          {{ record.type === 'EMR' ? '病历模板' : '处方套餐' }}
        </template>
        <template v-if="column.key === 'action'">
          <a-button type="link" @click="editTemplate(record)">编辑</a-button>
          <a-button type="link" danger @click="deleteTemplate(record)">删除</a-button>
        </template>
      </template>
    </a-table>
    
    <!-- Add/Edit Dialog -->
    <a-modal v-model:open="dialogVisible" :title="isEdit ? '编辑模板' : '新增模板'" @ok="saveTemplate" :confirmLoading="saving">
      <a-form :model="form" :label-col="{ span: 4 }" :wrapper-col="{ span: 20 }">
        <a-form-item label="模板名称">
          <a-input v-model:value="form.name" placeholder="请输入模板名称" />
        </el-form-item>
        <a-form-item label="模板类型">
          <a-select v-model:value="form.type" style="width: 100%">
            <a-select-option value="EMR">病历模板</a-select-option>
            <a-select-option value="PRESCRIPTION">处方套餐</a-select-option>
          </a-select>
        </a-form-item>
        
        <template v-if="form.type === 'EMR'">
          <a-form-item label="主诉">
            <a-textarea v-model:value="emrContent.symptom" :rows="2" placeholder="主诉" />
          </a-form-item>
          <a-form-item label="诊断">
            <a-input v-model:value="emrContent.diagnosis" placeholder="诊断" />
          </a-form-item>
          <a-form-item label="病历详情">
            <a-textarea v-model:value="emrContent.content" :rows="6" placeholder="详细病历内容" />
          </a-form-item>
        </template>
        
        <template v-else>
          <a-form-item label="模板内容">
            <a-textarea v-model:value="form.content" :rows="8" placeholder="请输入模板内容" />
          </a-form-item>
        </template>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { emrApi } from '@/utils/api'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const templates = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const saving = ref(false)

const form = reactive({
  tplId: null,
  name: '',
  type: 'EMR',
  content: '',
  deptId: userStore.userInfo.deptId,
  creatorId: userStore.userId
})

const emrContent = reactive({
  symptom: '',
  diagnosis: '',
  content: ''
})

const columns = [
  { title: '模板名称', dataIndex: 'name', key: 'name' },
  { title: '类型', dataIndex: 'type', key: 'type' },
  { title: '操作', key: 'action', width: 150 }
]

onMounted(() => {
  loadTemplates()
})

const loadTemplates = async () => {
  try {
    templates.value = await emrApi.getTemplates(userStore.userInfo.deptId)
  } catch (e) {
    console.error('Failed to load templates', e)
  }
}

const showAddDialog = () => {
  isEdit.value = false
  form.tplId = null
  form.name = ''
  form.type = 'EMR'
  form.content = ''
  emrContent.symptom = ''
  emrContent.diagnosis = ''
  emrContent.content = ''
  dialogVisible.value = true
}

const editTemplate = (row) => {
  isEdit.value = true
  form.tplId = row.tplId
  form.name = row.name
  form.type = row.type
  form.content = row.content
  
  if (row.type === 'EMR') {
    try {
      const parsed = JSON.parse(row.content)
      emrContent.symptom = parsed.symptom || ''
      emrContent.diagnosis = parsed.diagnosis || ''
      emrContent.content = parsed.content || ''
    } catch (e) {
      emrContent.content = row.content
    }
  }
  
  dialogVisible.value = true
}

const deleteTemplate = (row) => {
  Modal.confirm({
    title: '确认删除',
    content: '确定要删除这个模板吗？',
    onOk: async () => {
      try {
        await emrApi.deleteTemplate(row.tplId)
        message.success('删除成功')
        loadTemplates()
      } catch (e) {
        console.error(e)
      }
    }
  })
}

const saveTemplate = async () => {
  saving.value = true
  try {
    if (form.type === 'EMR') {
      form.content = JSON.stringify(emrContent)
    }
    
    await emrApi.saveTemplate(form)
    message.success('保存成功')
    dialogVisible.value = false
    loadTemplates()
  } catch (e) {
    console.error(e)
  } finally {
    saving.value = false
  }
}
</script>

<style scoped lang="scss">
.templates-page {
  padding: 20px;
  
  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    
    h2 {
      margin: 0;
    }
  }
}
</style>
