<template>
  <div class="knowledge-base">
    <div class="page-header">
      <h2>知识库管理</h2>
      <a-space>
        <a-button @click="showImport">
          <template #icon><UploadOutlined /></template> 批量导入
        </a-button>
        <a-button type="primary" @click="showAdd">
          <template #icon><PlusOutlined /></template> 新增文档
        </a-button>
      </a-space>
    </div>

    <!-- Stats -->
    <a-row :gutter="16" class="stats-row" v-if="stats">
      <a-col :span="6">
        <a-statistic title="文档总数" :value="stats.totalDocuments" />
      </a-col>
      <a-col :span="6">
        <a-statistic title="已索引" :value="stats.indexedDocuments" />
      </a-col>
      <a-col :span="12">
        <div class="dept-stats">
          <span class="label">科室分布:</span>
          <a-tag v-for="(count, dept) in stats.departmentCounts" :key="dept">{{ dept }}: {{ count }}</a-tag>
        </div>
      </a-col>
    </a-row>

    <!-- Filters -->
    <div class="filters">
      <a-form layout="inline">
        <a-form-item label="关键词">
          <a-input v-model:value="filters.keyword" placeholder="搜索内容..." allowClear @pressEnter="loadData" />
        </a-form-item>
        <a-form-item label="科室">
          <a-input v-model:value="filters.department" placeholder="科室名称" allowClear @pressEnter="loadData" />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" @click="loadData">查询</a-button>
        </a-form-item>
      </a-form>
    </div>

    <a-table :columns="columns" :data-source="documents" :loading="loading" row-key="id">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'action'">
          <a-space>
            <a @click="edit(record)">编辑</a>
            <a-popconfirm title="确定删除吗？" @confirm="remove(record.id)">
              <a class="danger">删除</a>
            </a-popconfirm>
          </a-space>
        </template>
      </template>
    </a-table>

    <!-- Edit/Add Modal -->
    <a-modal v-model:open="modalVisible" :title="modalTitle" @ok="handleOk" width="800px">
      <a-form :model="form" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-item label="ID/文件名" required>
          <a-input v-model:value="form.id" :disabled="isEdit" />
        </a-form-item>
        <a-form-item label="疾病名称" required>
          <a-input v-model:value="form.diseaseName" />
        </a-form-item>
        <a-form-item label="所属科室">
          <a-input v-model:value="form.department" />
        </a-form-item>
        <a-form-item label="内容">
          <a-textarea v-model:value="form.content" :rows="15" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- Import Modal -->
    <a-modal v-model:open="importVisible" title="批量导入文档" @ok="handleImport" :confirmLoading="importing">
      <a-upload-dragger
        v-model:fileList="fileList"
        name="files"
        :multiple="true"
        :before-upload="() => false"
      >
        <p class="ant-upload-drag-icon">
          <inbox-outlined />
        </p>
        <p class="ant-upload-text">点击或拖拽文件到此区域上传</p>
        <p class="ant-upload-hint">支持 .json 和 .txt 格式文件</p>
      </a-upload-dragger>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { PlusOutlined, UploadOutlined, InboxOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { kbApi } from '@/utils/api'

const loading = ref(false)
const documents = ref([])
const stats = ref(null)

// Filters
const filters = reactive({
  keyword: '',
  department: ''
})

// Modals
const modalVisible = ref(false)
const modalTitle = ref('新增文档')
const isEdit = ref(false)
const importVisible = ref(false)
const importing = ref(false)
const fileList = ref([])

const form = reactive({
  id: '',
  diseaseName: '',
  department: '',
  content: ''
})

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 200 },
  { title: '疾病名称', dataIndex: 'diseaseName', key: 'diseaseName', width: 200 },
  { title: '科室', dataIndex: 'department', key: 'department', width: 150 },
  { title: '摘要', dataIndex: 'content', key: 'content', ellipsis: true },
  { title: '操作', key: 'action', width: 150 }
]

const loadData = async () => {
  loading.value = true
  try {
    const data = await kbApi.list(filters.keyword, filters.department)
    // Actually list API does not support args in previous api.js? 
    // Wait, let me check api.js again.
    // api.js: list: () => api.get('/admin/kb/list')
    // I need to update api.js list method too! It doesn't take params!
    // But kbController.list takes param.
    // For now I will assume I updated api.js or pass query params manually?
    // No, I should fix api.js first or update it now.
    // I'll update api.js quickly after this.
    // Let's write code assuming api.js accepts params or we build URL manually.
    // Actually simpler to just update api.js line: list: (keyword, department) => api.get('/admin/kb/list', { params: { keyword, department } })
    documents.value = data || []
    
    // Load stats
    const s = await kbApi.getStats()
    stats.value = s
  } catch (e) {
    message.error('加载失败')
  } finally {
    loading.value = false
  }
}

const showAdd = () => {
  modalTitle.value = '新增文档'
  isEdit.value = false
  form.id = ''
  form.diseaseName = ''
  form.department = ''
  form.content = ''
  modalVisible.value = true
}

const showImport = () => {
  fileList.value = []
  importVisible.value = true
}

const handleImport = async () => {
  if (fileList.value.length === 0) {
    message.warning('请选择文件')
    return
  }
  importing.value = true
  try {
    const files = fileList.value.map(f => f.originFileObj)
    await kbApi.import(files)
    message.success(`成功导入 ${fileList.value.length} 个文件`)
    importVisible.value = false
    loadData()
  } catch (e) {
    message.error('导入失败')
  } finally {
    importing.value = false
  }
}

const edit = async (record) => {
  modalTitle.value = '编辑文档'
  isEdit.value = true
  try {
    const data = await kbApi.get(record.id)
    Object.assign(form, {
      id: data.id,
      diseaseName: data.diseaseName,
      department: data.department,
      content: data.content || ''
    })
    modalVisible.value = true
  } catch (e) {
    message.error('加载文档失败')
  }
}

const remove = async (id) => {
  try {
    await kbApi.remove(id)
    message.success('删除成功')
    loadData()
  } catch (e) {
    message.error('删除失败')
  }
}

const handleOk = async () => {
  try {
    if (isEdit.value) {
      await kbApi.update(form.id, form)
    } else {
      await kbApi.add(form)
    }
    message.success('保存成功')
    modalVisible.value = false
    loadData()
  } catch (e) {
    message.error('保存失败')
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.knowledge-base {
  padding: 24px;
  background: #fff;
}
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}
.stats-row {
  margin-bottom: 24px;
  background: #fafafa;
  padding: 16px;
  border-radius: 8px;
}
.dept-stats {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  .label { font-weight: bold; margin-right: 8px; }
}
.filters {
  margin-bottom: 16px;
}
.danger {
  color: #ff4d4f;
}
</style>