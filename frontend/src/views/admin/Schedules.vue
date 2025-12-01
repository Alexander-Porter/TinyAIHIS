<template>
  <div class="schedules-page">
    <div class="page-header">
      <h2>排班管理</h2>
      <el-button type="primary" @click="showAddDialog">新增排班</el-button>
    </div>
    
    <div class="filters">
      <el-select v-model="filterDept" placeholder="选择科室" @change="loadSchedules" style="width: 150px">
        <el-option v-for="d in departments" :key="d.deptId" :label="d.deptName" :value="d.deptId" />
      </el-select>
      <el-date-picker v-model="dateRange" type="daterange" start-placeholder="开始" end-placeholder="结束" @change="loadSchedules" />
    </div>
    
    <el-table :data="schedules" stripe>
      <el-table-column prop="scheduleId" label="ID" width="60" />
      <el-table-column prop="doctorName" label="医生" />
      <el-table-column prop="date" label="日期" />
      <el-table-column prop="shift" label="班次">
        <template #default="{ row }">{{ row.shift === 'AM' ? '上午' : '下午' }}</template>
      </el-table-column>
      <el-table-column label="号源">
        <template #default="{ row }">{{ row.quotaLeft }} / {{ row.quotaLeft + 10 }}</template>
      </el-table-column>
    </el-table>
    
    <!-- Add Schedule Dialog -->
    <el-dialog v-model="dialogVisible" title="新增排班" width="500px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="科室">
          <el-select v-model="form.deptId" @change="loadDoctors" style="width: 100%">
            <el-option v-for="d in departments" :key="d.deptId" :label="d.deptName" :value="d.deptId" />
          </el-select>
        </el-form-item>
        <el-form-item label="医生">
          <el-select v-model="form.doctorId" style="width: 100%">
            <el-option v-for="d in doctors" :key="d.userId" :label="d.realName" :value="d.userId" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期">
          <el-date-picker v-model="form.scheduleDate" type="date" style="width: 100%" />
        </el-form-item>
        <el-form-item label="班次">
          <el-radio-group v-model="form.shiftType">
            <el-radio label="AM">上午</el-radio>
            <el-radio label="PM">下午</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="最大号源">
          <el-input-number v-model="form.maxQuota" :min="1" :max="100" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveSchedule">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { scheduleApi } from '@/utils/api'

const departments = ref([])
const doctors = ref([])
const schedules = ref([])
const filterDept = ref(null)
const dateRange = ref(null)
const dialogVisible = ref(false)

const form = reactive({
  deptId: null,
  doctorId: null,
  scheduleDate: null,
  shiftType: 'AM',
  maxQuota: 30
})

onMounted(async () => {
  try { departments.value = await scheduleApi.getDepartments() } catch (e) { console.error(e) }
})

const loadSchedules = async () => {
  if (!filterDept.value) return
  const start = dateRange.value?.[0]?.toISOString().split('T')[0] || new Date().toISOString().split('T')[0]
  const end = dateRange.value?.[1]?.toISOString().split('T')[0] || new Date(Date.now() + 7 * 86400000).toISOString().split('T')[0]
  try { schedules.value = await scheduleApi.getScheduleList(filterDept.value, start, end) } catch (e) { console.error(e) }
}

const loadDoctors = async () => {
  if (!form.deptId) return
  try { doctors.value = await scheduleApi.getDoctors(form.deptId) } catch (e) { console.error(e) }
}

const showAddDialog = () => {
  Object.assign(form, { deptId: null, doctorId: null, scheduleDate: null, shiftType: 'AM', maxQuota: 30 })
  dialogVisible.value = true
}

const saveSchedule = async () => {
  try {
    await scheduleApi.saveSchedule({
      doctorId: form.doctorId,
      scheduleDate: form.scheduleDate?.toISOString().split('T')[0],
      shiftType: form.shiftType,
      maxQuota: form.maxQuota,
      currentCount: 0,
      status: 1
    })
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadSchedules()
  } catch (e) { console.error(e) }
}
</script>

<style scoped lang="scss">
.schedules-page { padding: 20px; .page-header { display: flex; justify-content: space-between; margin-bottom: 20px; h2 { margin: 0; } } .filters { display: flex; gap: 10px; margin-bottom: 20px; } }
</style>
