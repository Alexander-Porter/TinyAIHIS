<template>
  <div class="workstation">
    <div class="workstation-header">
      <h2>接诊工作台</h2>
      <div class="actions">
        <el-button type="primary" @click="callNext" :loading="calling">
          <el-icon><Bell /></el-icon> 叫号
        </el-button>
      </div>
    </div>
    
    <div class="workstation-content">
      <!-- Left: Queue -->
      <div class="queue-panel">
        <div class="panel-header">
          <span>候诊队列</span>
          <el-tag size="small">{{ queue.length }} 人</el-tag>
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
              <div class="name">患者 #{{ item.patientId }}</div>
              <div class="status">{{ getStatusText(item.status) }}</div>
            </div>
          </div>
        </div>
      </div>
      
      <!-- Right: EMR -->
      <div class="emr-panel">
        <template v-if="currentPatient">
          <el-tabs v-model="activeTab">
            <el-tab-pane label="病历录入" name="emr">
              <div class="emr-form">
                <el-form :model="emrForm" label-width="80px">
                  <el-form-item label="主诉">
                    <el-input v-model="emrForm.symptom" type="textarea" rows="2" placeholder="患者主诉" />
                  </el-form-item>
                  <el-form-item label="诊断">
                    <el-input v-model="emrForm.diagnosis" placeholder="诊断结果" />
                  </el-form-item>
                  <el-form-item label="病历详情">
                    <el-input v-model="emrForm.content" type="textarea" rows="4" placeholder="现病史、查体等" />
                  </el-form-item>
                </el-form>
              </div>
            </el-tab-pane>
            
            <el-tab-pane label="开具处方" name="prescription">
              <div class="prescription-form">
                <div class="drug-list">
                  <div class="drug-item" v-for="(drug, idx) in prescriptions" :key="idx">
                    <el-select v-model="drug.drugId" placeholder="选择药品" filterable style="width: 200px">
                      <el-option v-for="d in drugs" :key="d.drugId" :label="d.name" :value="d.drugId" />
                    </el-select>
                    <el-input-number v-model="drug.quantity" :min="1" placeholder="数量" />
                    <el-input v-model="drug.usageInstruction" placeholder="用法用量" style="width: 200px" />
                    <el-button type="danger" text @click="prescriptions.splice(idx, 1)">删除</el-button>
                  </div>
                </div>
                <el-button type="primary" plain @click="addPrescription">+ 添加药品</el-button>
              </div>
            </el-tab-pane>
            
            <el-tab-pane label="开具检查" name="lab">
              <div class="lab-form">
                <div class="lab-list">
                  <div class="lab-item" v-for="(item, idx) in labOrders" :key="idx">
                    <el-input v-model="item.itemName" placeholder="检查项目" style="width: 200px" />
                    <el-input-number v-model="item.price" :min="0" placeholder="价格" />
                    <el-button type="danger" text @click="labOrders.splice(idx, 1)">删除</el-button>
                  </div>
                </div>
                <el-button type="primary" plain @click="addLabOrder">+ 添加检查</el-button>
              </div>
            </el-tab-pane>
            
            <el-tab-pane label="模板" name="templates">
              <div class="template-list">
                <div class="template-item" v-for="tpl in templates" :key="tpl.tplId" @click="applyTemplate(tpl)">
                  <div class="name">{{ tpl.name }}</div>
                  <div class="type">{{ tpl.type === 'EMR' ? '病历模板' : '处方套餐' }}</div>
                </div>
              </div>
            </el-tab-pane>
          </el-tabs>
          
          <div class="emr-actions">
            <el-button type="primary" @click="saveEmr" :loading="saving">保存病历</el-button>
            <el-button type="success" @click="completeVisit">完成就诊</el-button>
          </div>
        </template>
        
        <div class="no-patient" v-else>
          <el-empty description="请点击叫号或选择患者" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Bell } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { doctorApi, emrApi, pharmacyApi } from '@/utils/api'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const doctorId = userStore.userId

const queue = ref([])
const currentPatient = ref(null)
const activeTab = ref('emr')
const calling = ref(false)
const saving = ref(false)
const drugs = ref([])
const templates = ref([])

const emrForm = reactive({
  symptom: '',
  diagnosis: '',
  content: ''
})

const prescriptions = ref([])
const labOrders = ref([])

onMounted(async () => {
  loadQueue()
  loadDrugs()
  loadTemplates()
})

