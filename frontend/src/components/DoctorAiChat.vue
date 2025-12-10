<template>
  <div class="doctor-ai-chat">
    <div class="chat-window">
      <div class="messages" ref="messagesRef">
        <div v-if="messages.length === 0" class="empty-state">
          <robot-outlined style="font-size: 48px; color: #1890ff; margin-bottom: 16px" />
          <p>我是您的AI诊疗助手，正在分析病历...</p>
        </div>

        <div v-for="(msg, idx) in messages" :key="idx" class="message-row" :class="msg.role">
          <div class="avatar">
            <user-outlined v-if="msg.role === 'user'" />
            <robot-outlined v-else />
          </div>
          <div class="message-bubble">
            <!-- Tool Call with Details -->
            <div v-if="msg.type === 'tool_call'" class="tool-call-block">
              <div class="tool-call-header">
                <api-outlined /> 调用工具: {{ msg.data.name }}
              </div>
              <div class="tool-call-content">
                <div class="tool-param">
                  <span class="param-label">查询:</span>
                  <span class="param-value">{{ msg.data.query }}</span>
                </div>
                <div class="tool-sources" v-if="msg.data.sources && msg.data.sources.length > 0">
                  <span class="param-label">检索结果:</span>
                  <div class="source-list">
                    <div v-for="(source, idx) in msg.data.sources" :key="idx" class="source-item">
                      <file-text-outlined /> {{ source.disease }} <a-tag size="small">{{ source.department }}</a-tag>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- Tool Updates -->
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

            <!-- Normal Text Message -->
            <div v-else class="text-content">
              <div v-html="renderMarkdown(msg.content)"></div>
              <div class="message-actions" v-if="msg.role === 'assistant' && !msg.loading">
                <a-button size="small" type="link" @click="$emit('apply', msg.content)">
                  <template #icon><copy-outlined /></template> 引用
                </a-button>
              </div>
            </div>
          </div>
        </div>

        <div v-if="loading" class="loading-status">
          <loading-outlined /> {{ statusText }}
        </div>
      </div>
    </div>

    <div class="input-area">
      <a-textarea v-model:value="inputText" placeholder="输入补充信息或追问..." :auto-size="{ minRows: 2, maxRows: 4 }"
        @pressEnter.prevent="sendMessage" :disabled="loading" />
      <a-button type="primary" @click="sendMessage" :loading="loading">发送</a-button>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted, watch } from 'vue'
import {
  UserOutlined, RobotOutlined, LoadingOutlined,
  BulbOutlined, DownOutlined, CopyOutlined,
  ApiOutlined, FileTextOutlined, SearchOutlined
} from '@ant-design/icons-vue'
import { marked } from 'marked'

const props = defineProps({
  initialQuery: String,
  patientId: [Number, String]
})

const emit = defineEmits(['apply'])

const messages = ref([])
const inputText = ref('')
const loading = ref(false)
const statusText = ref('')
const messagesRef = ref(null)
const conversationId = ref('')

