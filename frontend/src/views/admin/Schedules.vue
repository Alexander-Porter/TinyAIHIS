<template>
  <div class="schedules-page">
    <div class="page-header">
      <h2>排班管理</h2>
      <div class="actions">
        <a-select v-model:value="selectedDept" placeholder="选择科室" style="width: 180px" @change="loadDeptSchedules">
          <a-select-option v-for="d in departments" :key="d.deptId" :value="d.deptId">{{ d.deptName }}</a-select-option>
        </a-select>
        <a-button type="primary" @click="showAddDialog" :disabled="!selectedDept">新增排班</a-button>
      </div>
    </div>
    
    <!-- Week Schedule Grid -->
    <div class="schedule-grid" v-if="selectedDept">
      <div class="grid-header">
        <div class="time-col">时段</div>
        <div class="day-col" v-for="(day, idx) in weekDays" :key="idx">
          <div class="day-name">{{ day.name }}</div>
          <div class="day-date">{{ day.label }}</div>
        </div>
      </div>
      <div class="grid-row" v-for="shift in ['AM', 'PM', 'ER']" :key="shift">
        <div class="time-col">{{ shiftLabels[shift] }}</div>
        <div class="day-col" v-for="(day, idx) in weekDays" :key="idx">
          <div class="schedule-cell">
            <div 
              class="schedule-item" 
              v-for="s in getSchedulesForDayShift(idx, shift)" 
              :key="s.templateId"
              @click="editSchedule(s)"
            >
              <div class="doctor-name">{{ s.doctorName }}</div>
              <div class="room-name" v-if="s.roomName">{{ s.roomName }}</div>
              <div class="quota">限{{ s.maxQuota }}人</div>
            </div>
            <a-button type="dashed" size="small" @click="addScheduleFor(idx, shift)" v-if="getSchedulesForDayShift(idx, shift).length === 0">
              + 添加
            </a-button>
          </div>
        </div>
      </div>
    </div>
    
    <a-empty v-else description="请先选择科室" />
    
    <!-- Add/Edit Schedule Dialog -->
    <a-modal v-model:open="dialogVisible" :title="editingSchedule ? '编辑排班' : '新增排班'" @ok="saveSchedule" :confirmLoading="saving">
      <a-form :model="form" :label-col="{ span: 5 }" :wrapper-col="{ span: 18 }">
        <a-form-item label="医生" required>
          <a-select v-model:value="form.doctorId" placeholder="选择医生" style="width: 100%">
            <a-select-option v-for="d in doctors" :key="d.userId" :value="d.userId">{{ d.realName }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="诊室" required>
          <a-select v-model:value="form.roomId" placeholder="选择诊室" style="width: 100%">
            <a-select-option v-for="r in rooms" :key="r.roomId" :value="r.roomId">
              {{ r.roomName }} ({{ r.location }})
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="星期" required>
          <a-select v-model:value="form.dayOfWeek" style="width: 100%">
            <a-select-option v-for="(day, idx) in weekDays" :key="idx" :value="idx">{{ day.name }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="班次" required>
          <a-radio-group v-model:value="form.shiftType">
            <a-radio value="AM">上午</a-radio>
            <a-radio value="PM">下午</a-radio>
            <a-radio value="ER">急诊(全天)</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="最大号源">
          <a-input-number v-model:value="form.maxQuota" :min="1" :max="100" style="width: 100%" />
        </a-form-item>
      </a-form>
      <template #footer>
        <a-button v-if="editingSchedule" danger @click="deleteSchedule">删除</a-button>
        <a-button @click="dialogVisible = false">取消</a-button>
        <a-button type="primary" @click="saveSchedule" :loading="saving">保存</a-button>
      </template>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { message } from 'ant-design-vue'
import { scheduleApi, adminApi } from '@/utils/api'

const departments = ref([])
const doctors = ref([])
const rooms = ref([])
const scheduleTemplates = ref([]) // 周排班模板
const selectedDept = ref(null)
const dialogVisible = ref(false)
const editingSchedule = ref(null)
const saving = ref(false)

const form = reactive({
  doctorId: null,
  roomId: null,
  dayOfWeek: 0,
  shiftType: 'AM',
  maxQuota: 30
})

const shiftLabels = {
  'AM': '上午',
  'PM': '下午',
  'ER': '急诊'
}

const weekDays = [
  { name: '周一', label: '一' },
  { name: '周二', label: '二' },
  { name: '周三', label: '三' },
  { name: '周四', label: '四' },
  { name: '周五', label: '五' },
  { name: '周六', label: '六' },
  { name: '周日', label: '日' }
]

onMounted(async () => {
  try {
    const [depts, roomList] = await Promise.all([
      scheduleApi.getDepartments(),
      adminApi.getRooms()
    ])
    departments.value = depts
    rooms.value = roomList.filter(r => r.status === 1) // 只显示启用的诊室
  } catch (e) {
    console.error(e)
  }
})

const loadDeptSchedules = async () => {
  if (!selectedDept.value) return
  try {
    // 加载该科室的医生
    doctors.value = await scheduleApi.getDoctors(selectedDept.value)
    // 加载该科室的周排班模板
    scheduleTemplates.value = await adminApi.getScheduleTemplates(selectedDept.value)
  } catch (e) {
    console.error(e)
    scheduleTemplates.value = []
  }
}

const getSchedulesForDayShift = (dayOfWeek, shift) => {
  return scheduleTemplates.value.filter(s => s.dayOfWeek === dayOfWeek && s.shiftType === shift)
}

const showAddDialog = () => {
  editingSchedule.value = null
  Object.assign(form, { doctorId: null, roomId: null, dayOfWeek: 0, shiftType: 'AM', maxQuota: 30 })
  dialogVisible.value = true
}

const addScheduleFor = (dayOfWeek, shift) => {
  editingSchedule.value = null
  Object.assign(form, { doctorId: null, roomId: null, dayOfWeek, shiftType: shift, maxQuota: 30 })
  dialogVisible.value = true
}

const editSchedule = (schedule) => {
  editingSchedule.value = schedule
  Object.assign(form, {
    doctorId: schedule.doctorId,
    roomId: schedule.roomId,
    dayOfWeek: schedule.dayOfWeek,
    shiftType: schedule.shiftType,
    maxQuota: schedule.maxQuota
  })
  dialogVisible.value = true
}

const saveSchedule = async () => {
  if (!form.doctorId) {
    message.error('请选择医生')
    return
  }
  if (!form.roomId) {
    message.error('请选择诊室')
    return
  }
  
  saving.value = true
  try {
    const data = {
      ...form,
      deptId: selectedDept.value,
      templateId: editingSchedule.value?.templateId
    }
    await adminApi.saveScheduleTemplate(data)
    message.success('保存成功')
    dialogVisible.value = false
    loadDeptSchedules()
  } catch (e) {
    console.error(e)
  } finally {
    saving.value = false
  }
}

const deleteSchedule = async () => {
  if (!editingSchedule.value) return
  try {
    await adminApi.deleteScheduleTemplate(editingSchedule.value.templateId)
    message.success('删除成功')
    dialogVisible.value = false
    loadDeptSchedules()
  } catch (e) {
    console.error(e)
  }
}
</script>

<style scoped lang="scss">
.schedules-page {
  padding: 20px;
  
  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 24px;
    
    h2 { margin: 0; }
    
    .actions {
      display: flex;
      gap: 12px;
    }
  }
  
  .schedule-grid {
    background: #fff;
    border-radius: 8px;
    overflow: hidden;
    border: 1px solid #e8e8e8;
    
    .grid-header, .grid-row {
      display: flex;
    }
    
    .grid-header {
      background: #fafafa;
      font-weight: 600;
      
      .day-col {
        text-align: center;
        padding: 12px 8px;
        
        .day-name { font-size: 14px; }
        .day-date { font-size: 12px; color: #999; }
      }
    }
    
    .time-col {
      width: 80px;
      min-width: 80px;
      padding: 16px;
      background: #fafafa;
      font-weight: 500;
      display: flex;
      align-items: center;
      justify-content: center;
      border-right: 1px solid #e8e8e8;
    }
    
    .day-col {
      flex: 1;
      min-width: 120px;
      border-right: 1px solid #e8e8e8;
      
      &:last-child { border-right: none; }
    }
    
    .grid-row {
      border-top: 1px solid #e8e8e8;
      
      .day-col {
        padding: 8px;
        min-height: 100px;
      }
    }
    
    .schedule-cell {
      height: 100%;
      display: flex;
      flex-direction: column;
      gap: 8px;
    }
    
    .schedule-item {
      background: #e6f7ff;
      border: 1px solid #91d5ff;
      border-radius: 4px;
      padding: 8px;
      cursor: pointer;
      transition: all 0.2s;
      
      &:hover {
        background: #bae7ff;
      }
      
      .doctor-name {
        font-weight: 500;
        font-size: 13px;
      }
      
      .room-name {
        font-size: 11px;
        color: #1890ff;
        margin-top: 2px;
      }
      
      .quota {
        font-size: 12px;
        color: #666;
      }
    }
  }
}
</style>
