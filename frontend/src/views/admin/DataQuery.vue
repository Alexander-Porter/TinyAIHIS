<template>
  <div class="data-query-page">
    <div class="page-header">
      <h2>数据查询与导出</h2>
    </div>

    <!-- Query Type Selector -->
    <a-card class="query-builder">
      <a-tabs v-model:activeKey="queryType" @change="resetFilters">
        <a-tab-pane key="registration" tab="挂号记录" />
        <a-tab-pane key="prescription" tab="处方记录" />
        <a-tab-pane key="lab" tab="检查记录" />
        <a-tab-pane key="user" tab="用户数据" />
        <a-tab-pane key="department" tab="科室数据" />
        <a-tab-pane key="schedule" tab="排班数据" />
        <a-tab-pane key="drug" tab="药品数据" />
        <a-tab-pane key="checkItem" tab="检查项目" />
      </a-tabs>

      <!-- Dynamic Filters -->
      <div class="filters-section">
        <a-form layout="inline" :model="filters">
          <!-- Date Range (common) -->
          <a-form-item label="日期范围" v-if="hasDateFilter">
            <a-range-picker v-model:value="filters.dateRange" />
          </a-form-item>

          <!-- Department Filter -->
          <a-form-item label="科室" v-if="hasDeptFilter">
            <a-select v-model:value="filters.deptId" placeholder="全部科室" allowClear style="width: 150px">
              <a-select-option v-for="d in departments" :key="d.deptId" :value="d.deptId">{{ d.deptName }}</a-select-option>
            </a-select>
          </a-form-item>

          <!-- Doctor Filter -->
          <a-form-item label="医生" v-if="hasDoctorFilter">
            <a-select v-model:value="filters.doctorId" placeholder="全部医生" allowClear show-search option-filter-prop="label" style="width: 150px">
              <a-select-option v-for="d in doctors" :key="d.userId" :value="d.userId" :label="d.realName">{{ d.realName }}</a-select-option>
            </a-select>
          </a-form-item>

          <!-- Role Filter (for users) -->
          <a-form-item label="角色" v-if="queryType === 'user'">
            <a-select v-model:value="filters.role" placeholder="全部角色" allowClear style="width: 120px">
              <a-select-option value="ADMIN">管理员</a-select-option>
              <a-select-option value="CHIEF">主任医师</a-select-option>
              <a-select-option value="DOCTOR">医生</a-select-option>
              <a-select-option value="PHARMACY">药房</a-select-option>
              <a-select-option value="LAB">检验科</a-select-option>
            </a-select>
          </a-form-item>

          <!-- Exclude Chief (for prescription analysis) -->
          <a-form-item v-if="queryType === 'prescription'">
            <a-checkbox v-model:checked="filters.excludeChief">排除主任医师</a-checkbox>
          </a-form-item>

          <!-- Drug Filter -->
          <a-form-item label="药品" v-if="queryType === 'prescription'">
            <a-select v-model:value="filters.drugId" placeholder="全部药品" allowClear show-search option-filter-prop="label" style="width: 180px">
              <a-select-option v-for="d in drugs" :key="d.drugId" :value="d.drugId" :label="d.name">{{ d.name }}</a-select-option>
            </a-select>
          </a-form-item>

          <!-- Status Filter -->
          <a-form-item label="状态" v-if="hasStatusFilter">
            <a-select v-model:value="filters.status" placeholder="全部状态" allowClear style="width: 120px">
              <a-select-option v-for="s in statusOptions" :key="s.value" :value="s.value">{{ s.label }}</a-select-option>
            </a-select>
          </a-form-item>

          <!-- Keyword Search -->
          <a-form-item label="关键词" v-if="hasKeywordFilter">
            <a-input v-model:value="filters.keyword" placeholder="搜索..." allowClear style="width: 150px" />
          </a-form-item>
        </a-form>

        <div class="filter-actions">
          <a-button type="primary" @click="search" :loading="loading">
            <template #icon><SearchOutlined /></template> 查询
          </a-button>
          <a-button @click="resetFilters">重置</a-button>
          <a-button type="primary" ghost @click="exportData" :loading="exporting">
            <template #icon><DownloadOutlined /></template> 导出Excel
          </a-button>
        </div>
      </div>
    </a-card>

    <!-- Results Table -->
    <a-card class="results-section">
      <template #title>
        <span>查询结果</span>
        <a-tag color="blue" style="margin-left: 10px">共 {{ total }} 条</a-tag>
        <a-tag color="green" v-if="aggregation.totalAmount">总金额: ¥{{ aggregation.totalAmount.toFixed(2) }}</a-tag>
      </template>

      <a-table 
        :columns="columns" 
        :data-source="data" 
        :loading="loading"
        :pagination="pagination"
        @change="handleTableChange"
        :scroll="{ x: 1200 }"
        row-key="id"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'resultText'">
            <div v-if="record.resultText" v-html="record.resultText" style="max-height: 100px; overflow-y: auto;"></div>
            <span v-else>-</span>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { SearchOutlined, DownloadOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { adminApi, scheduleApi, pharmacyApi } from '@/utils/api'
import dayjs from 'dayjs'

const route = useRoute()

const queryType = ref(route.query.type || 'registration')
const loading = ref(false)
const exporting = ref(false)
const data = ref([])
const total = ref(0)
const aggregation = ref({})

const departments = ref([])
const doctors = ref([])
const drugs = ref([])

const filters = reactive({
  dateRange: null,
  deptId: null,
  doctorId: null,
  role: null,
  drugId: null,
  status: null,
  keyword: '',
  excludeChief: false
})

const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true
})

