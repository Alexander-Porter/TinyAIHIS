<template>
  <div class="users-page">
    <div class="page-header">
      <h2>用户管理</h2>
      <div class="actions">
        <el-input v-model="searchKeyword" placeholder="搜索用户" style="width: 200px" clearable />
        <el-select v-model="filterRole" placeholder="角色筛选" clearable style="width: 120px">
          <el-option label="医生" value="DOCTOR" />
          <el-option label="主任" value="CHIEF" />
          <el-option label="药房" value="PHARMACY" />
          <el-option label="检验" value="LAB" />
          <el-option label="管理员" value="ADMIN" />
        </el-select>
      </div>
    </div>
    
    <el-table :data="filteredUsers" stripe>
      <el-table-column prop="userId" label="ID" width="60" />
      <el-table-column prop="username" label="用户名" />
      <el-table-column prop="realName" label="姓名" />
      <el-table-column prop="role" label="角色">
        <template #default="{ row }">
          <el-tag>{{ getRoleName(row.role) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="deptId" label="科室ID" width="80" />
      <el-table-column prop="status" label="状态">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { authApi } from '@/utils/api'

const users = ref([
  { userId: 1, username: 'admin', realName: '系统管理员', role: 'ADMIN', deptId: null, status: 1 },
  { userId: 2, username: 'doctor1', realName: '张医生', role: 'DOCTOR', deptId: 1, status: 1 },
  { userId: 3, username: 'doctor2', realName: '李医生', role: 'DOCTOR', deptId: 3, status: 1 },
  { userId: 4, username: 'chief1', realName: '王主任', role: 'CHIEF', deptId: 1, status: 1 },
  { userId: 5, username: 'pharmacy', realName: '药房人员', role: 'PHARMACY', deptId: 10, status: 1 },
  { userId: 6, username: 'lab', realName: '检验人员', role: 'LAB', deptId: 9, status: 1 },
])

const searchKeyword = ref('')
const filterRole = ref('')

const filteredUsers = computed(() => {
  return users.value.filter(u => {
    const matchKeyword = !searchKeyword.value || 
      u.username.includes(searchKeyword.value) || 
      u.realName.includes(searchKeyword.value)
    const matchRole = !filterRole.value || u.role === filterRole.value
    return matchKeyword && matchRole
  })
})

const getRoleName = (role) => {
  const map = { ADMIN: '管理员', DOCTOR: '医生', CHIEF: '主任', PHARMACY: '药房', LAB: '检验' }
  return map[role] || role
}
</script>

<style scoped lang="scss">
.users-page {
  padding: 20px;
  .page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; h2 { margin: 0; } .actions { display: flex; gap: 10px; } }
}
</style>
