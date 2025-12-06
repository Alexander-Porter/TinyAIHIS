<template>
  <div class="emr-editor">


    <!-- Editor Content - 模拟电子病历表单 -->
    <div class="editor-content" ref="editorRef">
      <div class="emr-paper">
        <!-- Header -->
        <div class="paper-header">
          <h1 class="hospital-name" contenteditable="false">{{ hospitalName }}</h1>
          <h2 class="doc-title">门 诊 病 历</h2>
        </div>

        <div class="dept-line">
          <span>科室：</span>
          <editable-field 
            v-model="formData.deptName" 
            :readonly="readonly"
            placeholder="科室名称"
          />
          <span style="margin-left: 50px">门诊号：</span>
          <editable-field 
            v-model="formData.recordNo" 
            :readonly="readonly"
            placeholder="门诊号"
          />
        </div>

        <hr />

        <!-- Patient Info Section -->
        <div class="patient-section">
          <div class="info-line">
            <span class="label">姓名：</span>
            <editable-field 
              v-model="formData.patientName" 
              :readonly="readonly"
              placeholder="患者姓名"
              class="name-field"
            />
            <span class="label">性别：</span>
            <span class="gender-options" v-if="!readonly">
              <label><input type="radio" v-model="formData.gender" :value="1" />男</label>
              <label><input type="radio" v-model="formData.gender" :value="0" />女</label>
            </span>
            <span v-else>{{ formData.gender === 1 ? '男' : '女' }}</span>
            <span class="label">年龄：</span>
            <editable-field 
              v-model="formData.age" 
              :readonly="readonly"
              placeholder="年龄"
              type="number"
              class="age-field"
            />
            <span>岁</span>
          </div>

          <div class="info-line">
            <span class="label">民族：</span>
            <editable-field 
              v-model="formData.ethnicity" 
              :readonly="readonly"
              placeholder="民族"
              class="short-field"
            />
            <span class="label">婚姻：</span>
            <editable-field 
              v-model="formData.maritalStatus" 
              :readonly="readonly"
              placeholder="婚姻状况"
              class="short-field"
            />
            <span class="label">职业：</span>
            <editable-field 
              v-model="formData.occupation" 
              :readonly="readonly"
              placeholder="职业"
              class="short-field"
            />
            <span class="label">住院号：</span>
            <editable-field 
              v-model="formData.inpatientNo" 
              :readonly="readonly"
              placeholder="住院号"
              class="short-field"
            />
          </div>

          <div class="info-line">
            <span class="label">住址：</span>
            <editable-field 
              v-model="formData.address" 
              :readonly="readonly"
              placeholder="住址"
              class="long-field"
            />
          </div>

          <div class="info-line">
            <span class="label">就诊时间：</span>
            <editable-field 
              v-model="formData.visitTime" 
              :readonly="readonly"
              placeholder="就诊时间"
              class="time-field"
            />
          </div>
        </div>

        <hr />

        <!-- Main Content Section -->
        <div class="content-section">
          <div class="content-block">
            <div class="block-title">主 诉</div>
            <editable-area 
              v-model="formData.symptom" 
              :readonly="readonly"
              placeholder="请输入主诉内容..."
              :rows="1"
            />
          </div>

          <div class="content-block">
            <div class="block-title">现病史</div>
            <editable-area 
              v-model="formData.presentHistory" 
              :readonly="readonly"
              placeholder="请输入现病史..."
              :rows="2"
            />
          </div>

          <div class="content-block">
            <div class="block-title">既往史</div>
            <editable-area 
              v-model="formData.pastHistory" 
              :readonly="readonly"
              placeholder="请输入既往史..."
              :rows="1"
            />
          </div>

          <div class="content-block">
            <div class="block-title">查体</div>
            <editable-area 
              v-model="formData.physicalExam" 
              :readonly="readonly"
              placeholder="请输入查体结果..."
              :rows="1"
            />
          </div>

          <div class="content-block">
            <div class="block-title">诊 断</div>
            <editable-area 
              v-model="formData.diagnosis" 
              :readonly="readonly"
              placeholder="请输入诊断..."
              :rows="1"
            />
          </div>

          <div class="content-block">
            <div class="block-title">处 理</div>
            <editable-area 
              v-model="formData.treatment" 
              :readonly="readonly"
              placeholder="请输入处理意见..."
              :rows="1"
            />
          </div>

          <!-- 检查检验信息（只读显示） -->
          <div class="content-block" v-if="labOrders && labOrders.length > 0">
            <div class="block-title">检查检验</div>
            <div class="readonly-list">
              <div class="list-item" v-for="(lab, idx) in labOrders" :key="idx">
                <span class="item-index">{{ idx + 1 }}.</span>
                <span class="item-name">{{ lab.itemName }}</span>
                <span class="item-price">¥{{ lab.price || 0 }}</span>
              </div>
            </div>
          </div>

          <!-- 处方信息（只读显示） -->
          <div class="content-block" v-if="prescriptions && prescriptions.length > 0">
            <div class="block-title">处 方</div>
            <div class="readonly-list">
              <div class="list-item" v-for="(pres, idx) in prescriptions" :key="idx">
                <span class="item-index">{{ idx + 1 }}.</span>
                <span class="item-name">{{ getDrugName(pres.drugId) }}</span>
                <span class="item-quantity">× {{ pres.quantity }}</span>
                <span class="item-usage">{{ pres.usageInstruction || '遵医嘱' }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- Signature Section -->
        <div class="signature-section">
          <span class="sig-item">医师签名：</span>
          <div style="display: inline-block" @dblclick.capture.stop="autoSign">
            <editable-field 
              v-model="formData.doctorName" 
              :readonly="readonly"
              placeholder="医师姓名(双击自动签名)"
            />
          </div>
          <span class="sig-item" style="margin-left: 50px">日期：</span>
          <editable-field 
            v-model="formData.signDate" 
            :readonly="readonly"
            placeholder="签名日期"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, watch, onMounted, computed } from 'vue'
