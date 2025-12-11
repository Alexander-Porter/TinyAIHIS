<template>
  <div class="lab-workstation">
    <div class="page-header">
      <h2>检验工作台</h2>
      <div class="header-actions">
        <a-radio-group v-model:value="filterStatus" button-style="solid" @change="loadOrders">
          <a-radio-button value="pending">待检验</a-radio-button>
          <a-radio-button value="completed">已完成</a-radio-button>
          <a-radio-button value="all">全部</a-radio-button>
        </a-radio-group>
      </div>
    </div>
    
    <div class="workstation-content">
      <!-- Order List -->
      <div class="order-list-panel">
        <a-table 
          :dataSource="filteredOrders" 
          :columns="columns" 
          rowKey="orderId"
          :pagination="{ pageSize: 15 }"
          size="middle"
          :rowClassName="(record) => record.orderId === currentOrder?.orderId ? 'selected-row' : ''"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'patientInfo'">
              <div class="patient-cell">
                <span class="name">{{ record.patientName || '患者' }}</span>
                <span class="meta">{{ record.gender === 1 ? '男' : '女' }} {{ record.age }}岁</span>
              </div>
            </template>
            <template v-if="column.key === 'status'">
              <a-tag :color="getStatusColor(record.status)">
                {{ getStatusText(record.status) }}
              </a-tag>
            </template>
            <template v-if="column.key === 'action'">
              <a-button type="link" size="small" @click="selectOrder(record)">
                {{ record.status === 1 ? '录入结果' : '查看' }}
              </a-button>
            </template>
          </template>
        </a-table>
      </div>

      <!-- Result Editor Panel -->
      <div class="result-panel" v-if="currentOrder">
        <div class="panel-header">
          <div class="order-info">
            <h3>{{ currentOrder.itemName }}</h3>
            <div class="meta-info">
              <span>患者：{{ currentOrder.patientName || '未知' }}</span>
              <span>单号：#{{ currentOrder.orderId }}</span>
              <a-tag :color="getStatusColor(currentOrder.status)">{{ getStatusText(currentOrder.status) }}</a-tag>
            </div>
          </div>
          <div class="panel-actions" v-if="currentOrder.status === 1">
            <a-button @click="clearResult">清空</a-button>
            <a-button type="primary" @click="submitResult" :loading="submitting">
              提交结果
            </a-button>
          </div>
        </div>

        <div class="editor-container" v-if="currentOrder.status === 1">
          <div class="editor-label">检验结果</div>
          <div class="editor-wrapper">
            <Toolbar
              :editor="editorRef"
              :defaultConfig="toolbarConfig"
              mode="default"
              class="toolbar"
            />
            <Editor
              v-model="resultHtml"
              :defaultConfig="editorConfig"
              mode="default"
              class="editor"
              @onCreated="handleCreated"
            />
          </div>
          
          <div class="template-section">
            <div class="section-title">快捷模板</div>
            <div class="template-buttons">
              <a-button size="small" @click="applyTemplate('normal')">正常模板</a-button>
              <a-button size="small" @click="applyTemplate('blood')">血常规模板</a-button>
              <a-button size="small" @click="applyTemplate('urine')">尿常规模板</a-button>
              <a-button size="small" @click="applyTemplate('liver')">肝功能模板</a-button>
            </div>
          </div>
        </div>

        <!-- View Mode for Completed Results -->
        <div class="result-view" v-else>
          <div class="view-label">检验结果</div>
          <div class="result-content" v-html="currentOrder.resultText || '暂无结果'"></div>
          <div class="image-gallery" v-if="uploadedImages.length">
            <div class="img-item" v-for="img in uploadedImages" :key="img">
              <img :src="img" alt="检验图片" />
            </div>
          </div>
        </div>
      </div>

      <div class="empty-panel" v-else>
        <a-empty description="请从左侧列表选择检验单" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, shallowRef } from 'vue'
import { message } from 'ant-design-vue'
import { labApi } from '@/utils/api'
import '@wangeditor/editor/dist/css/style.css'
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'

// 状态
const orders = ref([])
const currentOrder = ref(null)
const submitting = ref(false)
const filterStatus = ref('pending')
const resultHtml = ref('')
const uploadedImages = computed(() => parseImages(currentOrder.value?.resultImages))

// 编辑器
const editorRef = shallowRef()
const toolbarConfig = {
  excludeKeys: ['uploadVideo', 'insertVideo', 'group-video']
}
const editorConfig = {
  placeholder: '请输入检验结果...',
  MENU_CONF: {}
}

editorConfig.MENU_CONF.uploadImage = {
  async customUpload(file, insertFn) {
    try {
      const url = await labApi.uploadImage(file)
      insertFn(url, file.name)
    } catch (e) {
      message.error('图片上传失败，请重试')
      console.error(e)
    }
  }
}

// 表格列
const columns = [
  { title: '#', dataIndex: 'orderId', key: 'orderId', width: 50 },
  { title: '患者', key: 'patientInfo', width: 90 },
  { title: '项目', dataIndex: 'itemName', key: 'itemName', ellipsis: true },
  { title: '状态', dataIndex: 'status', key: 'status', width: 70 },
  { title: '', key: 'action', width: 70 }
]