// Computed filter visibility
const hasDateFilter = computed(() => ['registration', 'prescription', 'lab', 'schedule'].includes(queryType.value))
const hasDeptFilter = computed(() => ['registration', 'prescription', 'lab', 'schedule', 'user'].includes(queryType.value))
const hasDoctorFilter = computed(() => ['registration', 'prescription', 'lab'].includes(queryType.value))
const hasStatusFilter = computed(() => ['registration', 'prescription', 'lab', 'user', 'drug'].includes(queryType.value))
const hasKeywordFilter = computed(() => ['user', 'drug', 'checkItem', 'department'].includes(queryType.value))

// Status options based on query type
const statusOptions = computed(() => {
  switch (queryType.value) {
    case 'registration':
      return [
        { value: 0, label: '待支付' },
        { value: 1, label: '已支付' },
        { value: 2, label: '候诊中' },
        { value: 3, label: '就诊中' },
        { value: 4, label: '已完成' },
        { value: 5, label: '已取消' }
      ]
    case 'prescription':
      return [
        { value: 0, label: '待支付' },
        { value: 1, label: '已支付' },
        { value: 2, label: '已发药' }
      ]
    case 'lab':
      return [
        { value: 0, label: '待支付' },
        { value: 1, label: '待检查' },
        { value: 2, label: '已完成' }
      ]
    case 'user':
    case 'drug':
      return [
        { value: 1, label: '启用' },
        { value: 0, label: '禁用' }
      ]
    default:
      return []
  }
})

// Dynamic columns based on query type
const columns = computed(() => {
  switch (queryType.value) {
    case 'registration':
      return [
        { title: 'ID', dataIndex: 'regId', width: 80 },
        { title: '患者', dataIndex: 'patientName', width: 100 },
        { title: '医生', dataIndex: 'doctorName', width: 100 },
        { title: '科室', dataIndex: 'deptName', width: 100 },
        { title: '日期', dataIndex: 'scheduleDate', width: 120 },
        { title: '时段', dataIndex: 'shiftType', width: 80 },
        { title: '队号', dataIndex: 'queueNumber', width: 80 },
        { title: '费用', dataIndex: 'fee', width: 100 },
        { title: '状态', dataIndex: 'statusText', width: 100 },
        { title: '创建时间', dataIndex: 'createTime', width: 160 }
      ]
    case 'prescription':
      return [
        { title: 'ID', dataIndex: 'presId', width: 80 },
        { title: '患者', dataIndex: 'patientName', width: 100 },
        { title: '医生', dataIndex: 'doctorName', width: 100 },
        { title: '科室', dataIndex: 'deptName', width: 100 },
        { title: '药品', dataIndex: 'drugName', width: 150 },
        { title: '规格', dataIndex: 'spec', width: 100 },
        { title: '数量', dataIndex: 'quantity', width: 80 },
        { title: '单价', dataIndex: 'price', width: 100 },
        { title: '金额', dataIndex: 'amount', width: 100 },
        { title: '状态', dataIndex: 'statusText', width: 100 },
        { title: '开具时间', dataIndex: 'createTime', width: 160 }
      ]
    case 'lab':
      return [
        { title: 'ID', dataIndex: 'orderId', width: 80 },
        { title: '患者', dataIndex: 'patientName', width: 100 },
        { title: '医生', dataIndex: 'doctorName', width: 100 },
        { title: '检查项目', dataIndex: 'itemName', width: 150 },
        { title: '费用', dataIndex: 'price', width: 100 },
        { title: '状态', dataIndex: 'statusText', width: 100 },
        { title: '结果', key: 'resultText', dataIndex: 'resultText', ellipsis: true, width: 300 },
        { title: '创建时间', dataIndex: 'createTime', width: 160 }
      ]
    case 'user':
      return [
        { title: 'ID', dataIndex: 'userId', width: 80 },
        { title: '用户名', dataIndex: 'username', width: 120 },
        { title: '姓名', dataIndex: 'realName', width: 100 },
        { title: '角色', dataIndex: 'roleText', width: 100 },
        { title: '科室', dataIndex: 'deptName', width: 100 },
        { title: '电话', dataIndex: 'phone', width: 120 },
        { title: '状态', dataIndex: 'statusText', width: 80 },
        { title: '创建时间', dataIndex: 'createTime', width: 160 }
      ]
    case 'department':
      return [
        { title: 'ID', dataIndex: 'deptId', width: 80 },
        { title: '科室名称', dataIndex: 'deptName', width: 150 },
        { title: '位置', dataIndex: 'location', width: 150 },
        { title: '描述', dataIndex: 'description', ellipsis: true },
        { title: '状态', dataIndex: 'statusText', width: 80 }
      ]
    case 'schedule':
      return [
        { title: 'ID', dataIndex: 'scheduleId', width: 80 },
        { title: '医生', dataIndex: 'doctorName', width: 100 },
        { title: '科室', dataIndex: 'deptName', width: 100 },
        { title: '日期', dataIndex: 'scheduleDate', width: 120 },
        { title: '时段', dataIndex: 'shiftType', width: 80 },
        { title: '已预约', dataIndex: 'currentCount', width: 80 },
        { title: '最大号源', dataIndex: 'maxQuota', width: 100 },
        { title: '状态', dataIndex: 'statusText', width: 80 }
      ]
    case 'drug':
      return [
        { title: 'ID', dataIndex: 'drugId', width: 80 },
        { title: '药品名称', dataIndex: 'name', width: 180 },
        { title: '规格', dataIndex: 'spec', width: 120 },
        { title: '单价', dataIndex: 'price', width: 100 },
        { title: '库存', dataIndex: 'stockQuantity', width: 100 },
        { title: '单位', dataIndex: 'unit', width: 80 },
        { title: '厂家', dataIndex: 'manufacturer', ellipsis: true },
        { title: '状态', dataIndex: 'statusText', width: 80 }
      ]
    case 'checkItem':
      return [
        { title: 'ID', dataIndex: 'itemId', width: 80 },
        { title: '项目名称', dataIndex: 'itemName', width: 180 },
        { title: '编码', dataIndex: 'itemCode', width: 100 },
        { title: '价格', dataIndex: 'price', width: 100 },
        { title: '类别', dataIndex: 'category', width: 100 },
        { title: '描述', dataIndex: 'description', ellipsis: true },
        { title: '状态', dataIndex: 'statusText', width: 80 }
      ]
    default:
      return []
  }
})

