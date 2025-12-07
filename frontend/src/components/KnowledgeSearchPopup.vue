<template>
  <a-modal v-model:open="visible" title="知识库检索" width="700px" :footer="null" @cancel="handleClose">
    <div class="knowledge-search">
      <a-input-search v-model:value="searchQuery" placeholder="输入关键词搜索医学知识..." size="large" @search="handleSearch"
        :loading="searching">
        <template #enterButton>
          <a-button type="primary">
            <template #icon>
              <SearchOutlined />
            </template>
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
              <h4>{{ getTitle(doc) }}</h4>
              <a-tag color="blue">{{ getDepartment(doc) || '未分科室' }}</a-tag>
            </div>
            <div class="result-content">
              <div class="content-preview" v-html="getContentPreviewHtml(doc)"></div>
            </div>
            <div class="result-actions">
              <a-button size="small" type="link" @click="viewDetail(doc)">
                查看详情
              </a-button>

            </div>
          </div>
        </div>
      </a-spin>
    </div>
  </a-modal>

  <!-- Detail Modal -->
  <a-modal v-model:open="detailVisible" :title="getTitle(currentDoc)" width="800px" :footer="null">
    <div v-if="currentDoc" class="doc-detail">
      <a-descriptions bordered :column="1" size="small">
        <a-descriptions-item label="疾病名称">{{ getTitle(currentDoc) }}</a-descriptions-item>
        <a-descriptions-item label="所属科室">{{ getDepartment(currentDoc) }}</a-descriptions-item>
      </a-descriptions>

      <div class="doc-content">
        <h4>详细内容</h4>
        <div class="content-html" v-html="getFullContentHtml(currentDoc)"></div>
      </div>

      <div class="detail-actions">
      </div>
    </div>
  </a-modal>
</template>

<script setup>
import { ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { SearchOutlined, FileSearchOutlined, PlusOutlined } from '@ant-design/icons-vue'
import api from '@/utils/api'

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
    const list = await api.post('/triage/search-knowledge', {
      query: searchQuery.value,
      limit: 10
    })

    results.value = Array.isArray(list) ? list : []
    if (results.value.length === 0) {
      message.info('未找到相关知识')
    }
  } catch (error) {
    console.error('Search error:', error)
  } finally {
    searching.value = false
  }
}

const getTitle = (doc) => doc?.diseaseName || doc?.title || doc?.name || '未命名条目'

const getDepartment = (doc) => doc?.department || doc?.section_group_tag?.name || doc?.section?.name || ''

const stripHtml = (html = '') => html.replace(/<[^>]+>/g, ' ').replace(/\s+/g, ' ').trim()

const parseContentHtml = (doc) => {
  // Try content field first (may be JSON string containing article info)
  if (doc?.content) {
    try {
      const parsed = typeof doc.content === 'string' ? JSON.parse(doc.content) : doc.content
      if (Array.isArray(parsed?.article)) {
        return parsed.article.map(a => a?.detail || '').filter(Boolean).join('\n') || ''
      }
      if (parsed?.detail) return parsed.detail
      if (typeof parsed === 'string') return parsed
      return JSON.stringify(parsed)
    } catch (e) {
      return doc.content
    }
  }

  // Fallback to article array on root
  if (Array.isArray(doc?.article)) {
    return doc.article.map(a => a?.detail || '').filter(Boolean).join('\n') || ''
  }

  return ''
}

const getContentPreviewHtml = (doc) => {
  const html = parseContentHtml(doc)
  if (!html) return '无内容预览'
  const text = stripHtml(html)
  if (!text) return '无内容预览'
  const preview = text.length > 200 ? `${text.slice(0, 200)}...` : text
  return preview
}

const getFullContentHtml = (doc) => {
  const html = parseContentHtml(doc)
  if (html) return html
  // Last resort: stringify the doc for debugging
  return `<pre>${JSON.stringify(doc, null, 2)}</pre>`
}

const viewDetail = (doc) => {
  currentDoc.value = doc
  detailVisible.value = true
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
          box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
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

    .content-html {
      background: #f5f5f5;
      padding: 16px;

      .content-html {
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

      margin-top: 20px;
      text-align: right;
    }
  }
}
</style>
