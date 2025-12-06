<template>
  <!-- 处方笺打印模板 -->
  <div v-if="type === 'prescription'" class="print-template prescription-template" ref="printRef">
    <div class="prescription-paper">
      <div class="paper-header">
        <div class="dept-label">{{ deptName }}</div>
        <h1 class="hospital-name">{{ hospitalName }}</h1>
        <h2 class="doc-title">处 方 笺</h2>
      </div>
      
      <div class="fee-section">
        <div class="fee-row">
          <span>费别：</span>
          <label><input type="checkbox" :checked="feeType === '公费'" disabled />公费</label>
          <label><input type="checkbox" :checked="feeType === '自费'" disabled />自费</label>
          <label><input type="checkbox" :checked="feeType === '农合'" disabled />农合</label>
        </div>
        <div class="fee-row">
          <label><input type="checkbox" :checked="feeType === '医保'" disabled />医保</label>
          <label><input type="checkbox" :checked="feeType === '其他'" disabled />其他</label>
          <span class="field-item">医疗证号：<u>{{ medicalCardNo || '&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;' }}</u></span>
          <span class="field-item right">处方编号：<u>{{ prescriptionNo }}</u></span>
        </div>
      </div>
      
      <div class="patient-info-section">
        <div class="info-row">
          <span class="field-item">姓名：<u>{{ patientName }}</u></span>
          <span class="field-item">性别：<label><input type="checkbox" :checked="gender === 1" disabled />男</label> <label><input type="checkbox" :checked="gender === 0" disabled />女</label></span>
          <span class="field-item">年龄：<u>{{ age }}</u>岁</span>
          <span class="field-item">日期：<u>{{ prescriptionDate }}</u></span>
        </div>
        <div class="info-row">
          <span class="field-item">体重：<u>____</u>千克</span>
          <span class="field-item">门诊/住院病历号：<u>{{ recordNo || '________' }}</u></span>
          <span class="field-item">科别（病区/床位号）：<u>{{ deptName }}</u></span>
        </div>
        <div class="info-row">
          <span class="field-item">临床诊断：<u>{{ diagnosis || '____________________' }}</u></span>
          <span class="field-item right">开具日期：<u>{{ prescriptionDate }}</u></span>
        </div>
        <div class="info-row">
          <span class="field-item">住址/电话：<u>{{ patientPhone || '____________________' }}</u></span>
        </div>
      </div>
      
      <div class="rx-section">
        <div class="rx-header">Rp</div>
        <div class="rx-content">
          <div class="drug-item" v-for="(drug, idx) in prescriptionItems" :key="idx">
            <span class="drug-index">{{ idx + 1 }}.</span>
            <span class="drug-name">{{ drug.drugName }}</span>
            <span class="drug-spec">{{ drug.spec }}</span>
            <span class="drug-quantity">× {{ drug.quantity }}{{ drug.unit || '盒' }}</span>
            <div class="drug-usage">用法：{{ drug.usageInstruction }}</div>
          </div>
        </div>
      </div>
      
      <div class="signature-section">
        <div class="sig-row">
          <span class="sig-item">医师：<u>{{ doctorName }}</u></span>
          <span class="sig-item">药品金额：<u>¥{{ totalAmount.toFixed(2) }}</u></span>
        </div>
        <div class="sig-row">
          <span class="sig-item">审核药师：<u>________</u></span>
          <span class="sig-item">调配药师：<u>________</u></span>
          <span class="sig-item">核对、发药药师：<u>________</u></span>
        </div>
      </div>
    </div>
  </div>

  <!-- 检查申请单打印模板 -->
  <div v-else-if="type === 'lab'" class="print-template lab-template" ref="printRef">
    <div class="lab-paper">
      <div class="paper-header">
        <h1 class="hospital-name">{{ hospitalName }}</h1>
        <h2 class="doc-title">检查/检验申请单</h2>
      </div>
      
      <div class="patient-info-section">
        <table class="info-table">
          <tr>
            <td>姓名：{{ patientName }}</td>
            <td>性别：{{ gender === 1 ? '男' : '女' }}</td>
            <td>年龄：{{ age }}岁</td>
            <td>门诊号：{{ recordNo }}</td>
          </tr>
          <tr>
            <td colspan="2">科室：{{ deptName }}</td>
            <td colspan="2">申请日期：{{ labDate }}</td>
          </tr>
          <tr>
            <td colspan="4">临床诊断：{{ diagnosis || '-' }}</td>
          </tr>
        </table>
      </div>
      
      <div class="lab-items-section">
        <h3>检查项目</h3>
        <table class="items-table">
          <thead>
            <tr>
              <th width="60">序号</th>
              <th>项目名称</th>
              <th width="100">单价</th>
              <th width="100">状态</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(item, idx) in labItems" :key="idx">
              <td>{{ idx + 1 }}</td>
              <td>{{ item.itemName }}</td>
              <td>¥{{ item.price }}</td>
              <td>{{ getLabStatusText(item.status) }}</td>
            </tr>
          </tbody>
          <tfoot>
            <tr>
              <td colspan="2" style="text-align: right">合计：</td>
              <td colspan="2">¥{{ labTotal.toFixed(2) }}</td>
            </tr>
          </tfoot>
        </table>
      </div>
      
      <div class="signature-section">
        <div class="sig-row">
          <span class="sig-item">开单医生：{{ doctorName }}</span>
          <span class="sig-item">日期：{{ labDate }}</span>
        </div>
      </div>
      
      <div class="barcode-section" v-if="recordNo">
        <div class="barcode-placeholder">[条码区域]</div>
      </div>
    </div>
  </div>

  <!-- 病历打印模板 -->
  <div v-else-if="type === 'emr'" class="print-template emr-template" ref="printRef">
    <div class="emr-paper">
      <div class="paper-header">
        <h1 class="hospital-name">{{ hospitalName }}</h1>
        <h2 class="doc-title">门 诊 病 历</h2>
      </div>
      
      <div class="emr-info-row">
        <span>科室：{{ deptName }}</span>
        <span style="margin-left: 50px">门诊号：{{ recordNo }}</span>
      </div>
      
      <hr />
      
      <div class="patient-info-section">
        <div class="info-row">
          <span>姓名：{{ patientName }}</span>
          <span>性别：{{ gender === 1 ? '男' : '女' }}</span>
          <span>出生日期：{{ birthDate || '____年__月__日' }}</span>
        </div>
        <div class="info-row">
          <span>民族：{{ ethnicity || '____' }}</span>
          <span>婚姻状况：{{ maritalStatus || '____' }}</span>
          <span>职业：{{ occupation || '____' }}</span>
          <span>住院号：{{ inpatientNo || '____' }}</span>
        </div>
        <div class="info-row">
          <span>住址：{{ address || '________________________________' }}</span>
        </div>
        <div class="info-row">
          <span>就诊时间：{{ visitTime }}</span>
        </div>
      </div>
      
      <hr />
      
      <div class="emr-content-section">
        <div class="content-block">
          <div class="block-title">主 诉</div>
          <div class="block-content">{{ symptom || '-' }}</div>
        </div>
        
        <div class="content-block">
          <div class="block-title">现病史</div>
          <div class="block-content">{{ emrContent || '-' }}</div>
        </div>
        
        <div class="content-block">
          <div class="block-title">诊 断</div>
          <div class="block-content diagnosis-content">{{ diagnosis || '-' }}</div>
        </div>
        
        <div class="content-block" v-if="prescriptionItems?.length">
          <div class="block-title">处 方</div>
          <div class="block-content">
            <div v-for="(drug, idx) in prescriptionItems" :key="idx">
              {{ idx + 1 }}. {{ drug.drugName }} {{ drug.spec }} ×{{ drug.quantity }} {{ drug.usageInstruction }}
            </div>
          </div>
        </div>
        
        <div class="content-block" v-if="labItems?.length">
          <div class="block-title">检查项目</div>
          <div class="block-content">
            <div v-for="(item, idx) in labItems" :key="idx">
              {{ idx + 1 }}. {{ item.itemName }} (¥{{ item.price }})
            </div>
          </div>
        </div>
      </div>
      
      <div class="signature-section">
        <div class="sig-row">
          <span class="sig-item">医师签名：{{ doctorName }}</span>
          <span class="sig-item">日期：{{ visitTime }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'

const props = defineProps({
  type: { type: String, default: 'prescription' }, // prescription, lab, emr
  hospitalName: { type: String, default: '武汉大学医院' },
  deptName: { type: String, default: '' },
  patientName: { type: String, default: '' },
  gender: { type: Number, default: 1 },
  age: { type: Number, default: 0 },
  diagnosis: { type: String, default: '' },
  doctorName: { type: String, default: '' },
  recordNo: { type: String, default: '' },
  prescriptionNo: { type: String, default: '' },
  prescriptionItems: { type: Array, default: () => [] },
  labItems: { type: Array, default: () => [] },
  feeType: { type: String, default: '自费' },
  medicalCardNo: { type: String, default: '' },
  patientPhone: { type: String, default: '' },
  symptom: { type: String, default: '' },
  emrContent: { type: String, default: '' },
  birthDate: { type: String, default: '' },
  ethnicity: { type: String, default: '' },
  maritalStatus: { type: String, default: '' },
  occupation: { type: String, default: '' },
  inpatientNo: { type: String, default: '' },
  address: { type: String, default: '' },
  visitTime: { type: String, default: '' }
})

const printRef = ref(null)

const prescriptionDate = computed(() => {
  const now = new Date()
  return `${now.getFullYear()}年${now.getMonth() + 1}月${now.getDate()}日`
})

const labDate = computed(() => {
  const now = new Date()
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`
})

const totalAmount = computed(() => {
  return props.prescriptionItems.reduce((sum, item) => {
    return sum + (item.price || 0) * (item.quantity || 1)
  }, 0)
})

const labTotal = computed(() => {
  return props.labItems.reduce((sum, item) => sum + (item.price || 0), 0)
})

const getLabStatusText = (status) => {
  const texts = { 0: '待缴费', 1: '待检查', 2: '已完成' }
  return texts[status] || '未知'
}

const print = () => {
  const printContent = printRef.value?.innerHTML
  if (!printContent) return
  
  const printWindow = window.open('', '_blank')
  printWindow.document.write(`
    <!DOCTYPE html>
    <html>
    <head>
      <meta charset="utf-8">
      <title>打印</title>
      <style>
        @page { margin: 10mm; size: A5; }
        @media print { body { -webkit-print-color-adjust: exact; } }
        body { font-family: 'SimSun', '宋体', serif; font-size: 12pt; margin: 0; padding: 10mm; }
        
        .prescription-paper { background: #c8f7c5; padding: 15px; border: 2px solid #333; }
        .lab-paper, .emr-paper { background: #fff; padding: 15px; border: 1px solid #333; }
        
        .paper-header { text-align: center; margin-bottom: 10px; }
        .dept-label { position: absolute; right: 20px; top: 10px; font-size: 14pt; }
        .hospital-name { font-size: 18pt; margin: 0 0 5px 0; }
        .doc-title { font-size: 16pt; margin: 0; }
        
        .fee-section { margin: 10px 0; font-size: 10pt; }
        .fee-row { margin: 5px 0; }
        .fee-row label { margin-right: 15px; }
        .fee-row input[type="checkbox"] { margin-right: 3px; }
        
        .patient-info-section { margin: 10px 0; }
        .info-row { margin: 5px 0; }
        .field-item { margin-right: 20px; }
        .field-item.right { float: right; }
        .field-item u { text-decoration: none; border-bottom: 1px solid #000; padding: 0 10px; }
        
        .info-table { width: 100%; border-collapse: collapse; margin: 10px 0; }
        .info-table td { padding: 5px; border: 1px solid #333; }
        
        .rx-section { margin: 15px 0; min-height: 200px; border: 1px solid #333; padding: 10px; background: #fff; }
        .rx-header { font-size: 24pt; font-family: 'Times New Roman', serif; margin-bottom: 10px; }
        .rx-content { min-height: 180px; }
        .drug-item { margin: 10px 0; }
        .drug-index { margin-right: 5px; }
        .drug-name { font-weight: bold; margin-right: 10px; }
        .drug-spec { margin-right: 10px; color: #666; }
        .drug-quantity { margin-left: 20px; }
        .drug-usage { margin-left: 20px; color: #666; font-size: 10pt; }
        
        .lab-items-section { margin: 15px 0; }
        .lab-items-section h3 { margin: 0 0 10px 0; font-size: 12pt; }
        .items-table { width: 100%; border-collapse: collapse; }
        .items-table th, .items-table td { padding: 8px; border: 1px solid #333; text-align: center; }
        .items-table th { background: #f0f0f0; }
        
        .emr-content-section { margin: 15px 0; }
        .content-block { margin: 15px 0; }
        .block-title { font-weight: bold; margin-bottom: 5px; }
        .block-content { padding-left: 20px; line-height: 1.8; white-space: pre-wrap; }
        .diagnosis-content { font-weight: bold; color: #c00; }
        
        .signature-section { margin-top: 20px; padding-top: 10px; border-top: 1px solid #333; }
        .sig-row { margin: 10px 0; display: flex; justify-content: space-between; }
        .sig-item { }
        .sig-item u { text-decoration: none; border-bottom: 1px solid #000; padding: 0 30px; }
        
        .barcode-section { margin-top: 15px; text-align: center; }
        .barcode-placeholder { padding: 10px; border: 1px dashed #999; color: #999; }
        
        hr { border: none; border-top: 1px solid #333; margin: 10px 0; }
      </style>
    </head>
    <body>${printContent}</body>
    </html>
  `)
  printWindow.document.close()
  printWindow.focus()
  setTimeout(() => {
    printWindow.print()
    printWindow.close()
  }, 250)
}

defineExpose({ print })
</script>

<style scoped lang="scss">
.print-template {
  display: none;
}

@media print {
  .print-template {
    display: block;
  }
}
</style>
