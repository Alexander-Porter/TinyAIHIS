<template>
  <div class="workstation">
    <div class="workstation-content">
      <!-- Left: Patient List -->
      <div class="left-panel">
        <div class="panel-card patient-list-panel">
          <div class="panel-header">
            <div class="header-title">
              <span>今日患者</span>
              <a-tag color="blue">{{ patients.length }}</a-tag>
            </div>
            <a-button type="primary" size="small" @click="callNext" :loading="calling">
              <template #icon><BellOutlined /></template> 叫号
            </a-button>
          </div>
          <!-- 患者状态筛选 -->
          <div class="patient-filter">
            <a-radio-group v-model:value="patientFilter" size="small" button-style="solid">
              <a-radio-button value="all">全部</a-radio-button>
              <a-radio-button value="waiting">待诊 ({{ waitingCount }})</a-radio-button>
              <a-radio-button value="consulting">就诊中</a-radio-button>
              <a-radio-button value="completed">已完成</a-radio-button>
            </a-radio-group>
          </div>
          <div class="patient-list">
            <div class="patient-empty" v-if="filteredPatients.length === 0">
              暂无患者
            </div>
            <div 
              class="patient-item" 
              v-for="p in filteredPatients" 
              :key="p.regId"
              :class="{ 
                current: currentPatient?.regId === p.regId,
                'in-consultation': p.status === 3,
                'waiting': p.status === 1 || p.status === 2,
                'completed': p.status === 4
              }"
              @click="selectPatient(p)">
              <div class="patient-number">{{ p.queueNumber || '-' }}</div>
              <div class="patient-info">
                <div class="name">{{ p.patientName || '患者#' + p.patientId }}</div>
                <div class="meta">
                  <span>{{ p.gender === 1 ? '男' : '女' }}</span>
                  <span v-if="p.age">{{ p.age }}岁</span>
                </div>
              </div>
              <div class="patient-status">
                <a-tag :color="getStatusColor(p.status)" size="small">
                  {{ getStatusText(p) }}
                </a-tag>
                <a-tag v-if="p.labOrders?.some(l => l.status === 2)" color="success" size="small">
                  检查完成
                </a-tag>
              </div>
            </div>
          </div>
        </div>
      </div>
      
      <!-- Middle: Main Workspace -->
      <div class="middle-panel">
        <template v-if="currentPatient">
          <!-- Patient Action Bar -->
          <div class="action-header">
            <div class="patient-basic">
              <span class="name">{{ currentPatient.patientName }}</span>
              <span class="meta">{{ currentPatient.gender === 1 ? '男' : '女' }} {{ currentPatient.age }}岁</span>
              <a-tag v-if="currentPatient.status === 3" color="processing">就诊中</a-tag>
              <a-tag v-else color="warning">不处在就诊状态</a-tag>
            </div>
            <div class="actions">
              <a-button v-if="currentPatient.status === 3" size="small" @click="pauseCurrentPatient">
                <template #icon><PauseOutlined /></template> 暂停
              </a-button>
              <a-button v-if="currentPatient.status === 2" type="primary" size="small" @click="resumeCurrentPatient">
                <template #icon><PlayCircleOutlined /></template> 继续
              </a-button>
              <a-button type="primary" size="small" @click="completeVisit" :disabled="currentPatient.status !== 3">
                <template #icon><CheckOutlined /></template> 完成就诊
              </a-button>
            </div>
          </div>

          <!-- Main Tabs -->
          <div class="panel-card content-card">
            <a-tabs v-model:activeKey="mainTab" type="card" class="workspace-tabs">
              <!-- Tab栏右侧按钮 -->
              <template #rightExtra>
                <div class="tab-extra-buttons">
                  <a-button v-if="mainTab === 'emr'" type="primary" size="small" @click="saveEmrFromTemplate" :loading="saving">
                    <template #icon><SaveOutlined /></template> 保存病历
                  </a-button>
                  <a-button v-if="mainTab === 'emr'" size="small" @click="printEmr">
                    <template #icon><PrinterOutlined /></template> 打印病历
                  </a-button>
                  <a-button v-if="mainTab === 'lab'" size="small" @click="printLabOrders" :disabled="labOrders.length === 0">
                    <template #icon><PrinterOutlined /></template> 打印申请单
                  </a-button>
                  <a-button v-if="mainTab === 'prescription'" size="small" @click="printPrescription" :disabled="prescriptions.length === 0">
                    <template #icon><PrinterOutlined /></template> 打印处方笺
                  </a-button>
                </div>
              </template>
              <!-- Tab 1: EMR -->
              <a-tab-pane key="emr" tab="电子病历">
                <div class="tab-content emr-tab-content">
                  <!-- 电子病历编辑器 -->
                  <div class="emr-editor-wrapper">
                    <EmrEditor 
                      ref="emrEditorRef"
                      v-model="emrTemplateData"
                      :patientInfo="currentPatientInfo"
                      :hospitalName="'武汉大学医院'"
                      :labOrders="labOrders"
                      :prescriptions="prescriptions"
                      :drugs="drugs"
                      @print="handleEmrPrint"
                    />
                  </div>
                </div>
              </a-tab-pane>

              <!-- Tab 2: Lab Orders -->
              <a-tab-pane key="lab" tab="检查检验">
                <div class="tab-content">
                  <div class="section-header">
                    <span>已开项目</span>
                    <div class="header-actions">
                      <a-button type="primary" size="small" @click="addLabOrder">+ 开立检查</a-button>
                    </div>
                  </div>
                  <div class="lab-list">
                    <div v-if="labOrders.length === 0" class="empty-tip">暂无检查项目</div>
                    <div class="lab-item" v-for="(lab, idx) in labOrders" :key="idx">
                      <div class="lab-row">
                        <div class="lab-info">
                          <a-auto-complete
                            v-model:value="lab.itemName"
                            :options="commonLabItems"
                            placeholder="检查项目名称"
                            style="width: 200px"
                            @select="(val) => onLabSelect(val, lab)"
                            :disabled="!!lab.orderId"
                          />
                          <span class="price">¥{{ lab.price }}</span>
                        </div>
                        <div class="lab-status">
                          <a-tag :color="getLabStatusColor(lab.status)">{{ getLabStatusText(lab.status) }}</a-tag>
                          <a-button 
                            v-if="lab.status === 2" 
                            type="link" 
                            size="small" 
                            @click="viewReport(lab)"
                          >查看报告</a-button>
                          <a-button 
                            v-if="!lab.orderId" 
                            type="text" 
                            danger 
                            @click="labOrders.splice(idx, 1)"
                          >
                            <template #icon><DeleteOutlined /></template>
                          </a-button>
                        </div>
                      </div>
                    </div>
                  </div>
                  <div class="form-actions" v-if="labOrders.some(l => !l.orderId)">
                    <a-button type="primary" @click="saveEmr" :loading="saving">提交申请</a-button>
                  </div>
                </div>
              </a-tab-pane>

              <!-- Tab 3: Prescriptions -->
              <a-tab-pane key="prescription" tab="处方开具">
                <div class="tab-content">
                  <div class="section-header">
                    <span>处方明细</span>
                    <div class="header-actions">
                      <a-button type="primary" size="small" @click="addPrescription">+ 添加药品</a-button>
                    </div>
                  </div>
                  <div class="prescription-list">
                    <div v-if="prescriptions.length === 0" class="empty-tip">暂无处方</div>
                    <a-table 
                      :dataSource="prescriptions" 
                      :columns="prescriptionColumns" 
                      size="small" 
                      :pagination="false"
                      rowKey="idx"
                    >
                      <template #bodyCell="{ column, record, index }">
                        <template v-if="column.key === 'drug'">
                          <a-select 
                            v-model:value="record.drugId" 
                            placeholder="选择药品" 
                            show-search 
                            option-filter-prop="label" 
                            style="width: 100%"
                            :disabled="!!record.presId"
                          >
                            <a-select-option v-for="d in drugs" :key="d.drugId" :value="d.drugId" :label="d.name">{{ d.name }}</a-select-option>
                          </a-select>
                        </template>
                        <template v-if="column.key === 'quantity'">
                          <a-input-number v-model:value="record.quantity" :min="1" size="small" :disabled="!!record.presId" />
                        </template>
                        <template v-if="column.key === 'usage'">
                          <a-input v-model:value="record.usageInstruction" size="small" :disabled="!!record.presId" />
                        </template>
                        <template v-if="column.key === 'action'">
                          <a-button v-if="!record.presId" type="text" danger size="small" @click="prescriptions.splice(index, 1)">
                            <template #icon><DeleteOutlined /></template>
                          </a-button>
                        </template>
                      </template>
                    </a-table>
                  </div>
                  <div class="form-actions" v-if="prescriptions.some(p => !p.presId)">
                    <a-button type="primary" @click="saveEmr" :loading="saving">发送处方</a-button>
                  </div>
                </div>
              </a-tab-pane>

              <!-- Tab 4: History -->
              <a-tab-pane key="history" tab="历史就诊">
                <div class="history-content">
                  <a-spin :spinning="loadingHistory">
                    <div v-if="historyRecords.length === 0" class="empty-tip">暂无历史就诊记录</div>
                    <a-collapse v-else accordion>
                      <a-collapse-panel v-for="(record, idx) in historyRecords" :key="idx">
                        <template #header>
                          <div class="history-header">
                            <span class="date">{{ record.scheduleDate }}</span>
                            <span class="diagnosis">{{ record.medicalRecord?.diagnosis || '未诊断' }}</span>
                          </div>
                        </template>
                        <div class="history-detail">
                          <div class="detail-row">
                            <label>主诉：</label>
                            <span>{{ record.medicalRecord?.symptom || '-' }}</span>
                          </div>
                          <div class="detail-row">
                            <label>诊断：</label>
                            <span>{{ record.medicalRecord?.diagnosis || '-' }}</span>
                          </div>
                          <div class="detail-row">
                            <label>病历内容：</label>
                            <span>{{ record.medicalRecord?.content || '-' }}</span>
                          </div>
                          <div class="detail-row" v-if="record.labOrders?.length">
                            <label>检查项目：</label>
                            <div class="sub-list">
                              <div v-for="lab in record.labOrders" :key="lab.orderId" class="history-lab-item">
                                <span class="lab-name">{{ lab.itemName }}</span>
                                <div class="lab-actions">
                                  <span v-if="!lab.resultText" class="no-result">无结果</span>
                                  <template v-else>
                                    <a-button type="link" size="small" @click="viewReport(lab)">查看报告</a-button>
                                    <a-button type="link" size="small" @click="insertLabResult(lab)">引用</a-button>
                                  </template>
                                </div>
                              </div>
                            </div>
                          </div>
                          <div class="detail-row" v-if="record.prescriptions?.length">
                            <label>处方：</label>
                            <div class="sub-list">
                              <div v-for="p in record.prescriptions" :key="p.presId">
                                {{ p.drugName }} x{{ p.quantity }} ({{ p.usageInstruction }})
                              </div>
                            </div>
                          </div>
                        </div>
                      </a-collapse-panel>
                    </a-collapse>
                  </a-spin>
                </div>
              </a-tab-pane>
            </a-tabs>
          </div>
        </template>

        <div class="no-patient" v-else>
          <a-empty description="请点击左侧叫号开始接诊" />
        </div>
      </div>

      <!-- Right: Tools -->
      <div class="right-panel" v-if="currentPatient">
        <div class="panel-card right-tools-panel">
          <a-tabs type="card" size="small" class="right-tabs">
            <a-tab-pane key="ai" tab="AI助手">
              <DoctorAiChat 
                :initialQuery="aiInitialQuery" 
                :patientId="currentPatient?.patientId"
                @apply="applyAiSuggestion" 
                style="height: calc(100vh - 180px); display: flex; flex-direction: column;"
              />
            </a-tab-pane>
            <a-tab-pane key="tpl" tab="常用模板">
              <div class="template-list">
                <div class="template-item" v-for="tpl in templates" :key="tpl.tplId" @click="applyTemplate(tpl)">
                  <div class="tpl-name">{{ tpl.name }}</div>
                  <a-tag size="small">{{ tpl.type === 'EMR' ? '病历' : '处方' }}</a-tag>
                </div>
                <div v-if="templates.length === 0" class="empty-tip">暂无模板</div>
              </div>
            </a-tab-pane>
          </a-tabs>
        </div>
      </div>
    </div>

    <!-- Report Viewer Modal -->
    <a-modal v-model:open="reportVisible" title="检查报告" width="800px">
      <div class="report-viewer" v-if="currentReport">
        <div class="report-header">
          <h3>{{ currentReport.itemName }}</h3>
          <span class="time">{{ currentReport.resultTime }}</span>
        </div>
        <div class="report-body" v-html="currentReport.resultText"></div>
      </div>
      <template #footer>
        <div style="display: flex; justify-content: space-between; align-items: center;">
          <span style="color: #999; font-size: 12px;">提示：选中报告内容可插入到病历</span>
          <div>
            <a-button @click="reportVisible = false">关闭</a-button>
            <a-button type="primary" @click="insertReportSelection">插入选中内容到查体</a-button>
          </div>
        </div>
      </template>
    </a-modal>
    
    <!-- Knowledge Search Popup -->
    <KnowledgeSearchPopup 
      v-model="knowledgeSearchVisible"
      :initialQuery="knowledgeSearchQuery"
      @insert="handleKnowledgeInsert"
    />
    
    <!-- Context Menu for Text Selection -->
    <teleport to="body">
      <div
        v-if="contextMenuVisible"
        class="context-menu"
        :style="{ top: contextMenuY + 'px', left: contextMenuX + 'px' }"
        ref="contextMenuRef"
        @mousedown.stop="handleContextMenuMouseDown"
      >
        <div class="context-menu-item" @click="searchSelectedText">
          <SearchOutlined /> 搜索知识库
        </div>
      </div>
    </teleport>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount, computed } from 'vue'
