<template>
  <div class="workstation">
    <div class="workstation-header">
      <h2>接诊工作台</h2>
      <div class="actions">
        <a-button type="primary" @click="callNext" :loading="calling">
          <template #icon><BellOutlined /></template> 叫号
        </a-button>
      </div>
    </div>
    
    <div class="workstation-content">
      <!-- Left: Queue & Patient Info (20%) -->
      <div class="left-panel">
        <div class="panel-card queue-panel">
          <div class="panel-header">
            <span>候诊队列</span>
            <a-tag>{{ queue.length }} 人</a-tag>
          </div>
          <div class="queue-list">
            <div class="queue-empty" v-if="queue.length === 0">
              暂无候诊患者
            </div>
            <div 
              class="queue-item" 
              v-for="item in queue" 
              :key="item.regId"
              :class="{ current: currentPatient?.regId === item.regId }"
              @click="selectPatient(item)">
              <div class="number">{{ item.queueNumber }}</div>
              <div class="info">
                <div class="name">{{ item.patientName || '患者 #' + item.patientId }}</div>
                <div class="status">{{ getStatusText(item.status) }}</div>
              </div>
            </div>
          </div>
        </div>
        
        <div class="panel-card patient-info-panel" v-if="currentPatient">
          <div class="panel-header">当前患者</div>
          <div class="info-content">
            <p><strong>姓名:</strong> {{ currentPatient.patientName || '未知' }}</p>
            <p><strong>性别:</strong> {{ currentPatient.gender === 1 ? '男' : '女' }}</p>
            <p><strong>年龄:</strong> {{ currentPatient.age || '-' }}岁</p>
          </div>
        </div>
      </div>
      
      <!-- Middle: EMR Editor (50%) -->
      <div class="middle-panel">
        <div class="panel-card emr-editor" v-if="currentPatient">
          <div class="panel-header">
            <span>病历录入</span>
            <div class="tools">
               <a-button size="small" type="link" @click="activeTab = 'templates'">引用模板</a-button>
            </div>
          </div>
          <div class="emr-form-container">
            <a-form :model="emrForm" layout="vertical">
              <a-form-item label="主诉">
                <a-textarea v-model:value="emrForm.symptom" :rows="2" placeholder="患者主诉" />
              </a-form-item>
              <a-form-item label="诊断">
                <a-input v-model:value="emrForm.diagnosis" placeholder="诊断结果" />
              </a-form-item>
              <a-form-item label="现病史/查体">
                <a-textarea v-model:value="emrForm.content" :rows="12" placeholder="详细病历内容..." />
              </a-form-item>
            </a-form>
            <div class="emr-actions">
              <a-button type="primary" @click="saveEmr" :loading="saving">保存病历</a-button>
              <a-button type="primary" ghost @click="completeVisit" style="margin-left: 10px">完成就诊</a-button>
            </div>
          </div>
        </div>
        <div class="no-patient" v-else>
          <a-empty description="请点击叫号或选择患者" />
        </div>
      </div>

      <!-- Right: Tools & History (30%) -->
      <div class="right-panel" v-if="currentPatient">
        <a-tabs v-model:activeKey="activeTab" type="card" class="tools-tabs">
          <a-tab-pane key="prescription" tab="处方">
            <div class="tool-content">
              <div class="drug-list">
                <div class="drug-item" v-for="(drug, idx) in prescriptions" :key="idx">
                  <div class="drug-row">
                    <a-select v-model:value="drug.drugId" placeholder="选择药品" show-search option-filter-prop="label" size="small" style="width: 100%">
                      <a-select-option v-for="d in drugs" :key="d.drugId" :value="d.drugId" :label="d.name">{{ d.name }}</a-select-option>
                    </a-select>
                    <a-button type="text" danger size="small" @click="prescriptions.splice(idx, 1)"><template #icon><DeleteOutlined /></template></a-button>
                  </div>
                  <div class="drug-row">
                    <a-input-number v-model:value="drug.quantity" :min="1" size="small" style="width: 100px" />
                    <a-input v-model:value="drug.usageInstruction" placeholder="用法" size="small" style="flex:1; margin-left:5px" />
                  </div>
                </div>
              </div>
              <a-button type="dashed" size="small" block @click="addPrescription">+ 添加药品</a-button>
            </div>
          </a-tab-pane>
          
          <a-tab-pane key="lab" tab="检查">
            <div class="tool-content">
              <div class="lab-list">
                <div class="lab-item" v-for="(item, idx) in labOrders" :key="idx">
                  <div class="lab-row">
                    <a-auto-complete
                      v-model:value="item.itemName"
                      :options="commonLabItems"
                      placeholder="检查项目"
                      size="small"
                      style="width: 100%"
                      @select="(val) => onLabSelect(val, item)"
                    />
                    <a-button type="text" danger size="small" @click="labOrders.splice(idx, 1)"><template #icon><DeleteOutlined /></template></a-button>
                  </div>
                  <div class="lab-row">
                    <a-input-number v-model:value="item.price" :min="0" size="small" placeholder="价格" style="width: 100%" />
                  </div>
                </div>
              </div>
              <a-button type="dashed" size="small" block @click="addLabOrder">+ 添加检查</a-button>
            </div>
          </a-tab-pane>
          
          <a-tab-pane key="history" tab="历史">
            <div class="history-list">
              <a-timeline>
                <a-timeline-item
                  v-for="(record, index) in historyRecords"
                  :key="index">
                  <a-card class="history-card" size="small">
                    <template #title>
                      <span>{{ formatDate(record.createTime) }}</span>
                    </template>
                    <h4>{{ record.diagnosis }}</h4>
                    <p>{{ record.symptom }}</p>
                  </a-card>
                </a-timeline-item>
              </a-timeline>
              <div v-if="historyRecords.length === 0" class="empty-text">暂无历史记录</div>
            </div>
          </a-tab-pane>

          <a-tab-pane key="templates" tab="模板">
            <div class="template-list">
              <div class="template-item" v-for="tpl in templates" :key="tpl.tplId" @click="applyTemplate(tpl)">
                <div class="tpl-name">{{ tpl.name }}</div>
                <a-tag size="small">{{ tpl.type === 'EMR' ? '病历' : '处方' }}</a-tag>
              </div>
            </div>
          </a-tab-pane>
        </a-tabs>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { BellOutlined, DeleteOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { doctorApi, emrApi, pharmacyApi, authApi } from '@/utils/api'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const doctorId = userStore.userId

const queue = ref([])
const currentPatient = ref(null)
const activeTab = ref('prescription') // Default to prescription tab in right panel
const calling = ref(false)
const saving = ref(false)
const drugs = ref([])
const templates = ref([])
const historyRecords = ref([])

const emrForm = reactive({
  symptom: '',
  diagnosis: '',
  content: ''
})

const prescriptions = ref([])
const labOrders = ref([])

// Common lab items for autocomplete
const commonLabItems = [
  { value: '血常规', price: 20 },
  { value: '尿常规', price: 15 },
  { value: '肝功能', price: 80 },
  { value: '肾功能', price: 60 },
  { value: '心电图', price: 30 },
  { value: '胸部CT', price: 280 },
  { value: '腹部B超', price: 120 },
  { value: '核磁共振(MRI)', price: 600 }
]

const onLabSelect = (val, item) => {
  const match = commonLabItems.find(i => i.value === val)
  if (match) {
    item.price = match.price
  }
}

onMounted(async () => {
  loadQueue()
  loadDrugs()
  loadTemplates()
})

const loadQueue = async () => {
  try {
    const list = await doctorApi.getQueue(doctorId)
    queue.value = list
  } catch (e) {
    console.error('Failed to load queue', e)
  }
}

const loadDrugs = async () => {
  try {
    drugs.value = await pharmacyApi.getDrugs()
  } catch (e) {
    console.error('Failed to load drugs', e)
  }
}

const loadTemplates = async () => {
  try {
    const deptId = userStore.userInfo.deptId
    templates.value = await emrApi.getTemplates(deptId)
  } catch (e) {
    console.error('Failed to load templates', e)
  }
}

const loadHistory = async (patientId) => {
  try {
    const res = await emrApi.getByPatient(patientId)
    historyRecords.value = res || []
  } catch (e) {
    console.error('Failed to load history', e)
    historyRecords.value = []
  }
}

const selectPatient = async (patient) => {
  currentPatient.value = patient
  // Reset form
  Object.assign(emrForm, { symptom: '', diagnosis: '', content: '' })
  prescriptions.value = []
  labOrders.value = []
  
  // Load patient details if needed
  try {
    const pInfo = await authApi.getPatient(patient.patientId)
    if (pInfo) {
      currentPatient.value = { ...patient, ...pInfo }
    }
  } catch (e) {
    // Ignore if failed
  }
  
  // Load history
  loadHistory(patient.patientId)
}

const callNext = async () => {
  calling.value = true
  try {
    const next = await doctorApi.callNext(doctorId)
    if (next) {
      message.success("请"+next+"号患者就诊")
      loadQueue()
      selectPatient(next)
    } else {
      message.info('暂无候诊患者')
    }
  } catch (e) {
    message.error('叫号失败')
  } finally {
    calling.value = false
  }
}

const addPrescription = () => {
  prescriptions.value.push({ drugId: null, quantity: 1, usageInstruction: '' })
}

const addLabOrder = () => {
  labOrders.value.push({ itemName: '', price: 0 })
}

// Watch for lab item selection to auto-fill price
watch(labOrders, (newVal) => {
  newVal.forEach(item => {
    const match = commonLabItems.find(i => i.value === item.itemName)
    if (match && item.price === 0) {
      item.price = match.price
    }
  })
}, { deep: true })

const applyTemplate = (tpl) => {
  if (tpl.type === 'EMR') {
    // Parse content if it's JSON, otherwise just text
    try {
      const content = JSON.parse(tpl.content)
      emrForm.symptom = content.symptom || emrForm.symptom
      emrForm.diagnosis = content.diagnosis || emrForm.diagnosis
      emrForm.content = content.content || emrForm.content
    } catch (e) {
      emrForm.content = tpl.content
    }
    message.success('已应用病历模板')
  } else {
    message.info('处方模板暂未实现')
  }
}

const saveEmr = async () => {
  if (!currentPatient.value) return
  
  saving.value = true
  try {
    await emrApi.save({
      regId: currentPatient.value.regId,
      ...emrForm,
      prescriptions: prescriptions.value.filter(p => p.drugId),
      labOrders: labOrders.value.filter(l => l.itemName)
    })
    message.success('病历保存成功')
  } catch (e) {
    message.error(e.message || '保存失败')
  } finally {
    saving.value = false
  }
}

const completeVisit = async () => {
  if (!currentPatient.value) return
  try {
    await doctorApi.complete(currentPatient.value.regId)
    message.success('就诊完成')
    currentPatient.value = null
    loadQueue()
  } catch (e) {
    message.error('操作失败')
  }
}

const getStatusText = (status) => {
  const map = { 1: '待就诊', 2: '候诊中', 3: '就诊中' }
  return map[status] || '未知'
}

const formatDate = (str) => {
  if (!str) return ''
  return new Date(str).toLocaleDateString()
}
</script>

<style scoped lang="scss">
.workstation {
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 10px;
  box-sizing: border-box;

  .workstation-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 10px;
    
    h2 {
      margin: 0;
      font-size: 18px;
    }
  }
  
  .workstation-content {
    flex: 1;
    display: flex;
    gap: 10px;
    overflow: hidden;
  }

  .left-panel {
    width: 20%;
    display: flex;
    flex-direction: column;
    gap: 10px;
  }

  .middle-panel {
    width: 50%;
    display: flex;
    flex-direction: column;
  }

  .right-panel {
    width: 30%;
    display: flex;
    flex-direction: column;
    background: #fff;
    border-radius: 4px;
    overflow: hidden;
  }

  .panel-card {
    background: #fff;
    border-radius: 4px;
    display: flex;
    flex-direction: column;
    overflow: hidden;
    box-shadow: 0 1px 4px rgba(0,0,0,0.05);

    &.queue-panel {
      flex: 2;
    }
    &.patient-info-panel {
      flex: 1;
    }
    &.emr-editor {
      height: 100%;
    }

    .panel-header {
      padding: 10px 15px;
      background: #f5f7fa;
      border-bottom: 1px solid #e4e7ed;
      font-weight: bold;
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
  }

  .queue-list {
    flex: 1;
    overflow-y: auto;
    padding: 5px;

    .queue-item {
      display: flex;
      align-items: center;
      padding: 8px;
      border-radius: 4px;
      cursor: pointer;
      margin-bottom: 5px;
      
      &:hover {
        background: #f0f9eb;
      }
      &.current {
        background: #ecf5ff;
        border-left: 3px solid #409eff;
      }

      .number {
        font-size: 16px;
        font-weight: bold;
        width: 30px;
        color: #409eff;
      }
      .info {
        flex: 1;
        .name { font-weight: 500; }
        .status { font-size: 12px; color: #909399; }
      }
    }
  }

  .info-content {
    padding: 15px;
    p {
      margin: 5px 0;
      font-size: 14px;
    }
  }

  .emr-form-container {
    flex: 1;
    overflow-y: auto;
    padding: 15px;
    
    .emr-actions {
      margin-top: 20px;
      display: flex;
      justify-content: flex-end;
    }
  }

  .tools-tabs {
    height: 100%;
    display: flex;
    flex-direction: column;
    border: none;
    
    :deep(.ant-tabs-content) {
      flex: 1;
      overflow-y: auto;
      padding: 10px;
    }
  }

  .tool-content {
    .drug-item, .lab-item {
      background: #f8f9fa;
      padding: 8px;
      border-radius: 4px;
      margin-bottom: 8px;
      
      .drug-row, .lab-row {
        display: flex;
        gap: 5px;
        margin-bottom: 5px;
        &:last-child { margin-bottom: 0; }
      }
    }
  }

  .history-list {
    padding: 10px;
    .history-card {
      margin-bottom: 5px;
      h4 { margin: 0 0 5px 0; font-size: 14px; }
      p { margin: 0; font-size: 12px; color: #666; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
    }
    .empty-text {
      text-align: center;
      color: #909399;
      margin-top: 20px;
    }
  }

  .template-list {
    .template-item {
      padding: 10px;
      border-bottom: 1px solid #eee;
      cursor: pointer;
      display: flex;
      justify-content: space-between;
      align-items: center;
      
      &:hover { background: #f5f7fa; }
      .tpl-name { font-size: 14px; }
    }
  }
  
  .no-patient {
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
    background: #fff;
    border-radius: 4px;
  }
}
</style>