import { PrinterOutlined } from '@ant-design/icons-vue'
import { useUserStore } from '@/stores/user'
import EditableField from './EditableField.vue'
import EditableArea from './EditableArea.vue'

const userStore = useUserStore()

const props = defineProps({
  modelValue: { type: Object, default: () => ({}) },
  readonly: { type: Boolean, default: false },
  hospitalName: { type: String, default: '武汉大学医院' },
  patientInfo: { type: Object, default: () => ({}) },
  labOrders: { type: Array, default: () => [] },
  prescriptions: { type: Array, default: () => [] },
  drugs: { type: Array, default: () => [] }
})

const emit = defineEmits(['update:modelValue', 'print'])

const editorRef = ref(null)

// 检查状态文字
const getLabStatusText = (status) => {
  const texts = { 0: '待缴费', 1: '待检查', 2: '已完成' }
  return texts[status] || '未知'
}

const getLabStatusClass = (status) => {
  const classes = { 0: 'status-pending', 1: 'status-processing', 2: 'status-done' }
  return classes[status] || ''
}

// 根据drugId获取药品名称
const getDrugName = (drugId) => {
  const drug = props.drugs.find(d => d.drugId === drugId)
  return drug?.name || '未知药品'
}

const formData = reactive({
  patientName: '',
  gender: 1,
  age: '',
  ethnicity: '',
  maritalStatus: '',
  occupation: '',
  inpatientNo: '',
  address: '',
  visitTime: '',
  deptName: '',
  recordNo: '',
  symptom: '',
  presentHistory: '',
  pastHistory: '',
  physicalExam: '',
  diagnosis: '',
  treatment: '',
  doctorName: '',
  signDate: ''
})

// 自动填充患者信息
watch(() => props.patientInfo, (info) => {
  if (info) {
    formData.patientName = info.patientName || formData.patientName
    formData.gender = info.gender ?? formData.gender
    formData.age = info.age || formData.age
    formData.deptName = info.deptName || formData.deptName
    formData.recordNo = info.recordNo || info.regId || formData.recordNo
    formData.visitTime = info.visitTime || new Date().toLocaleString('zh-CN')
  }
}, { immediate: true, deep: true })