import { BellOutlined, DeleteOutlined, SaveOutlined, CheckOutlined, PauseOutlined, PlayCircleOutlined, PrinterOutlined, RobotOutlined, SearchOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { doctorApi, emrApi, pharmacyApi, checkItemApi } from '@/utils/api'
import { useUserStore } from '@/stores/user'
import SockJS from 'sockjs-client'
import { Client } from '@stomp/stompjs'
import EmrEditor from '@/components/EmrEditor.vue'
import PrintTemplates from '@/components/PrintTemplates.vue'
import DoctorAiChat from '@/components/DoctorAiChat.vue'
import KnowledgeSearchPopup from '@/components/KnowledgeSearchPopup.vue'
import QRCode from 'qrcode'

const userStore = useUserStore()
const doctorId = userStore.userId
let stompClient = null

// State
const patients = ref([])
const patientFilter = ref('all')
const currentPatient = ref(null)
const mainTab = ref('emr')
const calling = ref(false)
const saving = ref(false)
const loadingHistory = ref(false)
const reportVisible = ref(false)
const currentReport = ref(null)
const aiDrawerVisible = ref(false)
const aiInitialQuery = ref('')
const knowledgeSearchVisible = ref(false)
const knowledgeSearchQuery = ref('')
const contextMenuVisible = ref(false)
const contextMenuX = ref(0)
const contextMenuY = ref(0)
const selectedText = ref('')
const contextMenuRef = ref(null)
const suppressHideOnSelection = ref(false)

const drugs = ref([])
const templates = ref([])
const historyRecords = ref([])

const emrForm = reactive({
  symptom: '',
  diagnosis: '',
  content: ''
})

const emrEditorRef = ref(null)
const emrTemplateData = ref({})

const prescriptions = ref([])
const labOrders = ref([])

// Table Columns
const prescriptionColumns = [
  { title: '药品名称', key: 'drug', width: '40%' },
  { title: '数量', key: 'quantity', width: '15%' },
  { title: '用法用量', key: 'usage', width: '35%' },
  { title: '', key: 'action', width: '10%' }
]

// Computed
const today = computed(() => new Date().toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' }))

const shiftLabel = computed(() => {
  const hour = new Date().getHours()
  if (hour < 12) return '上午班'
  if (hour < 18) return '下午班'
  return '急诊班'
})
const isEmergency = (reg) => {
  return reg.shiftType === 'ER' || reg.shiftType === '急诊(全天)' || reg.shiftType === '急诊'
}
// 患者状态: 0=待缴费, 1=待签到, 2=候诊中, 3=就诊中, 4=已完成, 5=已取消
const waitingCount = computed(() => patients.value.filter(p => p.status === 1 || p.status === 2).length)

const filteredPatients = computed(() => {
  if (patientFilter.value === 'all') return patients.value
  if (patientFilter.value === 'waiting') return patients.value.filter(p => p.status === 1 || p.status === 2)
  if (patientFilter.value === 'consulting') return patients.value.filter(p => p.status === 3)
  if (patientFilter.value === 'completed') return patients.value.filter(p => p.status === 4)
  return patients.value
})

const getStatusColor = (status) => {
  const colors = { 0: 'default', 1: 'orange', 2: 'blue', 3: 'processing', 4: 'success', 5: 'default' }
  return colors[status] || 'default'
}

const getStatusText = (parent) => {
  const status = parent.status
  const isER=isEmergency(parent)
  if (isER) {
    const textsER = { 0: '待缴费(急诊)', 1: '候诊中(急诊)', 2: '候诊中(急诊)', 3: '就诊中(急诊)', 4: '已完成(急诊)', 5: '已取消(急诊)' }
    return textsER[status] || '未知(急诊)'
  }
  const texts = { 0: '待缴费', 1: '待签到', 2: '候诊中', 3: '就诊中', 4: '已完成', 5: '已取消' }
  return texts[status] || '未知'
}

// 当前患者信息（用于模板自动填充）
const currentPatientInfo = computed(() => {
  if (!currentPatient.value) return {}
  return {
    patientName: currentPatient.value.patientName,
    gender: currentPatient.value.gender,
    age: currentPatient.value.age,
    deptName: currentPatient.value.deptName,
    regId: currentPatient.value.regId,
    recordNo: currentPatient.value.regId,
    visitTime: new Date().toLocaleString('zh-CN')
  }
})

// Common lab items for autocomplete
const commonLabItems = ref([])

// Lifecycle
const openAiAssistant = () => {
  // Build initial query from current EMR
  let query = ''
  if (emrForm.symptom) query += `主诉：${emrForm.symptom}\n`
  if (emrForm.content) query += `现病史：${emrForm.content}\n`
  if (emrForm.diagnosis) query += `初步诊断：${emrForm.diagnosis}\n`
  
  if (!query) {
    query = '患者刚入院，请协助问诊。'
  }
  
  aiInitialQuery.value = query
  aiDrawerVisible.value = true
}

const applyAiSuggestion = (content) => {
  // Simple append for now
  emrForm.content += '\n\n' + content
  message.success('已引用到病历')
}

// Knowledge search handlers
const handleKnowledgeInsert = (doc) => {
  if (emrEditorRef.value) {
    // Insert knowledge reference into EMR
    const reference = `\n\n【参考文献】\n疾病：${doc.diseaseName}\n科室：${doc.department}\n`
    emrEditorRef.value.insertText(reference)
  }
}

const searchSelectedText = () => {
  contextMenuVisible.value = false
  if (selectedText.value.trim()) {
    knowledgeSearchQuery.value = selectedText.value
    knowledgeSearchVisible.value = true
  }
}

const handleSelectionChange = () => {
  // Only react to new selections; don't auto-hide here to avoid killing clicks on the menu
  if (suppressHideOnSelection.value) return

  const selection = window.getSelection()
  if (!selection || selection.isCollapsed) return

  const text = selection.toString().trim()
  if (!text) return

  // Position the action button near the selected text
  const range = selection.getRangeAt(0)
  const rect = range.getBoundingClientRect()

  selectedText.value = text
  contextMenuX.value = rect.right + window.scrollX
  contextMenuY.value = rect.bottom + window.scrollY + 8
  contextMenuVisible.value = true
}

const handleClickOutside = (e) => {
  const menuEl = contextMenuRef.value
  if (menuEl && menuEl.contains(e.target)) return

  const selection = window.getSelection()
  const hasSelection = selection && !selection.isCollapsed && selection.toString().trim()
  if (hasSelection) return

  contextMenuVisible.value = false
}

const handleContextMenuMouseDown = () => {
  // Prevent selectionchange handler from immediately hiding the menu while clicking inside it
  suppressHideOnSelection.value = true
  // Reset shortly after to allow future selection changes to hide as normal
  setTimeout(() => { suppressHideOnSelection.value = false }, 0)
}

onMounted(async () => {
  await Promise.all([
    loadPatients(),
    loadDrugs(),
    loadTemplates(),
    loadCheckItems()
  ])
  connectWebSocket()
  
  // Show action when any text is selected
  document.addEventListener('selectionchange', handleSelectionChange)
  document.addEventListener('click', handleClickOutside)
})

onBeforeUnmount(() => {
  if (stompClient) {
    stompClient.deactivate()
  }
  
  // Remove text selection listeners
  document.removeEventListener('selectionchange', handleSelectionChange)
  document.removeEventListener('click', handleClickOutside)
})

// WebSocket connection
const connectWebSocket = () => {
  const deptId = userStore.userInfo?.deptId
  if (!deptId) return

  try {
    stompClient = new Client({
      webSocketFactory: () => new SockJS('/ws'),
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000
    })

    stompClient.onConnect = () => {
      console.log('Doctor workstation WebSocket connected')
      stompClient.subscribe(`/topic/queue/${deptId}`, (msg) => {
        console.log('Queue update received:', msg.body)
        loadPatients()
      })
    }

    stompClient.onStompError = (frame) => {
      console.error('STOMP error', frame)
    }

    stompClient.activate()
  } catch (e) {
    console.error('WebSocket connection failed', e)
  }
}

// Methods
const loadPatients = async () => {
  try {
    patients.value = await doctorApi.getTodayPatients(doctorId)
  } catch (e) {
    console.error('Failed to load patients', e)
    patients.value = []
  }
}

const loadDrugs = async () => {
  try {
    drugs.value = await pharmacyApi.getDrugs()
  } catch (e) {
    console.error('Failed to load drugs', e)
  }
}

const loadCheckItems = async () => {
  try {
    const items = await checkItemApi.getAll()
    commonLabItems.value = items.map(item => ({
      value: item.itemName,
      price: item.price,
      label: item.itemName
    }))
  } catch (e) {
    console.error('Failed to load check items', e)
  }
}

const loadTemplates = async () => {
  try {
    const deptId = userStore.userInfo?.deptId
    templates.value = await emrApi.getTemplates(deptId)
  } catch (e) {
    console.error('Failed to load templates', e)
  }
}

const loadHistory = async (patientId) => {
  loadingHistory.value = true
  try {
    historyRecords.value = await doctorApi.getPatientHistory(patientId, doctorId)
  } catch (e) {
    console.error('Failed to load history', e)
    historyRecords.value = []
  } finally {
    loadingHistory.value = false
  }
}

const selectPatient = async (patient) => {
  currentPatient.value = patient
  mainTab.value = 'emr'
  
  // Reset form
  Object.assign(emrForm, { symptom: '', diagnosis: '', content: '' })
  emrTemplateData.value = {}
  
  // Load existing EMR data if any
  if (patient.medicalRecord) {
    emrForm.symptom = patient.medicalRecord.symptom || ''
    emrForm.diagnosis = patient.medicalRecord.diagnosis || ''
    emrForm.content = patient.medicalRecord.content || ''
    
    // Parse content back to template data
    const parsed = parseEmrContent(patient.medicalRecord.content)
    emrTemplateData.value = {
      symptom: patient.medicalRecord.symptom || parsed.symptom || '',
      diagnosis: patient.medicalRecord.diagnosis || parsed.diagnosis || '',
      presentHistory: parsed.presentHistory || '',
      pastHistory: parsed.pastHistory || '',
      physicalExam: parsed.physicalExam || '',
      treatment: parsed.treatment || ''
    }
  }
  
  // Load prescriptions and lab orders
  prescriptions.value = (patient.prescriptions || []).map(p => ({
    presId: p.presId,
    drugId: p.drugId,
    quantity: p.quantity,
    usageInstruction: p.usageInstruction
  }))
  
  labOrders.value = (patient.labOrders || []).map(l => ({
    orderId: l.orderId,
    itemName: l.itemName,
    price: l.price,
    status: l.status,
    resultText: l.resultText,
    resultTime: l.resultTime
  }))
  
  // Load history in background
  loadHistory(patient.patientId)
}

const viewReport = (lab) => {
  if (lab.status === 2 && lab.resultText) {
    currentReport.value = lab
    reportVisible.value = true
  } else {
    message.info('暂无报告内容')
  }
}

const insertReportSelection = () => {
  const selection = window.getSelection()
  if (!selection || selection.rangeCount === 0 || selection.isCollapsed) {
    message.warning('请先在报告中选中要插入的内容')
    return
  }
  
  const range = selection.getRangeAt(0)
  const fragment = range.cloneContents()
  
  // Parse fragment to text
  const text = parseHtmlToText(fragment).trim()
  
  if (!text) {
    message.warning('选中的内容为空')
    return
  }
  
  // Insert into physical exam
  if (!emrTemplateData.value.physicalExam) {
    emrTemplateData.value.physicalExam = ''
  }
  
  if (emrTemplateData.value.physicalExam) {
    emrTemplateData.value.physicalExam += '\n' + text
  } else {
    emrTemplateData.value.physicalExam = text
  }
  
  message.success('已插入到查体')
}

const parseHtmlToText = (node) => {
  let text = ''
  
  if (node.nodeType === Node.TEXT_NODE) {
    return node.textContent
  }
  
  if (node.nodeType === Node.ELEMENT_NODE) {
    const tagName = node.tagName.toLowerCase()
    const isBlock = ['div', 'p', 'tr', 'h1', 'h2', 'h3', 'li', 'br'].includes(tagName)
    
    if (tagName === 'br') {
      return '\n'
    }
    
    for (let child of node.childNodes) {
      text += parseHtmlToText(child)
    }
    
    if (tagName === 'tr') {
      text += '\n'
    } else if (tagName === 'td' || tagName === 'th') {
      text += '  '
    } else if (isBlock) {
      text += '\n'
    }
  } else if (node.nodeType === Node.DOCUMENT_FRAGMENT_NODE) {
     for (let child of node.childNodes) {
      text += parseHtmlToText(child)
    }
  }
  
  // Clean up multiple newlines
  return text.replace(/\n\s*\n/g, '\n')
}

const insertLabResult = (lab) => {
  const text = `${lab.itemName}: ${lab.resultText || '无结果'}`;
  // Append to physicalExam (查体) or create a new line
  if (emrTemplateData.value.physicalExam) {
    emrTemplateData.value.physicalExam += `\n${text}`;
  } else {
    emrTemplateData.value.physicalExam = text;
  }
  message.success('已插入检查结果');
}

const callNext = async () => {
  calling.value = true
  try {
    const next = await doctorApi.callNext(doctorId)
    if (next) {
      message.success(`请 ${next.queueNumber} 号患者就诊`)
      await loadPatients()
      // Find and select the called patient
      const found = patients.value.find(p => p.regId === next.regId)
      if (found) {
        selectPatient(found)
      }
    } else {
      message.info('暂无候诊患者')
    }
  } catch (e) {
    if (e.message !== '当前没有等待的患者') {
      message.error('叫号失败')
    }
  } finally {
    calling.value = false
  }
}

const pauseCurrentPatient = async () => {
  if (!currentPatient.value) return
  try {
    await doctorApi.pause(currentPatient.value.regId)
    message.success('已暂停接诊，患者可去做检查')
    currentPatient.value.status = 2
    await loadPatients()
  } catch (e) {
    message.error('操作失败')
  }
}

const resumeCurrentPatient = async () => {
  if (!currentPatient.value) return
  try {
    await doctorApi.resume(currentPatient.value.regId)
    message.success('已恢复接诊')
    currentPatient.value.status = 3
    
    // Refresh patient detail to get latest lab results
    const updated = await doctorApi.getVisitDetail(currentPatient.value.regId)
    selectPatient(updated)
    await loadPatients()
  } catch (e) {
    message.error('操作失败')
  }
}

const addPrescription = () => {
  prescriptions.value.push({ drugId: null, quantity: 1, usageInstruction: '' })
}

const addLabOrder = () => {
  labOrders.value.push({ itemName: '', price: 0 })
}

const onLabSelect = (val, item) => {
  const match = commonLabItems.value.find(i => i.value === val)
  if (match) {
    item.price = match.price
  }
}

const getLabStatusColor = (status) => {
  const colors = { 0: 'default', 1: 'processing', 2: 'success' }
  return colors[status] || 'default'
}

const getLabStatusText = (status) => {
  const texts = { 0: '待缴费', 1: '待检查', 2: '已完成' }
  return texts[status] || '待缴费'
}

// 解析病历内容 - 支持 "标签: 内容" 格式
const parseEmrContent = (text) => {
  const result = {
    symptom: '',
    presentHistory: '',
    pastHistory: '',
    physicalExam: '',
    diagnosis: '',
    treatment: ''
  }
  
  if (!text) return result

  // 定义字段映射: 模板标签 -> 字段名
  const fieldMap = {
    '主诉': 'symptom',
    '现病史': 'presentHistory',
    '既往史': 'pastHistory',
    '查体': 'physicalExam',
    '诊断': 'diagnosis',
    '处理': 'treatment'
  }
  
  // 按行分割
  const lines = text.split('\n')
  let currentField = null
  
  for (const line of lines) {
    // 检查是否是新字段开始 (格式: "标签: 内容" 或 "标签:内容")
    let matched = false
    for (const [label, field] of Object.entries(fieldMap)) {
      const regex = new RegExp(`^${label}[：:]\\s*(.*)$`)
      const match = line.match(regex)
      if (match) {
        currentField = field
        result[field] = match[1].trim()
        matched = true
        break
      }
    }
    
    // 如果不是新字段，追加到当前字段
    if (!matched && currentField && line.trim()) {
      result[currentField] += '\n' + line.trim()
    }
  }
  
  return result
}

const applyTemplate = (tpl) => {
  if (tpl.type === 'EMR') {
    const parsed = parseEmrContent(tpl.content)
    
    // 更新 emrForm
    emrForm.symptom = parsed.symptom || emrForm.symptom
    emrForm.diagnosis = parsed.diagnosis || emrForm.diagnosis
    emrForm.content = tpl.content
    
    // 更新电子病历模板数据
    emrTemplateData.value = {
      ...emrTemplateData.value,
      symptom: parsed.symptom || emrTemplateData.value.symptom,
      presentHistory: parsed.presentHistory || emrTemplateData.value.presentHistory,
      pastHistory: parsed.pastHistory || emrTemplateData.value.pastHistory,
      physicalExam: parsed.physicalExam || emrTemplateData.value.physicalExam,
      diagnosis: parsed.diagnosis || emrTemplateData.value.diagnosis,
      treatment: parsed.treatment || emrTemplateData.value.treatment
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
    // Construct content from template data to ensure we have the latest edits
    const templateData = emrTemplateData.value
    const content = [
        templateData.presentHistory ? `现病史：${templateData.presentHistory}` : '',
        templateData.pastHistory ? `既往史：${templateData.pastHistory}` : '',
        templateData.physicalExam ? `查体：${templateData.physicalExam}` : '',
        templateData.treatment ? `处理：${templateData.treatment}` : ''
      ].filter(Boolean).join('\n\n')

    // Use template data if available, otherwise fallback to emrForm
    const symptom = templateData.symptom || emrForm.symptom
    const diagnosis = templateData.diagnosis || emrForm.diagnosis
    // If template data produced content, use it. Otherwise use emrForm.content (which might be the original loaded content)
    // But if user cleared the editor, content would be empty string. We should probably trust templateData if it was initialized.
    const finalContent = content || emrForm.content

    // 只提交新的检查和处方（没有 orderId/presId 的是新项目）
    const newPrescriptions = prescriptions.value.filter(p => p.drugId && !p.presId)
    const newLabOrders = labOrders.value.filter(l => l.itemName && !l.orderId)
    
    await emrApi.save({
      regId: currentPatient.value.regId,
      symptom: symptom,
      diagnosis: diagnosis,
      content: finalContent,
      prescriptions: newPrescriptions,
      labOrders: newLabOrders
    })
    message.success('保存成功')
    
    // Refresh patient detail
    const updated = await doctorApi.getVisitDetail(currentPatient.value.regId)
    selectPatient(updated)
  } catch (e) {
    message.error(e.message || '保存失败')
  } finally {
    saving.value = false
  }
}

// 从电子病历模板保存
const saveEmrFromTemplate = async () => {
  if (!currentPatient.value) return
  
  saving.value = true
  try {
    const templateData = emrTemplateData.value
    // 只提交新的检查和处方（没有 orderId/presId 的是新项目）
    const newPrescriptions = prescriptions.value.filter(p => p.drugId && !p.presId)
    const newLabOrders = labOrders.value.filter(l => l.itemName && !l.orderId)
    
    await emrApi.save({
      regId: currentPatient.value.regId,
      symptom: templateData.symptom || '',
      diagnosis: templateData.diagnosis || '',
      content: [
        templateData.presentHistory ? `现病史：${templateData.presentHistory}` : '',
        templateData.pastHistory ? `既往史：${templateData.pastHistory}` : '',
        templateData.physicalExam ? `查体：${templateData.physicalExam}` : '',
        templateData.treatment ? `处理：${templateData.treatment}` : ''
      ].filter(Boolean).join('\n\n'),
      prescriptions: newPrescriptions,
      labOrders: newLabOrders
    })
    message.success('保存成功')
    
    const updated = await doctorApi.getVisitDetail(currentPatient.value.regId)
    selectPatient(updated)
  } catch (e) {
    message.error(e.message || '保存失败')
  } finally {
    saving.value = false
  }
}

// 打印病历
const printEmr = () => {
  handleEmrPrint(emrTemplateData.value)
}

const handleEmrPrint = async (data) => {
  const printWindow = window.open('', '_blank')
  printWindow.document.write(await generateEmrHtml(data))
  printWindow.document.close()
  printWindow.focus()
  setTimeout(() => {
    printWindow.print()
    printWindow.close()
  }, 300)
}

// 打印处方
const printPrescription = async () => {
  if (prescriptions.value.length === 0) {
    message.warning('暂无处方可打印')
    return
  }
  
  const prescriptionItems = prescriptions.value.map(p => {
    const drug = drugs.value.find(d => d.drugId === p.drugId)
    return {
      drugName: drug?.name || '未知药品',
      spec: drug?.spec || '',
      price: drug?.price || 0,
      quantity: p.quantity,
      unit: drug?.unit || '盒',
      usageInstruction: p.usageInstruction || ''
    }
  })
  
  const printWindow = window.open('', '_blank')
  printWindow.document.write(await generatePrescriptionHtml(prescriptionItems))
  printWindow.document.close()
  printWindow.focus()
  setTimeout(() => {
    printWindow.print()
    printWindow.close()
  }, 300)
}

// 打印检查申请单
const printLabOrders = async () => {
  if (labOrders.value.length === 0) {
    message.warning('暂无检查项目可打印')
    return
  }
  
  const printWindow = window.open('', '_blank')
  printWindow.document.write(await generateLabHtml(labOrders.value))
  printWindow.document.close()
  printWindow.focus()
  setTimeout(() => {
    printWindow.print()
    printWindow.close()
  }, 300)
}

// 生成病历打印HTML - 黑白打印格式
const generateEmrHtml = async (data) => {
  const now = new Date()
  const regId = currentPatient.value?.regId || ''
  const APP_BASE_URL = import.meta.env.VITE_APP_URL || window.location.origin
  const qrUrl = `${APP_BASE_URL}/patient/payment?regId=${regId}`
  const qrSrc = await QRCode.toDataURL(qrUrl, { width: 100, margin: 0 })
  
  return `<!DOCTYPE html>
<html><head><meta charset="utf-8"><title>门诊病历</title>
<style>
  @page { margin: 15mm; size: A4; }
  @media print { * { -webkit-print-color-adjust: exact; print-color-adjust: exact; } }
  body { font-family: 'SimSun', '宋体', serif; font-size: 12pt; line-height: 1.8; color: #000; }
  .paper { max-width: 210mm; margin: 0 auto; position: relative; }
  .header { text-align: center; margin-bottom: 20px; }
  .header h1 { font-size: 22pt; margin: 0 0 10px 0; font-weight: bold; }
  .header h2 { font-size: 16pt; margin: 0; letter-spacing: 5px; }
  hr { border: none; border-top: 1px solid #000; margin: 10px 0; }
  .info-row { margin: 8px 0; }
  .info-row span { margin-right: 20px; }
  .info-row u { text-decoration: none; border-bottom: 1px solid #000; padding: 0 10px; }
  .block { margin: 15px 0; }
  .block-title { font-weight: bold; margin-bottom: 5px; }
  .block-content { padding-left: 20px; white-space: pre-wrap; }
  .diagnosis .block-title { border-bottom: 1px solid #000; display: inline-block; }
  .signature { margin-top: 30px; padding-top: 15px; border-top: 1px solid #000; }
  .qr-code { position: absolute; top: 0; right: 0; text-align: center; }
  .qr-code img { width: 80px; height: 80px; }
  .qr-code div { font-size: 10px; margin-top: 2px; }
</style></head><body>
<div class="paper">
  <div class="qr-code">
    <img src="${qrSrc}" />
    <div>扫码缴费</div>
  </div>
  <div class="header">
    <h1>武汉大学医院</h1>
    <h2>门 诊 病 历</h2>
  </div>
  <div class="info-row">
    <span>科室：<u>${data.deptName || userStore.userInfo?.deptName || ''}</u></span>
    <span>门诊号：<u>${regId}</u></span>
  </div>
  <hr />
  <div class="info-row">
    <span>姓名：<u>${data.patientName || currentPatient.value?.patientName || ''}</u></span>
    <span>性别：${(data.gender ?? currentPatient.value?.gender) === 1 ? '男' : '女'}</span>
    <span>年龄：<u>${data.age || currentPatient.value?.age || ''}</u>岁</span>
  </div>
  <div class="info-row">
    <span>就诊时间：<u>${data.visitTime || now.toLocaleString('zh-CN')}</u></span>
  </div>
  <hr />
  <div class="block">
    <div class="block-title">主 诉</div>
    <div class="block-content">${data.symptom || emrForm.symptom || '-'}</div>
  </div>
  <div class="block">
    <div class="block-title">现病史</div>
    <div class="block-content">${data.presentHistory || emrForm.content || '-'}</div>
  </div>
  <div class="block diagnosis">
    <div class="block-title">诊 断</div>
    <div class="block-content">${data.diagnosis || emrForm.diagnosis || '-'}</div>
  </div>
  <div class="block">
    <div class="block-title">处 理</div>
    <div class="block-content">${data.treatment || '-'}</div>
  </div>
  <div class="signature">
    <span>医师签名：${data.doctorName || userStore.userInfo?.realName || ''}</span>
    <span style="margin-left: 50px">日期：${now.toLocaleDateString('zh-CN')}</span>
  </div>
</div>
</body></html>`
}

// 生成处方笺打印HTML - 黑白打印格式（符合中国医院处方笺标准）
const generatePrescriptionHtml = async (items) => {
  const now = new Date()
  const dateStr = `${now.getFullYear()}年${now.getMonth() + 1}月${now.getDate()}日`
  const total = items.reduce((sum, item) => sum + (item.price || 0) * (item.quantity || 1), 0)
  const regId = currentPatient.value?.regId || ''
  const APP_BASE_URL = import.meta.env.VITE_APP_URL || window.location.origin
  const qrUrl = `${APP_BASE_URL}/patient/payment?regId=${regId}`
  const qrSrc = await QRCode.toDataURL(qrUrl, { width: 100, margin: 0 })
  
  const drugListHtml = items.map((item, idx) => `
    <div class="drug-item">
      <span class="drug-index">${idx + 1}.</span>
      <span class="drug-name">${item.drugName}</span>
      <span class="drug-spec">${item.spec}</span>
      <span class="drug-quantity">× ${item.quantity}${item.unit}</span>
      <div class="drug-usage">用法：${item.usageInstruction || '遵医嘱'}</div>
    </div>
  `).join('')
  
  return `<!DOCTYPE html>
<html><head><meta charset="utf-8"><title>处方笺</title>
<style>
  @page { margin: 10mm; size: A5; }
  @media print { * { -webkit-print-color-adjust: exact; print-color-adjust: exact; } }
  body { font-family: 'SimSun', '宋体', serif; font-size: 11pt; margin: 0; padding: 10mm; color: #000; }
  .paper { background: #fff; padding: 15px; border: 2px solid #000; position: relative; }
  .header { text-align: center; margin-bottom: 10px; position: relative; }
  .dept-label { position: absolute; right: 10px; top: 0; font-size: 12pt; }
  .hospital-name { font-size: 16pt; margin: 0 0 5px 0; font-weight: bold; }
  .doc-title { font-size: 14pt; margin: 0; }
  .fee-row { margin: 5px 0; font-size: 10pt; }
  .fee-row label { margin-right: 10px; }
  .info-row { margin: 5px 0; }
  .field-item { margin-right: 15px; }
  .field-item u { text-decoration: none; border-bottom: 1px solid #000; padding: 0 8px; }
  .rx-section { margin: 10px 0; min-height: 150px; border: 1px solid #000; padding: 10px; background: #fff; }
  .rx-header { font-size: 20pt; font-family: 'Times New Roman', serif; margin-bottom: 8px; font-weight: bold; }
  .drug-item { margin: 8px 0; }
  .drug-index { margin-right: 5px; }
  .drug-name { font-weight: bold; margin-right: 8px; }
  .drug-spec { margin-right: 8px; }
  .drug-quantity { margin-left: 15px; }
  .drug-usage { margin-left: 20px; font-size: 10pt; }
  .signature { margin-top: 10px; padding-top: 10px; border-top: 1px solid #000; }
  .sig-row { margin: 8px 0; display: flex; justify-content: space-between; }
  .qr-code { position: absolute; top: 10px; right: 10px; text-align: center; z-index: 10; }
  .qr-code img { width: 60px; height: 60px; }
  .qr-code div { font-size: 9px; margin-top: 2px; background: #fff; }
</style></head><body>
<div class="paper">
  <div class="qr-code">
    <img src="${qrSrc}" />
    <div>扫码缴费</div>
  </div>
  <div class="header">
    <div class="dept-label">${userStore.userInfo?.deptName || ''}</div>
    <div class="hospital-name">武汉大学医院</div>
    <div class="doc-title">处 方 笺</div>
  </div>
  <div class="fee-row">
    <span>费别：</span>
    <label>☐ 公费</label>
    <label>☑ 自费</label>
    <label>☐ 农合</label>
    <label>☐ 医保</label>
    <label>☐ 其他</label>
    <span class="field-item" style="float:right">处方编号：<u>${currentPatient.value?.regId || ''}</u></span>
  </div>
  <div class="info-row">
    <span class="field-item">姓名：<u>${currentPatient.value?.patientName || ''}</u></span>
    <span class="field-item">性别：${currentPatient.value?.gender === 1 ? '男' : '女'}</span>
    <span class="field-item">年龄：<u>${currentPatient.value?.age || ''}</u>岁</span>
    <span class="field-item">日期：<u>${dateStr}</u></span>
  </div>
  <div class="info-row">
    <span class="field-item">临床诊断：<u>${emrForm.diagnosis || emrTemplateData.value?.diagnosis || '____'}</u></span>
  </div>
  <div class="rx-section">
    <div class="rx-header">Rp</div>
    ${drugListHtml}
  </div>
  <div class="signature">
    <div class="sig-row">
      <span>医师：<u>${userStore.userInfo?.realName || ''}</u></span>
      <span>药品金额：<u>¥${total.toFixed(2)}</u></span>
    </div>
    <div class="sig-row">
      <span>审核药师：________</span>
      <span>调配药师：________</span>
      <span>发药药师：________</span>
    </div>
  </div>
</div>
</body></html>`
}

// 生成检查申请单打印HTML - 黑白打印格式
const generateLabHtml = async (items) => {
  const now = new Date()
  const dateStr = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`
  const total = items.reduce((sum, item) => sum + (item.price || 0), 0)
  const regId = currentPatient.value?.regId || ''
  const APP_BASE_URL = import.meta.env.VITE_APP_URL || window.location.origin
  const qrUrl = `${APP_BASE_URL}/patient/payment?regId=${regId}`
  const qrSrc = await QRCode.toDataURL(qrUrl, { width: 100, margin: 0 })
  
  const itemsHtml = items.map((item, idx) => `
    <tr>
      <td>${idx + 1}</td>
      <td>${item.itemName}</td>
      <td>¥${item.price || 0}</td>
      <td>${getLabStatusText(item.status)}</td>
    </tr>
  `).join('')
  
  return `<!DOCTYPE html>
<html><head><meta charset="utf-8"><title>检查申请单</title>
<style>
  @page { margin: 15mm; size: A5; }
  @media print { * { -webkit-print-color-adjust: exact; print-color-adjust: exact; } }
  body { font-family: 'SimSun', '宋体', serif; font-size: 11pt; color: #000; }
  .paper { max-width: 148mm; margin: 0 auto; border: 1px solid #000; padding: 15px; position: relative; }
  .header { text-align: center; margin-bottom: 15px; }
  .header h1 { font-size: 16pt; margin: 0 0 8px 0; font-weight: bold; }
  .header h2 { font-size: 14pt; margin: 0; }
  .info-table { width: 100%; border-collapse: collapse; margin: 10px 0; }
  .info-table td { padding: 5px; border: 1px solid #000; }
  .items-table { width: 100%; border-collapse: collapse; margin: 10px 0; }
  .items-table th, .items-table td { padding: 6px; border: 1px solid #000; text-align: center; }
  .items-table th { background: #eee; }
  .signature { margin-top: 15px; }
  .qr-code { position: absolute; top: 10px; right: 10px; text-align: center; z-index: 10; }
  .qr-code img { width: 60px; height: 60px; }
  .qr-code div { font-size: 9px; margin-top: 2px; background: #fff; }
</style></head><body>
<div class="paper">
  <div class="qr-code">
    <img src="${qrSrc}" />
    <div>扫码缴费</div>
  </div>
  <div class="header">
    <h1>武汉大学医院</h1>
    <h2>检查/检验申请单</h2>
  </div>
  <table class="info-table">
    <tr>
      <td>姓名：${currentPatient.value?.patientName || ''}</td>
      <td>性别：${currentPatient.value?.gender === 1 ? '男' : '女'}</td>
      <td>年龄：${currentPatient.value?.age || ''}岁</td>
      <td>门诊号：${currentPatient.value?.regId || ''}</td>
    </tr>
    <tr>
      <td colspan="2">科室：${userStore.userInfo?.deptName || ''}</td>
      <td colspan="2">申请日期：${dateStr}</td>
    </tr>
    <tr>
      <td colspan="4">临床诊断：${emrForm.diagnosis || emrTemplateData.value?.diagnosis || '-'}</td>
    </tr>
  </table>
  <h3 style="margin: 10px 0 5px 0; font-size: 12pt;">检查项目</h3>
  <table class="items-table">
    <thead>
      <tr>
        <th width="50">序号</th>
        <th>项目名称</th>
        <th width="80">单价</th>
        <th width="80">状态</th>
      </tr>
    </thead>
    <tbody>${itemsHtml}</tbody>
    <tfoot>
      <tr>
        <td colspan="2" style="text-align: right">合计：</td>
        <td colspan="2">¥${total.toFixed(2)}</td>
      </tr>
    </tfoot>
  </table>
  <div class="signature">
    <span>开单医生：${userStore.userInfo?.realName || ''}</span>
    <span style="margin-left: 30px">日期：${dateStr}</span>
  </div>
</div>
</body></html>`
}

const completeVisit = async () => {
  if (!currentPatient.value) return
  try {
    await doctorApi.complete(currentPatient.value.regId)
    message.success('就诊完成')
    currentPatient.value = null
    await loadPatients()
  } catch (e) {
    message.error('操作失败')
  }
}
</script>

<style scoped lang="scss">
.workstation {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #f0f2f5;
  padding: 12px;
  box-sizing: border-box;
  
  .workstation-content {
    flex: 1;
    display: flex;
    gap: 12px;
    overflow: hidden;
  }

  .left-panel {
    width: 240px;
    flex-shrink: 0;
  }

  .middle-panel {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 12px;
    overflow: hidden;
  }

  .right-panel {
    width: 320px;
    flex-shrink: 0;
  }

  .panel-card {
    background: #fff;
    border-radius: 6px;
    display: flex;
    flex-direction: column;
    overflow: hidden;
    box-shadow: 0 1px 3px rgba(0,0,0,0.06);
    height: 100%;

    .panel-header {
      padding: 12px 16px;
      background: #fafafa;
      border-bottom: 1px solid #f0f0f0;
      font-weight: 600;
      display: flex;
      justify-content: space-between;
      align-items: center;
      flex-shrink: 0;
      
      .header-title {
        display: flex;
        align-items: center;
        gap: 8px;
      }
    }
  }

  // Patient List
  .patient-list-panel {
    .patient-filter {
      padding: 8px 12px;
      border-bottom: 1px solid #f0f0f0;
    }
    
    .patient-list {
      flex: 1;
      overflow-y: auto;
      padding: 8px;
    }
    
    .patient-empty {
      text-align: center;
      color: #999;
      padding: 20px;
    }
    
    .patient-item {
      display: flex;
      align-items: center;
      padding: 10px;
      border-radius: 6px;
      cursor: pointer;
      margin-bottom: 6px;
      border: 1px solid #f0f0f0;
      transition: all 0.2s;
      
      &:hover {
        background: #f5f7fa;
        border-color: #e0e0e0;
      }
      
      &.current {
        background: #e6f7ff;
        border-color: #1890ff;
      }
      
      &.in-consultation {
        border-left: 3px solid #1890ff;
      }
      
      &.waiting {
        border-left: 3px solid #faad14;
      }
      
      &.completed {
        opacity: 0.7;
        border-left: 3px solid #52c41a;
      }
      
      .patient-number {
        width: 28px;
        height: 28px;
        background: #f0f0f0;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        font-weight: 600;
        font-size: 12px;
        margin-right: 10px;
        flex-shrink: 0;
      }
      
      .patient-info {
        flex: 1;
        min-width: 0;
        
        .name {
          font-weight: 500;
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
        }
        
        .meta {
          font-size: 12px;
          color: #999;
          
          span + span::before {
            content: ' · ';
          }
        }
      }
      
      .patient-status {
        display: flex;
        flex-direction: column;
        gap: 2px;
        align-items: flex-end;
      }
    }
  }
  
  // Action Header
  .action-header {
    background: #fff;
    padding: 12px 20px;
    border-radius: 6px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    box-shadow: 0 1px 3px rgba(0,0,0,0.06);
    flex-shrink: 0;
    
    .patient-basic {
      display: flex;
      align-items: center;
      gap: 12px;
      
      .name {
        font-size: 18px;
        font-weight: 600;
      }
      
      .meta {
        color: #666;
      }
    }
    
    .actions {
      display: flex;
      gap: 8px;
    }
  }
  
  // Content Card
  .content-card {
    flex: 1;
    overflow: hidden;
    
    .workspace-tabs {
      height: 100%;
      display: flex;
      flex-direction: column;
      
      :deep(.ant-tabs-nav) {
        margin-bottom: 0;
        background: #fafafa;
        padding: 8px 8px 0 8px;
        flex-shrink: 0;
      }
      
      :deep(.ant-tabs-content-holder) {
        flex: 1;
        overflow: hidden;
        display: flex;
        flex-direction: column;
      }
      
      :deep(.ant-tabs-content) {
        flex: 1;
        height: 100%;
        display: flex;
        flex-direction: column;
        
        .ant-tabs-tabpane {
          flex: 1;
          height: 100%;
          display: flex;
          flex-direction: column;
          overflow: hidden;
        }
        
        .ant-tabs-tabpane-active {
          display: flex !important;
        }
      }
      
      .tab-extra-buttons {
        display: flex;
        gap: 8px;
        padding-right: 8px;
      }
    }
    
    .tab-content {
      padding: 16px;
      flex: 1;
      display: flex;
      flex-direction: column;
      overflow: hidden;
      min-height: 0;
      
      .emr-form {
        max-width: 800px;
        margin: 0 auto;
        width: 100%;
      }
      
      // 电子病历 tab 特殊样式
      &.emr-tab-content {
        .emr-editor-wrapper {
          flex: 1;
          overflow-y: auto;
          min-height: 0;
          background: #f5f5f5;
          border-radius: 4px;
        }
        
        .form-actions {
          flex-shrink: 0;
          margin-top: 12px;
        }
      }
      
      .section-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 16px;
        font-weight: 600;
        font-size: 15px;
        flex-shrink: 0;
        
        .header-actions {
          display: flex;
          gap: 8px;
        }
      }
      
      .lab-list, .prescription-list {
        flex: 1;
        overflow-y: auto;
        border: 1px solid #f0f0f0;
        border-radius: 4px;
        padding: 12px;
        background: #fafafa;
        min-height: 0;
      }
      
      .lab-item {
        background: #fff;
        padding: 12px;
        border-radius: 4px;
        margin-bottom: 8px;
        border: 1px solid #e8e8e8;
        
        .lab-row {
          display: flex;
          justify-content: space-between;
          align-items: center;
          
          .lab-info {
            display: flex;
            align-items: center;
            gap: 12px;
            
            .price {
              color: #ff4d4f;
              font-weight: 500;
            }
          }
          
          .lab-status {
            display: flex;
            align-items: center;
            gap: 8px;
          }
        }
      }
      
      .form-actions {
        margin-top: 20px;
        display: flex;
        justify-content: center;
        padding-top: 16px;
        border-top: 1px solid #f0f0f0;
      }
    }
  }
  
  .history-content {
    padding: 16px;
    
    .empty-tip {
      color: #999;
      text-align: center;
      padding: 20px;
    }
    
    .history-header {
      display: flex;
      gap: 16px;
      
      .date {
        color: #1890ff;
        font-weight: 500;
      }
      
      .diagnosis {
        color: #666;
      }
    }
    
    .history-detail {
      .detail-row {
        margin-bottom: 8px;
        
        label {
          color: #999;
          margin-right: 8px;
        }
        
        .sub-list {
          margin-top: 4px;
          padding-left: 16px;
          color: #666;
          font-size: 13px;

          .history-lab-item {
            display: flex;
            justify-content: space-between;
            align-items: center;
            padding: 4px 0;
            border-bottom: 1px dashed #eee;
            
            &:last-child {
              border-bottom: none;
            }

            .lab-name {
              font-weight: 500;
            }

            .lab-actions {
              display: flex;
              align-items: center;
              gap: 8px;
            }

            .no-result {
              color: #999;
              font-size: 12px;
            }
          }
        }
      }
    }
  }
  
  // Template Panel
  .template-panel {
    .template-list {
      flex: 1;
      overflow-y: auto;
      padding: 8px;
      
      .empty-tip {
        color: #999;
        text-align: center;
        padding: 20px;
        font-size: 13px;
      }
    }
    
    .template-item {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 10px;
      border-radius: 4px;
      cursor: pointer;
      border: 1px solid #f0f0f0;
      margin-bottom: 6px;
      
      &:hover {
        background: #f5f7fa;
        border-color: #1890ff;
      }
      
      .tpl-name {
        font-size: 13px;
      }
    }
  }

  .right-tools-panel {
    height: 100%;
    padding: 8px;
    
    .right-tabs {
      height: 100%;
      display: flex;
      flex-direction: column;
      
      :deep(.ant-tabs-content) {
        flex: 1;
        height: 100%;
      }
      
      :deep(.ant-tabs-tabpane) {
        height: 100%;
        overflow: hidden;
      }
    }
  }
  
  .no-patient {
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
    background: #fff;
    border-radius: 6px;
  }
}

// Context Menu
.context-menu {
  position: fixed;
  background: white;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.15);
  z-index: 9999;
  min-width: 150px;
  
  .context-menu-item {
    padding: 8px 16px;
    cursor: pointer;
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 14px;
    color: #262626;
    
    &:hover {
      background: #f5f5f5;
    }
  }
}
</style>
