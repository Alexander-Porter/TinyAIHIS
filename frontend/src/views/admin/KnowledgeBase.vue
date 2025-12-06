<template>
  <div class="knowledge-base">
    <div class="page-header">
      <h2>知识库管理</h2>
      <a-button type="primary" @click="showAdd">
        <template #icon><PlusOutlined /></template> 新增文档
      </a-button>
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

    <a-modal v-model:open="modalVisible" :title="modalTitle" @ok="handleOk">
      <a-form :model="form" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
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
          <a-textarea v-model:value="form.content" :rows="10" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { kbApi } from '@/utils/api'

const loading = ref(false)
const documents = ref([])
const modalVisible = ref(false)
const modalTitle = ref('新增文档')
const isEdit = ref(false)

const form = reactive({
  id: '',
  diseaseName: '',
  department: '',
  content: ''
})

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id' },
  { title: '疾病名称', dataIndex: 'diseaseName', key: 'diseaseName' },
  { title: '科室', dataIndex: 'department', key: 'department' },
  { title: '操作', key: 'action', width: 150 }
]

const loadData = async () => {
  loading.value = true
  try {
    const data = await kbApi.list()
    documents.value = data || []
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
.danger {
  color: #ff4d4f;
}
</style>