onMounted(async () => {
  await loadBaseData()
  search()
})

const loadBaseData = async () => {
  try {
    const [depts, drugList] = await Promise.all([
      scheduleApi.getDepartments(),
      pharmacyApi.getDrugs()
    ])
    departments.value = depts
    drugs.value = drugList
  } catch (e) {
    console.error(e)
  }
}

watch(() => filters.deptId, async (deptId) => {
  if (deptId) {
    try {
      doctors.value = await scheduleApi.getDoctors(deptId)
    } catch (e) {
      doctors.value = []
    }
  } else {
    doctors.value = []
  }
  filters.doctorId = null
})

const resetFilters = () => {
  Object.assign(filters, {
    dateRange: null,
    deptId: null,
    doctorId: null,
    role: null,
    drugId: null,
    status: null,
    keyword: '',
    excludeChief: false
  })
  pagination.current = 1
}

const search = async () => {
  loading.value = true
  try {
    const params = {
      type: queryType.value,
      page: pagination.current,
      size: pagination.pageSize,
      ...buildQueryParams()
    }
    const res = await adminApi.queryData(params)
    data.value = res.list || []
    total.value = res.total || 0
    pagination.total = res.total || 0
    aggregation.value = res.aggregation || {}
  } catch (e) {
    message.error('查询失败')
    console.error(e)
  } finally {
    loading.value = false
  }
}

const buildQueryParams = () => {
  const params = {}
  if (filters.dateRange && filters.dateRange.length === 2) {
    params.startDate = dayjs(filters.dateRange[0]).format('YYYY-MM-DD')
    params.endDate = dayjs(filters.dateRange[1]).format('YYYY-MM-DD')
  }
  if (filters.deptId) params.deptId = filters.deptId
  if (filters.doctorId) params.doctorId = filters.doctorId
  if (filters.role) params.role = filters.role
  if (filters.drugId) params.drugId = filters.drugId
  if (filters.status !== null && filters.status !== undefined) params.status = filters.status
  if (filters.keyword) params.keyword = filters.keyword
  if (filters.excludeChief) params.excludeChief = true
  return params
}

const handleTableChange = (pag) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  search()
}

const exportData = async () => {
  exporting.value = true
  try {
    const params = {
      type: queryType.value,
      ...buildQueryParams()
    }
    await adminApi.exportData(params)
    message.success('导出成功')
  } catch (e) {
    message.error('导出失败')
  } finally {
    exporting.value = false
  }
}
</script>

<style scoped lang="scss">
.data-query-page {
  padding: 20px;
  
  .page-header {
    margin-bottom: 20px;
    h2 { margin: 0; }
  }
  
  .query-builder {
    margin-bottom: 20px;
    
    .filters-section {
      margin-top: 16px;
      padding-top: 16px;
      border-top: 1px solid #f0f0f0;
      
      :deep(.ant-form-item) {
        margin-bottom: 12px;
      }
    }
    
    .filter-actions {
      margin-top: 16px;
      display: flex;
      gap: 10px;
    }
  }
  
  .results-section {
    :deep(.ant-card-head-title) {
      display: flex;
      align-items: center;
    }
  }
}
</style>