watch(() => props.patientId, () => {
  conversationId.value = ''
  messages.value = []
  if (props.initialQuery) {
    sendMessage(props.initialQuery)
  }
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

const sendMessage = async (text = null) => {
  // Handle event object being passed as first argument
  if (text && typeof text !== 'string') {
    text = null
  }

  const content = text || inputText.value
  if (!content || !content.trim() || loading.value) return

  if (!text) inputText.value = ''

  // Add user message if it's not the initial auto-send
  if (!text || messages.value.length > 0) {
    messages.value.push({
      role: 'user',
      type: 'text',
      content: content
    })
  }

  loading.value = true
  statusText.value = '正在思考...'

  scrollToBottom()

  try {
    const response = await fetch('/api/triage/doctor-assist', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${localStorage.getItem('token')}`
      },
      body: JSON.stringify({
        content: content,
        patientId: props.patientId,
        conversationId: conversationId.value
      })
    })
    // Explicitly fail fast on non-2xx so we don't fall into stream parsing with an error body
    if (!response.ok || !response.body) {
      throw new Error(`Request failed: ${response.status}`)
    }

    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      const chunk = decoder.decode(value, { stream: true })
      buffer += chunk
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''

      let currentEvent = null

      for (const line of lines) {
        if (line.startsWith('event:')) {
          currentEvent = line.slice(6).trim()
        } else if (line.startsWith('data:')) {
          let data = line.slice(5) // Keep spaces

          // Decode Base64
          try {
            if (data.startsWith(' ')) data = data.slice(1)
            data = decodeURIComponent(escape(window.atob(data)))
          } catch (e) {
            console.error('Failed to decode base64', e)
            continue
          }

          if (currentEvent === 'session') {
            conversationId.value = data
          } else if (currentEvent === 'tool_call') {
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
          } else if (currentEvent === 'tool') {
            messages.value.push({
              role: 'assistant',
              type: 'tool',
              content: data
            })
          } else if (currentEvent === 'thought') {
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
          } else if (currentEvent === 'message') {
            if (lastMsg && lastMsg.type === 'text' && lastMsg.role === 'assistant') {
              lastMsg.content += data
            } else {
              // If starting a new message bubble, ignore leading whitespace/newlines
              // to prevent "air bubbles" (empty message bubbles)
              if (!data.trim()) return

              messages.value.push({
                role: 'assistant',
                type: 'text',
                content: data,
                loading: true
              })
            }
          }

          scrollToBottom()
          currentEvent = null
        }
      }
    }
  } catch (e) {
    console.error(e)
    messages.value.push({
      role: 'assistant',
      type: 'text',
      content: '抱歉，服务暂时不可用。'
    })
  } finally {
    loading.value = false
    statusText.value = ''

    // Turn off loading for the last message if it's a text message
    const lastMsg = messages.value[messages.value.length - 1]
    if (lastMsg && lastMsg.type === 'text') {
      lastMsg.loading = false
    }

    // Collapse thought after done
    const thoughtMsgs = messages.value.filter(m => m.type === 'thought')
    thoughtMsgs.forEach(m => m.collapsed = true)
  }
}

onMounted(() => {
  if (props.initialQuery) {
    sendMessage(props.initialQuery)
  }
})
</script>

<style scoped lang="scss">
.doctor-ai-chat {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #f0f2f5;

  .chat-window {
    flex: 1;
    overflow-y: auto;
    padding: 16px;

    .messages {
      display: flex;
      flex-direction: column;
      gap: 16px;
    }

    .empty-state {
      text-align: center;
      padding: 40px;
      color: #999;
    }

    .message-row {
      display: flex;
      gap: 12px;

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
        width: 32px;
        height: 32px;
        border-radius: 50%;
        background: #e6f7ff;
        display: flex;
        align-items: center;
        justify-content: center;
        color: #1890ff;
        flex-shrink: 0;
      }

      .message-bubble {
        max-width: 80%;
        padding: 12px;
        font-size: 14px;
        line-height: 1.6;

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
          background: #f6ffed;
          border: 1px solid #b7eb8f;
          border-radius: 8px;
          margin-bottom: 8px;
          overflow: hidden;

          .thought-header {
            padding: 8px 12px;
            background: #f6ffed;
            color: #52c41a;
            font-size: 12px;
            cursor: pointer;
            display: flex;
            align-items: center;
            gap: 8px;
            font-weight: bold;

            .collapse-icon {
              margin-left: auto;
              transition: transform 0.3s;
            }
          }

          .thought-content {
            padding: 8px 12px;
            border-top: 1px solid #b7eb8f;
            font-size: 12px;
            color: #666;
            white-space: pre-wrap;
            max-height: 200px;
            overflow-y: auto;
          }
        }

        .text-content {
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

        .message-actions {
          margin-top: 8px;
          border-top: 1px solid #eee;
          padding-top: 4px;
          text-align: right;
        }
      }
    }

    .loading-status {
      text-align: center;
      color: #999;
      font-size: 12px;
      padding: 8px;
    }
  }

  .input-area {
    padding: 16px;
    background: #fff;
    border-top: 1px solid #e8e8e8;
    display: flex;
    gap: 12px;
    align-items: flex-end;
  }
}
</style>