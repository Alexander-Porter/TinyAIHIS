<template>
  <div class="templates-page">
    <div class="page-header">
      <h2>模板管理</h2>
      <el-button type="primary" @click="showAddDialog">新增模板</el-button>
    </div>
    
    <el-table :data="templates" stripe>
      <el-table-column prop="name" label="模板名称" />
      <el-table-column prop="type" label="类型">
        <template #default="{ row }">
          {{ row.type === 'EMR' ? '病历模板' : '处方套餐' }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150">
        <template #default="{ row }">
          <el-button type="primary" text @click="editTemplate(row)">编辑</el-button>
          <el-button type="danger" text @click="deleteTemplate(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    
    <!-- Add/Edit Dialog -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑模板' : '新增模板'" width="600px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="模板名称">
          <el-input v-model="form.name" placeholder="请输入模板名称" />
        </el-form-item>
        <el-form-item label="模板类型">
          <el-select v-model="form.type" style="width: 100%">
            <el-option label="病历模板" value="EMR" />
            <el-option label="处方套餐" value="PRESCRIPTION" />
          </el-select>
        </el-form-item>
        <el-form-item label="模板内容">
          <el-input v-model="form.content" type="textarea" rows="8" placeholder="请输入模板内容" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveTemplate" :loading="saving">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
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
  dialogVisible.value = true
}

const editTemplate = (tpl) => {
  isEdit.value = true
  form.tplId = tpl.tplId
  form.name = tpl.name
  form.type = tpl.type
  form.content = tpl.content
  dialogVisible.value = true
}

const saveTemplate = async () => {
  saving.value = true
  try {
    await emrApi.saveTemplate({
      ...form
    })
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadTemplates()
  } catch (e) {
    console.error('Save template failed', e)
  } finally {
    saving.value = false
  }
}

const deleteTemplate = async (tpl) => {
  await ElMessageBox.confirm('确定删除该模板?', '提示', { type: 'warning' })
  
  try {
    await emrApi.deleteTemplate(tpl.tplId)
    ElMessage.success('删除成功')
    loadTemplates()
  } catch (e) {
    console.error('Delete template failed', e)
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