const loadQueue = async () => {
  try {
    queue.value = await doctorApi.getQueue(doctorId)
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

const callNext = async () => {
  calling.value = true
  try {
    const patient = await doctorApi.callNext(doctorId)
    currentPatient.value = patient
    loadQueue()
    ElMessage.success(`已叫号：${patient.queueNumber}号`)
  } catch (e) {
    console.error('Call next failed', e)
  } finally {
    calling.value = false
  }
}

const selectPatient = (patient) => {
  currentPatient.value = patient
}

const getStatusText = (status) => {
  const map = { 2: '候诊', 3: '就诊中' }
  return map[status] || ''
}

const addPrescription = () => {
  prescriptions.value.push({ drugId: null, quantity: 1, usageInstruction: '' })
}

const addLabOrder = () => {
  labOrders.value.push({ itemName: '', price: 100 })
}

const applyTemplate = (tpl) => {
  if (tpl.type === 'EMR') {
    emrForm.content = tpl.content
  }
  ElMessage.success('已应用模板')
}

const saveEmr = async () => {
  if (!currentPatient.value) return
  
  saving.value = true
  try {
    await emrApi.save({
      regId: currentPatient.value.regId,
      symptom: emrForm.symptom,
      diagnosis: emrForm.diagnosis,
      content: emrForm.content,
      prescriptions: prescriptions.value.filter(p => p.drugId),
      labOrders: labOrders.value.filter(l => l.itemName)
    })
    ElMessage.success('病历已保存')
  } catch (e) {
    console.error('Save EMR failed', e)
  } finally {
    saving.value = false
  }
}

const completeVisit = async () => {
  if (!currentPatient.value) return
  
  try {
    await doctorApi.complete(currentPatient.value.regId)
    ElMessage.success('就诊已完成')
    currentPatient.value = null
    emrForm.symptom = ''
    emrForm.diagnosis = ''
    emrForm.content = ''
    prescriptions.value = []
    labOrders.value = []
    loadQueue()
  } catch (e) {
    console.error('Complete visit failed', e)
  }
}
</script>

<style scoped lang="scss">
.workstation {
  padding: 20px;
  
  .workstation-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    
    h2 {
      margin: 0;
    }
  }
  
  .workstation-content {
    display: flex;
    gap: 20px;
    height: calc(100vh - 140px);
  }
  
  .queue-panel {
    width: 280px;
    background: #fff;
    border-radius: 8px;
    display: flex;
    flex-direction: column;
    
    .panel-header {
      padding: 15px;
      border-bottom: 1px solid #eee;
      display: flex;
      justify-content: space-between;
      align-items: center;
      font-weight: 500;
    }
    
    .queue-list {
      flex: 1;
      overflow: auto;
      padding: 10px;
      
      .queue-empty {
        text-align: center;
        color: #999;
        padding: 40px 0;
      }
      
      .queue-item {
        display: flex;
        align-items: center;
        padding: 12px;
        border-radius: 8px;
        cursor: pointer;
        margin-bottom: 8px;
        background: #f5f7fa;
        
        &:hover {
          background: #e8f4ff;
        }
        
        &.current {
          background: #409eff;
          color: #fff;
        }
        
        .number {
          width: 40px;
          height: 40px;
          background: rgba(0,0,0,0.1);
          border-radius: 50%;
          display: flex;
          align-items: center;
          justify-content: center;
          font-weight: bold;
          margin-right: 12px;
        }
        
        .info {
          .name {
            font-weight: 500;
          }
          .status {
            font-size: 12px;
            opacity: 0.8;
          }
        }
      }
    }
  }
  
  .emr-panel {
    flex: 1;
    background: #fff;
    border-radius: 8px;
    padding: 20px;
    display: flex;
    flex-direction: column;
    
    .no-patient {
      flex: 1;
      display: flex;
      align-items: center;
      justify-content: center;
    }
    
    .emr-form, .prescription-form, .lab-form {
      padding: 20px 0;
    }
    
    .drug-list, .lab-list {
      margin-bottom: 15px;
      
      .drug-item, .lab-item {
        display: flex;
        gap: 10px;
        margin-bottom: 10px;
        align-items: center;
      }
    }
    
    .template-list {
      .template-item {
        padding: 15px;
        background: #f5f7fa;
        border-radius: 8px;
        margin-bottom: 10px;
        cursor: pointer;
        
        &:hover {
          background: #e8f4ff;
        }
        
        .name {
          font-weight: 500;
        }
        .type {
          font-size: 12px;
          color: #999;
          margin-top: 5px;
        }
      }
    }
    
    .emr-actions {
      margin-top: 20px;
      padding-top: 20px;
      border-top: 1px solid #eee;
      display: flex;
      gap: 10px;
    }
  }
}
</style>