// 计算属性
const filteredOrders = computed(() => {
  if (filterStatus.value === 'all') return orders.value
  if (filterStatus.value === 'pending') return orders.value.filter(o => o.status === 1)
  if (filterStatus.value === 'completed') return orders.value.filter(o => o.status === 2)
  return orders.value
})

// 生命周期
onMounted(() => {
  loadOrders()
})

onBeforeUnmount(() => {
  if (editorRef.value) {
    editorRef.value.destroy()
  }
})

// 方法
const handleCreated = (editor) => {
  editorRef.value = editor
}

const loadOrders = async () => {
  try {
    orders.value = await labApi.getOrders(filterStatus.value)
  } catch (e) {
    console.error('Failed to load orders', e)
  }
}

const selectOrder = (order) => {
  currentOrder.value = order
  if (order.status === 1) {
    resultHtml.value = ''
  }
}

const getStatusColor = (status) => {
  const colors = { 0: 'default', 1: 'warning', 2: 'success' }
  return colors[status] || 'default'
}

const getStatusText = (status) => {
  const texts = { 0: '待缴费', 1: '待检验', 2: '已完成' }
  return texts[status] || '待缴费'
}

const clearResult = () => {
  resultHtml.value = ''
  if (editorRef.value) {
    editorRef.value.clear()
  }
}

const submitResult = async () => {
  if (!resultHtml.value || resultHtml.value === '<p><br></p>') {
    message.warning('请输入检验结果')
    return
  }

  submitting.value = true
  try {
    const images = extractImageSources(resultHtml.value)
    await labApi.submitResult({
      orderId: currentOrder.value.orderId,
      resultText: resultHtml.value,
      resultImages: JSON.stringify(images)
    })
    message.success('检验结果已提交')
    currentOrder.value = null
    loadOrders()
  } catch (e) {
    message.error('提交失败')
    console.error(e)
  } finally {
    submitting.value = false
  }
}

// 模板
const templates = {
  normal: `
    <h3>检验报告</h3>
    <table border="1" cellpadding="8" cellspacing="0" style="border-collapse: collapse; width: 100%;">
      <tr style="background: #f5f5f5;"><th>检验项目</th><th>结果</th><th>参考值</th><th>单位</th></tr>
      <tr><td>项目1</td><td style="color: green;">正常</td><td>-</td><td>-</td></tr>
    </table>
    <p><strong>结论：</strong>未见明显异常</p>
  `,
  blood: `
    <h3>血常规检验报告</h3>
    <table border="1" cellpadding="8" cellspacing="0" style="border-collapse: collapse; width: 100%;">
      <tr style="background: #f5f5f5;"><th>检验项目</th><th>结果</th><th>参考值</th><th>单位</th></tr>
      <tr><td>白细胞计数(WBC)</td><td></td><td>4.0-10.0</td><td>×10⁹/L</td></tr>
      <tr><td>红细胞计数(RBC)</td><td></td><td>3.5-5.5</td><td>×10¹²/L</td></tr>
      <tr><td>血红蛋白(HGB)</td><td></td><td>110-160</td><td>g/L</td></tr>
      <tr><td>血小板计数(PLT)</td><td></td><td>100-300</td><td>×10⁹/L</td></tr>
      <tr><td>中性粒细胞百分比</td><td></td><td>50-70</td><td>%</td></tr>
      <tr><td>淋巴细胞百分比</td><td></td><td>20-40</td><td>%</td></tr>
    </table>
    <p><strong>结论：</strong></p>
  `,
  urine: `
    <h3>尿常规检验报告</h3>
    <table border="1" cellpadding="8" cellspacing="0" style="border-collapse: collapse; width: 100%;">
      <tr style="background: #f5f5f5;"><th>检验项目</th><th>结果</th><th>参考值</th></tr>
      <tr><td>尿比重(SG)</td><td></td><td>1.005-1.030</td></tr>
      <tr><td>尿酸碱度(pH)</td><td></td><td>5.0-8.0</td></tr>
      <tr><td>尿蛋白(PRO)</td><td></td><td>阴性(-)</td></tr>
      <tr><td>尿糖(GLU)</td><td></td><td>阴性(-)</td></tr>
      <tr><td>尿潜血(BLD)</td><td></td><td>阴性(-)</td></tr>
      <tr><td>白细胞(WBC)</td><td></td><td>阴性(-)</td></tr>
    </table>
    <p><strong>结论：</strong></p>
  `,
  liver: `
    <h3>肝功能检验报告</h3>
    <table border="1" cellpadding="8" cellspacing="0" style="border-collapse: collapse; width: 100%;">
      <tr style="background: #f5f5f5;"><th>检验项目</th><th>结果</th><th>参考值</th><th>单位</th></tr>
      <tr><td>谷丙转氨酶(ALT)</td><td></td><td>0-40</td><td>U/L</td></tr>
      <tr><td>谷草转氨酶(AST)</td><td></td><td>0-40</td><td>U/L</td></tr>
      <tr><td>总胆红素(TBIL)</td><td></td><td>3.4-17.1</td><td>μmol/L</td></tr>
      <tr><td>直接胆红素(DBIL)</td><td></td><td>0-6.8</td><td>μmol/L</td></tr>
      <tr><td>总蛋白(TP)</td><td></td><td>60-80</td><td>g/L</td></tr>
      <tr><td>白蛋白(ALB)</td><td></td><td>35-55</td><td>g/L</td></tr>
    </table>
    <p><strong>结论：</strong></p>
  `
}

