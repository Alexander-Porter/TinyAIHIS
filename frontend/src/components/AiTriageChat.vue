<template>
  <div class="ai-triage-chat">
    <div class="chat-window">
      <div class="messages" ref="messagesRef">
        <div v-if="messages.length === 0" class="empty-state">
          <robot-outlined style="font-size: 48px; color: #1890ff; margin-bottom: 16px" />
          <p>我是您的AI导诊助手，请描述您的症状，我将为您推荐科室。</p>
        </div>

        <div v-for="(msg, idx) in messages" :key="idx" class="message-row" :class="msg.role">
          <div class="avatar">
            <user-outlined v-if="msg.role === 'user'" />
            <robot-outlined v-else />
          </div>
          <div class="message-bubble">
            <!-- Tool Call with Details -->
            <div v-if="msg.type === 'tool_call'" class="tool-call-block">
              <!-- Search Knowledge Base -->
              <div v-if="msg.data.name === 'search_knowledge_base'" class="tool-simple-info">
                <search-outlined />
                <span>已检索到 {{ msg.data.count || (msg.data.sources ? msg.data.sources.length : 0) }} 份相关信息</span>
              </div>

              <!-- Recommend Department -->
              <div v-else-if="msg.data.name === 'recommend_department'" class="dept-recommendation">
                <div class="dept-card">
                  <div class="dept-icon">
                    <medicine-box-outlined />
                  </div>
                  <div class="dept-info">
                    <h4>{{ msg.data.department?.name }}</h4>
                    <p>{{ msg.data.department?.location }}</p>
                  </div>
                  <a-button type="primary" @click="handleDeptSelect(msg.data.department)">
                    前往诊室
                  </a-button>
                </div>
              </div>

              <!-- Fallback for other tools -->
              <div v-else class="tool-call-header">
                <api-outlined /> 调用工具: {{ msg.data.name }}
              </div>
            </div>

            <!-- Tool/Status Updates -->
            <div v-else-if="msg.type === 'tool'" class="tool-update">
              <search-outlined /> {{ msg.content }}
            </div>

            <!-- Thinking Process -->
            <div v-else-if="msg.type === 'thought'" class="thought-block">
              <div class="thought-header" @click="msg.collapsed = !msg.collapsed">
                <bulb-outlined /> 思考过程
                <down-outlined :rotate="msg.collapsed ? 0 : 180" class="collapse-icon" />
              </div>
              <div class="thought-content" v-show="!msg.collapsed">
                {{ msg.content }}
              </div>
            </div>

            <!-- Final Result Card -->
            <div v-else-if="msg.type === 'result'" class="result-card">
              <div class="result-header">
                <medicine-box-outlined /> 推荐科室：{{ msg.data.department }}
              </div>
              <div class="result-body">
                <p><strong>推荐理由：</strong>{{ msg.data.reason }}</p>
                <p v-if="msg.data.possibleDiseases"><strong>可能疾病：</strong>{{ msg.data.possibleDiseases }}</p>
              </div>
              <div class="result-actions">
                <a-button type="primary" block @click="handleResultSelect(msg.data)">
                  去挂号
                </a-button>
              </div>
            </div>

            <!-- Normal Text Message -->
            <div v-else class="text-content" v-html="renderMarkdown(msg.content)"></div>
          </div>
        </div>

        <div v-if="loading" class="loading-status">
          <loading-outlined /> {{ statusText }}
        </div>
      </div>
    </div>

    <div class="input-area" v-if="!hasUserMessage">
      <a-textarea v-model:value="inputText" placeholder="请描述您的症状，例如：头痛、发烧..." :auto-size="{ minRows: 2, maxRows: 4 }"
        @pressEnter.prevent="sendMessage" :disabled="loading" />
      <a-button type="primary" @click="sendMessage" :loading="loading">发送</a-button>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick, watch, computed } from 'vue'
