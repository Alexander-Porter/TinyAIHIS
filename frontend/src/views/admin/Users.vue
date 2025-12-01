<template>
  <div class="users-page">
    <div class="page-header">
      <h2>用户管理</h2>
      <div class="actions">
        <a-input-search v-model:value="searchKeyword" placeholder="搜索用户名/姓名" style="width: 200px" @search="loadUsers" />
        <a-select v-model:value="filterRole" placeholder="角色筛选" allowClear style="width: 120px" @change="loadUsers">
          <a-select-option value="DOCTOR">医生</a-select-option>
          <a-select-option value="CHIEF">主任</a-select-option>
          <a-select-option value="PHARMACY">药房</a-select-option>
          <a-select-option value="LAB">检验</a-select-option>
          <a-select-option value="ADMIN">管理员</a-select-option>
        </a-select>
        <a-button type="primary" @click="showAddDialog">
          <template #icon><PlusOutlined /></template>
          新增用户
        </a-button>
      </div>
    </div>
    
    <a-table :dataSource="users" :columns="columns" :loading="loading" rowKey="userId" :pagination="pagination" @change="handleTableChange">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'role'">
          <a-tag :color="getRoleColor(record.role)">{{ getRoleName(record.role) }}</a-tag>
        </template>
        <template v-if="column.key === 'deptName'">
          {{ record.deptName || '-' }}
        </template>
        <template v-if="column.key === 'status'">
          <a-switch :checked="record.status === 1" @change="toggleStatus(record)" size="small" />
        </template>
        <template v-if="column.key === 'action'">
          <a-space>
            <a @click="editUser(record)">编辑</a>
            <a-popconfirm title="确定删除该用户？" @confirm="deleteUser(record)">
              <a style="color: #ff4d4f">删除</a>
            </a-popconfirm>
          </a-space>
        </template>
      </template>
    </a-table>
    
    <!-- Add/Edit Dialog -->
    <a-modal v-model:open="dialogVisible" :title="editingUser ? '编辑用户' : '新增用户'" @ok="saveUser" :confirmLoading="saving">
      <a-form :model="form" :label-col="{ span: 5 }" :wrapper-col="{ span: 18 }">
        <a-form-item label="用户名" required>
          <a-input v-model:value="form.username" :disabled="!!editingUser" placeholder="登录用户名" />
        </a-form-item>
        <a-form-item label="密码" :required="!editingUser">
          <a-input-password v-model:value="form.password" :placeholder="editingUser ? '留空不修改' : '请输入密码'" />
        </a-form-item>
        <a-form-item label="姓名" required>
          <a-input v-model:value="form.realName" placeholder="真实姓名" />
        </a-form-item>
        <a-form-item label="角色" required>
          <a-select v-model:value="form.role" placeholder="选择角色">
            <a-select-option value="DOCTOR">医生</a-select-option>
            <a-select-option value="CHIEF">主任</a-select-option>
            <a-select-option value="PHARMACY">药房</a-select-option>
            <a-select-option value="LAB">检验</a-select-option>
            <a-select-option value="ADMIN">管理员</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="所属科室" v-if="form.role === 'DOCTOR' || form.role === 'CHIEF'">
          <a-select v-model:value="form.deptId" placeholder="选择科室">
            <a-select-option v-for="dept in departments" :key="dept.deptId" :value="dept.deptId">
              {{ dept.deptName }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="联系电话">
          <a-input v-model:value="form.phone" placeholder="手机号码" />
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

const users = ref([])
const departments = ref([])
const loading = ref(false)
const saving = ref(false)
const searchKeyword = ref('')
const filterRole = ref(null)
const dialogVisible = ref(false)
const editingUser = ref(null)
const pagination = reactive({ current: 1, pageSize: 10, total: 0 })

const form = reactive({
  username: '',
  password: '',
  realName: '',
  role: null,
  deptId: null,
  phone: ''
})

const columns = [
  { title: 'ID', dataIndex: 'userId', key: 'userId', width: 60 },
  { title: '用户名', dataIndex: 'username', key: 'username', width: 120 },
  { title: '姓名', dataIndex: 'realName', key: 'realName', width: 100 },
  { title: '角色', dataIndex: 'role', key: 'role', width: 100 },
  { title: '科室', dataIndex: 'deptName', key: 'deptName', width: 120 },
  { title: '联系电话', dataIndex: 'phone', key: 'phone', width: 130 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 80 },
  { title: '操作', key: 'action', width: 120 }
]

onMounted(async () => {
  await loadDepartments()
  await loadUsers()
})

const loadDepartments = async () => {
  try {
    departments.value = await scheduleApi.getDepartments()
  } catch (e) {
    console.error(e)
  }
}

const loadUsers = async () => {
  loading.value = true
  try {
    const params = {
      page: pagination.current,
      size: pagination.pageSize,
      keyword: searchKeyword.value || undefined,
      role: filterRole.value || undefined
    }
    const res = await adminApi.getUsers(params)
    users.value = res.list || res
    pagination.total = res.total || users.value.length
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const handleTableChange = (pag) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  loadUsers()
}

const getRoleName = (role) => {
  const map = { ADMIN: '管理员', DOCTOR: '医生', CHIEF: '主任', PHARMACY: '药房', LAB: '检验' }
  return map[role] || role
}

const getRoleColor = (role) => {
  const map = { ADMIN: 'red', DOCTOR: 'blue', CHIEF: 'purple', PHARMACY: 'green', LAB: 'orange' }
  return map[role] || 'default'
}

const showAddDialog = () => {
  editingUser.value = null
  Object.assign(form, { username: '', password: '', realName: '', role: null, deptId: null, phone: '' })
  dialogVisible.value = true
}

const editUser = (user) => {
  editingUser.value = user
  Object.assign(form, {
    username: user.username,
    password: '',
    realName: user.realName,
    role: user.role,
    deptId: user.deptId,
    phone: user.phone
  })
  dialogVisible.value = true
}

const saveUser = async () => {
  if (!form.username || !form.realName || !form.role) {
    message.error('请填写必填项')
    return
  }
  if (!editingUser.value && !form.password) {
    message.error('请设置密码')
    return
  }
  
  saving.value = true
  try {
    const data = { ...form }
    if (editingUser.value) {
      data.userId = editingUser.value.userId
      if (!data.password) delete data.password
    }
    await adminApi.saveUser(data)
    message.success(editingUser.value ? '修改成功' : '创建成功')
    dialogVisible.value = false
    loadUsers()
  } catch (e) {
    console.error(e)
  } finally {
    saving.value = false
  }
}

const toggleStatus = async (user) => {
  try {
    await adminApi.updateUserStatus(user.userId, user.status === 1 ? 0 : 1)
    user.status = user.status === 1 ? 0 : 1
    message.success('状态已更新')
  } catch (e) {
    console.error(e)
  }
}

const deleteUser = async (user) => {
  try {
    await adminApi.deleteUser(user.userId)
    message.success('删除成功')
    loadUsers()
  } catch (e) {
    console.error(e)
  }
}
</script>

<style scoped lang="scss">
.users-page {
  padding: 20px;
  
  .page-header { 
    display: flex; 
    justify-content: space-between; 
    align-items: center; 
    margin-bottom: 20px; 
    
    h2 { margin: 0; } 
    
    .actions { 
      display: flex; 
      gap: 12px; 
    } 
  }
}
</style>