// 同步外部数据
watch(() => props.modelValue, (val) => {
  if (val) {
    Object.assign(formData, val)
  }
}, { immediate: true, deep: true })

// 发送更新
watch(formData, (val) => {
  emit('update:modelValue', { ...val })
}, { deep: true })

onMounted(() => {
  if (!formData.visitTime) {
    formData.visitTime = new Date().toLocaleString('zh-CN')
  }
  if (!formData.signDate) {
    formData.signDate = new Date().toLocaleDateString('zh-CN')
  }
})

const print = () => {
  emit('print', { ...formData })
}

const autoSign = () => {
  if (props.readonly) return
  if (userStore.userInfo && userStore.userInfo.realName) {
    formData.doctorName = userStore.userInfo.realName
    formData.signDate = new Date().toLocaleDateString('zh-CN')
  }
}

const getData = () => ({ ...formData })

defineExpose({ getData, print })
</script>

<style scoped lang="scss">
.emr-editor {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #f5f5f5;

  .editor-toolbar {
    padding: 8px 12px;
    background: #fff;
    border-bottom: 1px solid #e8e8e8;
    display: flex;
    align-items: center;
    gap: 8px;

    .toolbar-hint {
      color: #999;
      font-size: 12px;
    }
  }

  .editor-content {
    flex: 1;
    overflow: auto;
    padding: 20px;
    display: flex;
    justify-content: center;
  }

  .emr-paper {
    width: 210mm;
    min-height: 297mm;
    background: #fff;
    padding: 15mm 20mm;
    box-shadow: 0 2px 12px rgba(0,0,0,0.1);
    font-family: 'SimSun', '宋体', serif;
    font-size: 11pt;
    line-height: 1.5;

    .paper-header {
      text-align: center;
      margin-bottom: 8px;

      .hospital-name {
        font-size: 18pt;
        margin: 0 0 5px 0;
      }

      .doc-title {
        font-size: 14pt;
        margin: 0;
        letter-spacing: 3px;
      }
    }

    .dept-line {
      display: flex;
      align-items: center;
      margin: 10px 0;
    }

    hr {
      border: none;
      border-top: 1px solid #333;
      margin: 10px 0;
    }

    .patient-section {
      margin: 8px 0;

      .info-line {
        display: flex;
        align-items: center;
        margin: 4px 0;
        flex-wrap: wrap;

        .label {
          white-space: nowrap;
          margin-left: 15px;

          &:first-child {
            margin-left: 0;
          }
        }

        .gender-options {
          label {
            margin-right: 10px;
            
            input {
              margin-right: 3px;
            }
          }
        }

        .name-field { width: 100px; }
        .age-field { width: 50px; }
        .short-field { width: 80px; }
        .long-field { flex: 1; min-width: 300px; }
        .time-field { width: 180px; }
      }
    }

    .content-section {
      margin: 10px 0;

      .content-block {
        margin: 8px 0;

        .block-title {
          font-weight: bold;
          margin-bottom: 2px;
          color: #000;
        }

        // 检查检验和处方的只读列表样式 - 无边框
        .readonly-list {
          padding: 5px 0;

          .list-item {
            display: flex;
            align-items: center;
            padding: 4px 0;
            font-size: 11pt;

            .item-index {
              width: 24px;
              color: #666;
            }

            .item-name {
              flex: 1;
              font-weight: 500;
            }

            .item-price {
              color: #333;
              margin: 0 15px;
            }

            .item-quantity {
              color: #333;
              margin: 0 10px;
            }

            .item-usage {
              color: #666;
              font-size: 10pt;
            }

            .item-status {
              padding: 2px 6px;
              font-size: 10pt;
              color: #666;
            }
          }
        }
      }
    }

    .signature-section {
      margin-top: 30px;
      padding-top: 15px;
      border-top: 1px solid #ccc;
      display: flex;
      align-items: center;

      .sig-item {
        white-space: nowrap;
      }
    }
  }
}
</style>