import {
  UserOutlined, RobotOutlined, LoadingOutlined,
  SearchOutlined, BulbOutlined, DownOutlined,
  MedicineBoxOutlined, ApiOutlined, FileTextOutlined
} from '@ant-design/icons-vue'
import { marked } from 'marked'

const props = defineProps({
  initialQuery: String
})

const emit = defineEmits(['select'])

const messages = ref([])
const inputText = ref('')
const loading = ref(false)
const statusText = ref('')
const messagesRef = ref(null)

const hasUserMessage = computed(() => {
  return messages.value.some(m => m.role === 'user')
})

const renderMarkdown = (text) => {
  return marked(text || '')
}

const scrollToBottom = () => {
  nextTick(() => {
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight
    }
  })
}

const handleEvent = (event, data) => {
  // Remove leading space if present (standard SSE adds a space after colon)
  if (data.startsWith(' ')) data = data.slice(1)

  // Ignore empty or null messages to prevent interruption
  if (event === 'message' && (data === 'null' || !data)) return

  // Decode Base64
  try {
    data = decodeURIComponent(escape(window.atob(data)))
  } catch (e) {
    console.error('Failed to decode base64', e)
    return
  }

  if (event === 'status') {
    statusText.value = data
  } else if (event === 'tool_call') {
    // Handle structured tool call information
    try {
      const toolCall = JSON.parse(data)
      messages.value.push({
        role: 'assistant',
        type: 'tool_call',
        data: toolCall
      })
    } catch (e) {
      console.error('Failed to parse tool_call', e)
    }
  } else if (event === 'tool_call') {
    let lastMsg = messages.value[messages.value.length - 1]
    if (lastMsg && lastMsg.type === 'tool') {
      lastMsg.content += data
    } else {
      messages.value.push({
        role: 'assistant',
        type: 'tool',
        content: data
      })
    }
  } else if (event === 'thought') {
    let lastMsg = messages.value[messages.value.length - 1]
    if (lastMsg && lastMsg.type === 'thought') {
      lastMsg.content += data
    } else {
      messages.value.push({
        role: 'assistant',
        type: 'thought',
        content: data,
        collapsed: false
      })
    }
  } else if (event === 'message') {
    let lastMsg = messages.value[messages.value.length - 1]
    // 只有当上一条消息是文本且是助手发的，才追加内容
    // 关键修改：如果上一条是 tool_call，这里会进入 else 分支创建新气泡
    if (lastMsg && lastMsg.type === 'text' && lastMsg.role === 'assistant') {
      lastMsg.content += data
    } else {
      // 即使是空白字符也创建气泡，防止首字丢失
      messages.value.push({
        role: 'assistant',
        type: 'text',
        content: data
      })
    }
  } else if (event === 'result') {
    try {
      const result = JSON.parse(data)
      messages.value.push({
        role: 'assistant',
        type: 'result',
        data: result
      })
    } catch (e) {
      console.error('Failed to parse result', e)
    }
  }

  scrollToBottom()
}

let buffer = ''
const processBuffer = (chunk) => {
  buffer += chunk
  const lines = buffer.split('\n')
  buffer = lines.pop() || ''

  let currentEvent = null

  for (const line of lines) {
    if (line.trim() === '') continue

    if (line.startsWith('event:')) {
      currentEvent = line.slice(6).trim()
    } else if (line.startsWith('data:')) {
      const data = line.slice(5) // Keep spaces
      if (currentEvent) {
        handleEvent(currentEvent, data)
      }
      // Reset event is NOT done here because one event can have multiple data lines?
      // Standard SSE: event -> data -> data -> empty line -> dispatch
      // But my backend seems to send event -> data -> empty line.
      // Let's assume event persists until next event or empty line?
      // Actually, usually 'event' line sets the type for the *next* 'data' lines until dispatch.
      // My backend sends:
      // event: thought
      // data: xxx
      // \n

      // So if I see 'data:', I handle it with currentEvent.
    }
  }
}

