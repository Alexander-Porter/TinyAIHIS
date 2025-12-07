<template>
  <a-modal
    v-model:open="visible"
    title="知识库检索"
    width="700px"
    :footer="null"
    @cancel="handleClose"
  >
    <div class="knowledge-search">
      <a-input-search
        v-model:value="searchQuery"
        placeholder="输入关键词搜索医学知识..."
        size="large"
        @search="handleSearch"
        :loading="searching"
      >
        <template #enterButton>
          <a-button type="primary">
            <template #icon><SearchOutlined /></template>
            搜索
          </a-button>
        </template>
      </a-input-search>
      
      <a-spin :spinning="searching" class="search-results">
        <div v-if="results.length === 0 && !searching" class="empty-state">
          <FileSearchOutlined style="font-size: 48px; color: #bfbfbf" />
          <p>{{ hasSearched ? '未找到相关知识' : '请输入关键词开始搜索' }}</p>
        </div>
        
        <div v-else class="result-list">
          <div v-for="(doc, idx) in results" :key="idx" class="result-item">
            <div class="result-header">
              <h4>{{ doc.diseaseName }}</h4>
              <a-tag color="blue">{{ doc.department }}</a-tag>
            </div>
            <div class="result-content">
              <div v-if="doc.content" class="content-preview">
                {{ getContentPreview(doc.content) }}
              </div>
            </div>
            <div class="result-actions">
              <a-button size="small" type="link" @click="viewDetail(doc)">
                查看详情
              </a-button>
              <a-button size="small" type="link" @click="insertToEmr(doc)">
                <template #icon><PlusOutlined /></template>
                引用到病历
              </a-button>
            </div>
          </div>
        </div>
      </a-spin>
    </div>
  </a-modal>
  
  <!-- Detail Modal -->
  <a-modal
    v-model:open="detailVisible"
    :title="currentDoc?.diseaseName"
    width="800px"
    :footer="null"
  >
    <div v-if="currentDoc" class="doc-detail">
      <a-descriptions bordered :column="1" size="small">
        <a-descriptions-item label="疾病名称">{{ currentDoc.diseaseName }}</a-descriptions-item>
        <a-descriptions-item label="所属科室">{{ currentDoc.department }}</a-descriptions-item>
      </a-descriptions>
      
      <div class="doc-content">
        <h4>详细内容</h4>
        <pre>{{ formatContent(currentDoc.content) }}</pre>
      </div>
      
      <div class="detail-actions">
        <a-button type="primary" @click="insertToEmr(currentDoc)">
          <template #icon><PlusOutlined /></template>
          引用到病历
        </a-button>
      </div>
    </div>
  </a-modal>
</template>

<script setup>
import { ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { SearchOutlined, FileSearchOutlined, PlusOutlined } from '@ant-design/icons-vue'

const props = defineProps({
  modelValue: Boolean,
  initialQuery: String
})

const emit = defineEmits(['update:modelValue', 'insert'])

const visible = ref(false)
const searchQuery = ref('')
const searching = ref(false)
const hasSearched = ref(false)
const results = ref([])
const detailVisible = ref(false)
const currentDoc = ref(null)

watch(() => props.modelValue, (val) => {
  visible.value = val
  if (val && props.initialQuery) {
    searchQuery.value = props.initialQuery
    handleSearch()
  }
})

watch(visible, (val) => {
  emit('update:modelValue', val)
  if (!val) {
    // Reset on close
    searchQuery.value = ''
    results.value = []
    hasSearched.value = false
  }
})

const handleClose = () => {
  visible.value = false
}

const handleSearch = async () => {
  if (!searchQuery.value.trim()) {
    message.warning('请输入搜索关键词')
    return
  }
  
  searching.value = true
  hasSearched.value = true
  
  try {
    const response = await fetch('/api/triage/search-knowledge', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${localStorage.getItem('token')}`
      },
      body: JSON.stringify({
        query: searchQuery.value,
        limit: 10
      })
    })
    
    const result = await response.json()
    if (result.success) {
      results.value = result.data || []
      if (results.value.length === 0) {
        message.info('未找到相关知识')
      }
    } else {
      message.error(result.message || '搜索失败')
    }
  } catch (error) {
    console.error('Search error:', error)
    message.error('搜索失败，请稍后重试')
  } finally {
    searching.value = false
  }
}

const getContentPreview = (content) => {
  if (!content) return '无内容预览'
  try {
    const parsed = JSON.parse(content)
    // Extract first text value found
    const extractText = (obj) => {
      if (typeof obj === 'string') return obj
      if (Array.isArray(obj)) {
        for (const item of obj) {
          const text = extractText(item)
          if (text) return text
        }
      } else if (typeof obj === 'object') {
        for (const key in obj) {
          const text = extractText(obj[key])
          if (text) return text
        }
      }
      return ''
    }
    const text = extractText(parsed)
    return text.length > 200 ? text.substring(0, 200) + '...' : text
  } catch (e) {
    return content.length > 200 ? content.substring(0, 200) + '...' : content
  }
}

const formatContent = (content) => {
  if (!content) return '无内容'
  try {
    const parsed = JSON.parse(content)
    return JSON.stringify(parsed, null, 2)
  } catch (e) {
    return content
  }
}

const viewDetail = (doc) => {
  currentDoc.value = doc
  detailVisible.value = true
}

const insertToEmr = (doc) => {
  emit('insert', doc)
  message.success('已引用到病历')
  visible.value = false
  detailVisible.value = false
}
</script>

<style scoped lang="scss">
.knowledge-search {
  .search-results {
    margin-top: 20px;
    min-height: 300px;
    max-height: 500px;
    overflow-y: auto;
    
    .empty-state {
      text-align: center;
      padding: 60px 0;
      color: #8c8c8c;
      
      p {
        margin-top: 16px;
        font-size: 14px;
      }
    }
    
    .result-list {
      .result-item {
        border: 1px solid #e8e8e8;
        border-radius: 8px;
        padding: 16px;
        margin-bottom: 12px;
        background: #fafafa;
        transition: all 0.3s;
        
        &:hover {
          box-shadow: 0 2px 8px rgba(0,0,0,0.1);
        }
        
        .result-header {
          display: flex;
          align-items: center;
          justify-content: space-between;
          margin-bottom: 8px;
          
          h4 {
            margin: 0;
            font-size: 16px;
            font-weight: 600;
            color: #262626;
          }
        }
        
        .result-content {
          .content-preview {
            font-size: 13px;
            color: #595959;
            line-height: 1.6;
            margin-bottom: 12px;
          }
        }
        
        .result-actions {
          display: flex;
          gap: 8px;
          justify-content: flex-end;
        }
      }
    }
  }
}

.doc-detail {
  .doc-content {
    margin-top: 20px;
    
    h4 {
      margin-bottom: 12px;
      font-weight: 600;
    }
    
    pre {
      background: #f5f5f5;
      padding: 16px;
      border-radius: 4px;
      max-height: 400px;
      overflow: auto;
      font-size: 13px;
      line-height: 1.6;
      white-space: pre-wrap;
      word-break: break-word;
    }
  }
  
  .detail-actions {
    margin-top: 20px;
    text-align: right;
  }
}
</style>
