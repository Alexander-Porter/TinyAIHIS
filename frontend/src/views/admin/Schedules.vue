<template>
  <div class="schedules-page">
    <div class="page-header">
      <h2>排班管理</h2>
      <a-button type="primary" @click="showAddDialog">新增排班</a-button>
    </div>
    
    <div class="filters">
      <a-select v-model:value="filterDept" placeholder="选择科室" @change="loadSchedules" style="width: 150px">
        <a-select-option v-for="d in departments" :key="d.deptId" :value="d.deptId">{{ d.deptName }}</a-select-option>
      </a-select>
      <a-range-picker v-model:value="dateRange" @change="loadSchedules" />
    </div>
    
    <a-table :dataSource="schedules" :columns="columns" rowKey="scheduleId">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'shift'">
          {{ record.shift === 'AM' ? '上午' : '下午' }}
        </template>
        <template v-if="column.key === 'quota'">
          {{ record.quotaLeft }} / {{ record.quotaLeft + 10 }}
        </template>
      </template>
    </a-table>
    
    <!-- Add Schedule Dialog -->
    <a-modal v-model:open="dialogVisible" title="新增排班" @ok="saveSchedule">
      <a-form :model="form" :label-col="{ span: 4 }" :wrapper-col="{ span: 20 }">
        <a-form-item label="科室">
          <a-select v-model:value="form.deptId" @change="loadDoctors" style="width: 100%">
            <a-select-option v-for="d in departments" :key="d.deptId" :value="d.deptId">{{ d.deptName }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="医生">
          <a-select v-model:value="form.doctorId" style="width: 100%">
            <a-select-option v-for="d in doctors" :key="d.userId" :value="d.userId">{{ d.realName }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="日期">
          <a-date-picker v-model:value="form.scheduleDate" style="width: 100%" />
        </a-form-item>
        <a-form-item label="班次">
          <a-radio-group v-model:value="form.shiftType">
            <a-radio value="AM">上午</a-radio>
            <a-radio value="PM">下午</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="最大号源">
          <a-input-number v-model:value="form.maxQuota" :min="1" :max="100" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { scheduleApi } from '@/utils/api'
import dayjs from 'dayjs'

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

const columns = [
  { title: 'ID', dataIndex: 'scheduleId', key: 'scheduleId', width: 60 },
  { title: '医生', dataIndex: 'doctorName', key: 'doctorName' },
  { title: '日期', dataIndex: 'date', key: 'date' },
  { title: '班次', dataIndex: 'shift', key: 'shift' },
  { title: '号源', key: 'quota' }
]

onMounted(async () => {
  try { departments.value = await scheduleApi.getDepartments() } catch (e) { console.error(e) }
})

const formatDateLocal = (date) => {
  if (!date) return null
  return dayjs(date).format('YYYY-MM-DD')
}

const loadSchedules = async () => {
  if (!filterDept.value) return
  const start = dateRange.value ? formatDateLocal(dateRange.value[0]) : formatDateLocal(new Date())
  const end = dateRange.value ? formatDateLocal(dateRange.value[1]) : formatDateLocal(new Date(Date.now() + 7 * 86400000))
  try { schedules.value = await scheduleApi.getScheduleList(filterDept.value, start, end) } catch (e) { console.error(e) }
}

const loadDoctors = async () => {
  if (!form.deptId) return
  try { doctors.value = await scheduleApi.getDoctors(form.deptId) } catch (e) { console.error(e) }
}

const showAddDialog = () => {
  form.deptId = null
  form.doctorId = null
  form.scheduleDate = null
  form.shiftType = 'AM'
  form.maxQuota = 30
  dialogVisible.value = true
}

const saveSchedule = async () => {
  try {
    await scheduleApi.saveSchedule({
      ...form,
      scheduleDate: formatDateLocal(form.scheduleDate)
    })
    message.success('排班已保存')
    dialogVisible.value = false
    loadSchedules()
  } catch (e) { console.error(e) }
}
</script>

<style scoped>
.schedules-page {
  padding: 20px;
}
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.filters {
  margin-bottom: 20px;
  display: flex;
  gap: 10px;
}
</style>
