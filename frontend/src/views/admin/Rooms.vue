<template>
  <div class="rooms-page">
    <div class="page-header">
      <h2>诊室管理</h2>
      <a-button type="primary" @click="showAddDialog">
        <template #icon><PlusOutlined /></template> 新增诊室
      </a-button>
    </div>

    <a-table :dataSource="rooms" :columns="columns" :loading="loading" rowKey="roomId">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'deptNames'">
          <a-tag v-for="dept in getDeptNames(record.deptIds)" :key="dept" color="blue">{{ dept }}</a-tag>
        </template>
        <template v-if="column.key === 'status'">
          <a-tag :color="record.status === 1 ? 'success' : 'default'">
            {{ record.status === 1 ? '启用' : '停用' }}
          </a-tag>
        </template>
        <template v-if="column.key === 'action'">
          <a-space>
            <a-button type="link" size="small" @click="editRoom(record)">编辑</a-button>
            <a-popconfirm 
              :title="record.status === 1 ? '确定停用该诊室?' : '确定启用该诊室?'"
              @confirm="toggleStatus(record)"
            >
              <a-button type="link" size="small" :danger="record.status === 1">
                {{ record.status === 1 ? '停用' : '启用' }}
              </a-button>
            </a-popconfirm>
            <a-popconfirm title="确定删除该诊室?" @confirm="deleteRoom(record.roomId)">
              <a-button type="link" size="small" danger>删除</a-button>
            </a-popconfirm>
          </a-space>
        </template>
      </template>
    </a-table>

    <!-- Add/Edit Room Dialog -->
    <a-modal v-model:open="dialogVisible" :title="editingRoom ? '编辑诊室' : '新增诊室'" @ok="saveRoom" :confirmLoading="saving">
      <a-form :model="form" :label-col="{ span: 5 }" :wrapper-col="{ span: 18 }">
        <a-form-item label="诊室名称" required>
          <a-input v-model:value="form.roomName" placeholder="如：1号诊室" />
        </a-form-item>
        <a-form-item label="位置" required>
          <a-input v-model:value="form.location" placeholder="如：门诊楼2层东侧" />
        </a-form-item>
        <a-form-item label="所属科室">
          <a-select 
            v-model:value="form.deptIdList" 
            mode="multiple" 
            placeholder="选择可使用该诊室的科室（不选则所有科室可用）"
            style="width: 100%"
          >
            <a-select-option v-for="d in departments" :key="d.deptId" :value="d.deptId">{{ d.deptName }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="描述">
          <a-textarea v-model:value="form.description" :rows="2" placeholder="诊室描述" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { adminApi, scheduleApi } from '@/utils/api'

const rooms = ref([])
const departments = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const editingRoom = ref(null)
const saving = ref(false)

const form = reactive({
  roomName: '',
  location: '',
  deptIdList: [],
  description: ''
})

const columns = [
  { title: '诊室ID', dataIndex: 'roomId', key: 'roomId', width: 80 },
  { title: '诊室名称', dataIndex: 'roomName', key: 'roomName' },
  { title: '位置', dataIndex: 'location', key: 'location' },
  { title: '所属科室', key: 'deptNames' },
  { title: '状态', key: 'status', width: 80 },
  { title: '操作', key: 'action', width: 200 }
]

// 根据deptIds获取科室名称
const getDeptNames = (deptIds) => {
  if (!deptIds) return ['所有科室']
  try {
    const ids = typeof deptIds === 'string' ? JSON.parse(deptIds) : deptIds
    if (!ids || ids.length === 0) return ['所有科室']
    return ids.map(id => {
      const dept = departments.value.find(d => d.deptId === id)
      return dept ? dept.deptName : `科室#${id}`
    })
  } catch {
    return ['所有科室']
  }
}

onMounted(async () => {
  await Promise.all([loadRooms(), loadDepartments()])
})

const loadRooms = async () => {
  loading.value = true
  try {
    rooms.value = await adminApi.getRooms()
  } catch (e) {
    console.error(e)
    rooms.value = []
  } finally {
    loading.value = false
  }
}

const loadDepartments = async () => {
  try {
    departments.value = await scheduleApi.getDepartments()
  } catch (e) {
    console.error(e)
  }
}

const showAddDialog = () => {
  editingRoom.value = null
  Object.assign(form, { roomName: '', location: '', deptIdList: [], description: '' })
  dialogVisible.value = true
}

const editRoom = (room) => {
  editingRoom.value = room
  let deptIdList = []
  if (room.deptIds) {
    try {
      deptIdList = typeof room.deptIds === 'string' ? JSON.parse(room.deptIds) : room.deptIds
    } catch {
      deptIdList = []
    }
  }
  Object.assign(form, {
    roomName: room.roomName,
    location: room.location,
    deptIdList: deptIdList || [],
    description: room.description || ''
  })
  dialogVisible.value = true
}

const saveRoom = async () => {
  if (!form.roomName || !form.location) {
    message.error('请填写必填项')
    return
  }
  
  saving.value = true
  try {
    const data = {
      roomId: editingRoom.value?.roomId,
      roomName: form.roomName,
      location: form.location,
      deptIds: form.deptIdList.length > 0 ? JSON.stringify(form.deptIdList) : null,
      description: form.description,
      status: editingRoom.value?.status ?? 1
    }
    await adminApi.saveRoom(data)
    message.success('保存成功')
    dialogVisible.value = false
    loadRooms()
  } catch (e) {
    console.error(e)
  } finally {
    saving.value = false
  }
}

const toggleStatus = async (room) => {
  try {
    const data = { ...room, status: room.status === 1 ? 0 : 1 }
    await adminApi.saveRoom(data)
    message.success('更新成功')
    loadRooms()
  } catch (e) {
    console.error(e)
  }
}

const deleteRoom = async (roomId) => {
  try {
    await adminApi.deleteRoom(roomId)
    message.success('删除成功')
    loadRooms()
  } catch (e) {
    console.error(e)
  }
}
</script>

<style scoped lang="scss">
.rooms-page {
  padding: 20px;
  
  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 24px;
    
    h2 { margin: 0; }
  }
}
</style>
