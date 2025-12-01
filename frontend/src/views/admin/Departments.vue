<template>
  <div class="departments-page">
    <div class="page-header">
      <h2>科室管理</h2>
      <a-button type="primary" @click="showAddDialog">
        <template #icon><PlusOutlined /></template>
        新增科室
      </a-button>
    </div>
    
    <a-table :dataSource="departments" :columns="columns" :loading="loading" rowKey="deptId">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'status'">
          <a-switch :checked="record.status === 1" @change="toggleStatus(record)" size="small" />
        </template>
        <template v-if="column.key === 'action'">
          <a-space>
            <a @click="editDept(record)">编辑</a>
            <a-popconfirm title="确定删除该科室？" @confirm="deleteDept(record)">
              <a style="color: #ff4d4f">删除</a>
            </a-popconfirm>
          </a-space>
        </template>
      </template>
    </a-table>
    
    <!-- Add/Edit Dialog -->
    <a-modal v-model:open="dialogVisible" :title="editingDept ? '编辑科室' : '新增科室'" @ok="saveDept" :confirmLoading="saving">
      <a-form :model="form" :label-col="{ span: 5 }" :wrapper-col="{ span: 18 }">
        <a-form-item label="科室名称" required>
          <a-input v-model:value="form.deptName" placeholder="如：内科" />
        </a-form-item>
        <a-form-item label="位置">
          <a-input v-model:value="form.location" placeholder="如：门诊楼1楼" />
        </a-form-item>
        <a-form-item label="叫号屏ID">
          <a-input v-model:value="form.screenId" placeholder="关联的叫号屏设备ID" />
        </a-form-item>
        <a-form-item label="科室描述">
          <a-textarea v-model:value="form.description" :rows="3" placeholder="科室简介" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { adminApi, scheduleApi } from '@/utils/api'

const departments = ref([])
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const editingDept = ref(null)

const form = reactive({
  deptName: '',
  location: '',
  screenId: '',
  description: ''
})

const columns = [
  { title: 'ID', dataIndex: 'deptId', key: 'deptId', width: 60 },
  { title: '科室名称', dataIndex: 'deptName', key: 'deptName', width: 150 },
  { title: '位置', dataIndex: 'location', key: 'location', width: 150 },
  { title: '叫号屏ID', dataIndex: 'screenId', key: 'screenId', width: 120 },
  { title: '描述', dataIndex: 'description', key: 'description' },
  { title: '状态', dataIndex: 'status', key: 'status', width: 80 },
  { title: '操作', key: 'action', width: 120 }
]

onMounted(() => {
  loadDepartments()
})

const loadDepartments = async () => {
  loading.value = true
  try {
    departments.value = await scheduleApi.getDepartments()
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const showAddDialog = () => {
  editingDept.value = null
  Object.assign(form, { deptName: '', location: '', screenId: '', description: '' })
  dialogVisible.value = true
}

const editDept = (dept) => {
  editingDept.value = dept
  Object.assign(form, {
    deptName: dept.deptName,
    location: dept.location || '',
    screenId: dept.screenId || '',
    description: dept.description || ''
  })
  dialogVisible.value = true
}

const saveDept = async () => {
  if (!form.deptName) {
    message.error('请填写科室名称')
    return
  }
  
  saving.value = true
  try {
    const data = { ...form }
    if (editingDept.value) {
      data.deptId = editingDept.value.deptId
    }
    await adminApi.saveDepartment(data)
    message.success(editingDept.value ? '修改成功' : '创建成功')
    dialogVisible.value = false
    loadDepartments()
  } catch (e) {
    console.error(e)
  } finally {
    saving.value = false
  }
}

const toggleStatus = async (dept) => {
  try {
    await adminApi.updateDepartmentStatus(dept.deptId, dept.status === 1 ? 0 : 1)
    dept.status = dept.status === 1 ? 0 : 1
    message.success('状态已更新')
  } catch (e) {
    console.error(e)
  }
}

const deleteDept = async (dept) => {
  try {
    await adminApi.deleteDepartment(dept.deptId)
    message.success('删除成功')
    loadDepartments()
  } catch (e) {
    console.error(e)
  }
}
</script>

<style scoped lang="scss">
.departments-page { 
  padding: 20px; 
  
  .page-header { 
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px; 
    
    h2 { margin: 0; } 
  } 
}
</style>