const sendMessage = async () => {
  if (!inputText.value.trim() || loading.value) return

  const text = inputText.value
  inputText.value = ''

  // Add user message
  messages.value.push({
    role: 'user',
    type: 'text',
    content: text
  })

  loading.value = true
  statusText.value = '正在连接...'

  try {
    const response = await fetch('/api/triage/stream', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${localStorage.getItem('token')}`
      },
      body: JSON.stringify({
        description: text,
        bodyPart: '' // Optional
      })
    })

    const reader = response.body.getReader()
    const decoder = new TextDecoder()

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      const chunk = decoder.decode(value, { stream: true })
      processBuffer(chunk)
    }
  } catch (e) {
    console.error(e)
    messages.value.push({
      role: 'assistant',
      type: 'text',
      content: '抱歉，服务暂时不可用，请稍后再试。'
    })
  } finally {
    loading.value = false
    statusText.value = ''
  }
}

watch(() => props.initialQuery, (val) => {
  if (val) {
    inputText.value = val
    sendMessage()
  }
})

const handleDeptSelect = (department) => {
  if (!department) return
  const deptId = department.id ?? department.deptId ?? null
  const deptName = department.name ?? department.deptName ?? ''
  emit('select', {
    deptId,
    deptName,
    department: deptName,
    raw: department
  })
}

const handleResultSelect = (result) => {
  if (!result) return
  const deptInfo = typeof result.department === 'object' ? result.department : null
  const deptId = result.deptId ?? deptInfo?.id ?? deptInfo?.deptId ?? null
  const deptName = result.deptName ?? (deptInfo ? (deptInfo.name ?? deptInfo.deptName) : (typeof result.department === 'string' ? result.department : ''))
  emit('select', {
    deptId,
    deptName,
    department: deptName,
    raw: result
  })
}
</script>

<style scoped lang="scss">
.ai-triage-chat {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #f5f7fa;
  border-radius: 8px;
  overflow: hidden;

  .chat-window {
    flex: 1;
    overflow-y: auto;
    padding: 20px;

    .empty-state {
      text-align: center;
      color: #999;
      margin-top: 40px;
    }

    .message-row {
      display: flex;
      gap: 12px;
      margin-bottom: 20px;

      &.user {
        flex-direction: row-reverse;

        .message-bubble {
          background: #1890ff;
          color: #fff;
          border-radius: 12px 0 12px 12px;
        }
      }

      &.assistant {
        .message-bubble {
          background: #fff;
          border-radius: 0 12px 12px 12px;
          box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
        }
      }

      .avatar {
        width: 36px;
        height: 36px;
        border-radius: 50%;
        background: #e6f7ff;
        color: #1890ff;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 20px;
        flex-shrink: 0;
      }

      .message-bubble {
        max-width: 80%;
        padding: 12px;
        position: relative;

        .tool-call-block {
          background: #e6f7ff;
          border: 1px solid #91d5ff;
          border-radius: 8px;
          margin-bottom: 12px;
          overflow: hidden;

          .tool-call-header {
            padding: 8px 12px;
            background: #bae7ff;
            color: #0050b3;
            font-size: 13px;
            font-weight: 600;
            display: flex;
            align-items: center;
            gap: 8px;
          }

          .tool-call-content {
            padding: 10px 12px;

            .tool-param {
              margin-bottom: 8px;
              font-size: 13px;

              .param-label {
                font-weight: 600;
                color: #595959;
                margin-right: 8px;
              }

              .param-value {
                color: #262626;
                background: #fff;
                padding: 2px 6px;
                border-radius: 3px;
              }
            }

            .tool-sources {
              .param-label {
                font-weight: 600;
                color: #595959;
                display: block;
                margin-bottom: 6px;
              }

              .source-list {
                .source-item {
                  padding: 4px 8px;
                  background: #fff;
                  border-radius: 4px;
                  margin-bottom: 4px;
                  font-size: 12px;
                  color: #262626;
                  display: flex;
                  align-items: center;
                  gap: 6px;
                }
              }
            }
          }
        }

        .tool-update {
          color: #666;
          font-size: 12px;
          margin-bottom: 8px;
          display: flex;
          align-items: center;
          gap: 6px;
          background: #f5f5f5;
          padding: 4px 8px;
          border-radius: 4px;
        }

        .thought-block {
          background: #f9f9f9;
          border-left: 3px solid #faad14;
          margin-bottom: 12px;
          border-radius: 4px;
          overflow: hidden;

          .thought-header {
            padding: 6px 10px;
            font-size: 12px;
            color: #faad14;
            cursor: pointer;
            display: flex;
            align-items: center;
            gap: 6px;
            font-weight: 500;

            .collapse-icon {
              margin-left: auto;
              transition: transform 0.3s;
            }
          }

          .thought-content {
            padding: 8px 10px;
            font-size: 13px;
            color: #666;
            white-space: pre-wrap;
            border-top: 1px solid #eee;
          }
        }

        .result-card {
          border: 1px solid #e8e8e8;
          border-radius: 8px;
          padding: 12px;
          background: #fafafa;

          .result-header {
            font-weight: bold;
            font-size: 16px;
            color: #1890ff;
            margin-bottom: 8px;
            display: flex;
            align-items: center;
            gap: 8px;
          }

          .result-body {
            font-size: 14px;
            color: #333;
            margin-bottom: 12px;
          }
        }

        .tool-simple-info {
          display: flex;
          align-items: center;
          gap: 8px;
          color: #8c8c8c;
          font-size: 13px;
          padding: 4px 0;
        }

        .dept-recommendation {
          margin-top: 8px;

          .dept-card {
            background: #fff;
            border: 1px solid #e6f7ff;
            border-radius: 8px;
            padding: 16px;
            display: flex;
            align-items: center;
            gap: 16px;
            box-shadow: 0 2px 8px rgba(24, 144, 255, 0.1);

            .dept-icon {
              font-size: 24px;
              color: #1890ff;
              background: #e6f7ff;
              width: 48px;
              height: 48px;
              border-radius: 50%;
              display: flex;
              align-items: center;
              justify-content: center;
            }

            .dept-info {
              flex: 1;

              h4 {
                margin: 0 0 4px;
                font-size: 16px;
                font-weight: 600;
                color: #262626;
              }

              p {
                margin: 0;
                font-size: 13px;
                color: #8c8c8c;
              }
            }
          }
        }

        .text-content {
          line-height: 1.6;

          :deep(p) {
            margin-bottom: 8px;

            &:last-child {
              margin-bottom: 0;
            }
          }

          :deep(ul),
          :deep(ol) {
            padding-left: 20px;
            margin-bottom: 8px;
          }

          :deep(h1),
          :deep(h2),
          :deep(h3),
          :deep(h4) {
            font-weight: 600;
            margin: 12px 0 8px;
            color: #333;
          }

          :deep(h1) {
            font-size: 1.4em;
          }

          :deep(h2) {
            font-size: 1.2em;
          }

          :deep(h3) {
            font-size: 1.1em;
          }

          :deep(code) {
            background: rgba(0, 0, 0, 0.05);
            padding: 2px 4px;
            border-radius: 3px;
            font-family: monospace;
          }

          :deep(pre) {
            background: #f5f5f5;
            padding: 10px;
            border-radius: 4px;
            overflow-x: auto;
            margin-bottom: 8px;
          }

          :deep(blockquote) {
            border-left: 4px solid #ddd;
            margin: 0 0 8px;
            padding-left: 12px;
            color: #666;
          }
        }
      }
    }

    .loading-status {
      text-align: center;
      color: #999;
      font-size: 12px;
      margin-top: 10px;
    }
  }

  .input-area {
    padding: 16px;
    background: #fff;
    border-top: 1px solid #e8e8e8;
    display: flex;
    gap: 10px;
    align-items: flex-end;
  }
}
</style>