const applyTemplate = (type) => {
  if (templates[type]) {
    resultHtml.value = templates[type]
  }
}

function parseImages(val) {
  if (!val) return []
  try {
    const arr = JSON.parse(val)
    return Array.isArray(arr) ? arr : []
  } catch (e) {
    return []
  }
}

function extractImageSources(html) {
  if (!html) return []
  const parser = new DOMParser()
  const doc = parser.parseFromString(html, 'text/html')
  const imgs = Array.from(doc.querySelectorAll('img'))
  return Array.from(new Set(imgs.map(img => img.getAttribute('src')).filter(Boolean)))
}
</script>

<style scoped lang="scss">
.lab-workstation {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #f0f2f5;
  padding: 16px;
  box-sizing: border-box;

  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
    background: #fff;
    padding: 16px 20px;
    border-radius: 8px;
    box-shadow: 0 1px 4px rgba(0,0,0,0.06);
    
    h2 {
      margin: 0;
      font-size: 18px;
    }
  }

  .workstation-content {
    flex: 1;
    display: flex;
    gap: 16px;
    overflow: hidden;
  }

  .order-list-panel {
    width: 360px;
    min-width: 360px;
    background: #fff;
    border-radius: 8px;
    padding: 12px;
    overflow: auto;
    box-shadow: 0 1px 4px rgba(0,0,0,0.06);

    :deep(.selected-row) {
      background: #e6f7ff !important;
    }

    :deep(.ant-table) {
      font-size: 13px;
    }

    .patient-cell {
      .name {
        display: block;
        font-weight: 500;
      }
      .meta {
        font-size: 12px;
        color: #999;
      }
    }
  }

  .result-panel {
    flex: 1;
    background: #fff;
    border-radius: 8px;
    padding: 16px;
    display: flex;
    flex-direction: column;
    overflow: hidden;
    box-shadow: 0 1px 4px rgba(0,0,0,0.06);

    .panel-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      padding-bottom: 16px;
      border-bottom: 1px solid #f0f0f0;
      margin-bottom: 16px;

      .order-info {
        h3 {
          margin: 0 0 8px 0;
          font-size: 16px;
        }
        .meta-info {
          display: flex;
          gap: 16px;
          font-size: 13px;
          color: #666;
        }
      }

      .panel-actions {
        display: flex;
        gap: 8px;
      }
    }

    .editor-container {
      flex: 1;
      display: flex;
      flex-direction: column;
      overflow: hidden;

      .editor-label {
        font-weight: 500;
        margin-bottom: 8px;
        color: #333;
      }

      .editor-wrapper {
        flex: 1;
        border: 1px solid #d9d9d9;
        border-radius: 4px;
        display: flex;
        flex-direction: column;
        overflow: hidden;

        .toolbar {
          border-bottom: 1px solid #d9d9d9;
        }

        .editor {
          flex: 1;
          overflow-y: auto;
        }
      }

      .template-section {
        margin-top: 16px;
        padding-top: 16px;
        border-top: 1px solid #f0f0f0;

        .section-title {
          font-size: 13px;
          color: #666;
          margin-bottom: 8px;
        }

        .template-buttons {
          display: flex;
          gap: 8px;
          flex-wrap: wrap;
        }
      }
    }

    .result-view {
      flex: 1;
      overflow: auto;

      .view-label {
        font-weight: 500;
        margin-bottom: 12px;
        color: #333;
      }

      .result-content {
        padding: 16px;
        background: #fafafa;
        border-radius: 4px;
        border: 1px solid #e8e8e8;
        
        :deep(table) {
          width: 100%;
          border-collapse: collapse;
          
          th, td {
            border: 1px solid #d9d9d9;
            padding: 8px;
            text-align: left;
          }
          
          th {
            background: #fafafa;
          }
        }

        :deep(h3) {
          margin-top: 0;
        }
      }

      .image-gallery {
        margin-top: 12px;
        display: grid;
        grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
        gap: 10px;

        .img-item {
          border: 1px solid #e8e8e8;
          border-radius: 4px;
          background: #fff;
          padding: 6px;
          display: flex;
          align-items: center;
          justify-content: center;

          img {
            max-width: 100%;
            max-height: 160px;
            object-fit: contain;
          }
        }
      }
    }
  }

  .empty-panel {
    flex: 1;
    background: #fff;
    border-radius: 8px;
    display: flex;
    align-items: center;
    justify-content: center;
    box-shadow: 0 1px 4px rgba(0,0,0,0.06);
  }
}
</style>
