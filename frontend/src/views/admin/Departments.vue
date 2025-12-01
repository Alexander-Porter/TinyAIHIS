<template>
  <div class="departments-page">
    <div class="page-header"><h2>科室管理</h2></div>
    
    <el-table :data="departments" stripe>
      <el-table-column prop="deptId" label="ID" width="60" />
      <el-table-column prop="deptName" label="科室名称" />
      <el-table-column prop="location" label="位置" />
      <el-table-column prop="description" label="描述" />
      <el-table-column prop="status" label="状态">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">
            {{ row.status === 1 ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { scheduleApi } from '@/utils/api'

const departments = ref([])

onMounted(async () => {
  try { departments.value = await scheduleApi.getDepartments() } catch (e) { console.error(e) }
})
</script>

<style scoped lang="scss">
.departments-page { padding: 20px; .page-header { margin-bottom: 20px; h2 { margin: 0; } } }
</style>
