<template>
  <div 
    class="editable-area"
    :class="{ 
      'is-editing': isEditing, 
      'is-readonly': readonly,
      'is-empty': !modelValue 
    }"
    @dblclick="startEdit"
  >
    <template v-if="!isEditing">
      <div class="area-value" :class="{ placeholder: !modelValue }">
        {{ displayValue }}
      </div>
    </template>
    <template v-else>
      <textarea
        ref="textareaRef"
        :value="modelValue"
        :rows="rows"
        @input="onInput"
        @blur="finishEdit"
        @keydown.esc="cancelEdit"
        class="area-input"
      ></textarea>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, nextTick } from 'vue'

const props = defineProps({
  modelValue: { type: String, default: '' },
  readonly: { type: Boolean, default: false },
  placeholder: { type: String, default: '双击编辑内容...' },
  rows: { type: Number, default: 3 }
})

const emit = defineEmits(['update:modelValue'])

const isEditing = ref(false)
const textareaRef = ref(null)
const originalValue = ref('')

const displayValue = computed(() => {
  return props.modelValue || props.placeholder
})

const startEdit = () => {
  if (props.readonly) return
  originalValue.value = props.modelValue
  isEditing.value = true
  nextTick(() => {
    textareaRef.value?.focus()
    // 将光标移到末尾
    const len = textareaRef.value?.value?.length || 0
    textareaRef.value?.setSelectionRange(len, len)
  })
}

const onInput = (e) => {
  emit('update:modelValue', e.target.value)
}

const finishEdit = () => {
  isEditing.value = false
}

const cancelEdit = () => {
  emit('update:modelValue', originalValue.value)
  isEditing.value = false
}
</script>

<style scoped lang="scss">
.editable-area {
  position: relative;
  min-height: 2em;
  padding: 5px 10px;
  border: 1px dashed transparent;
  border-radius: 4px;
  cursor: text;
  transition: all 0.2s;

  &:not(.is-readonly):hover {
    border-color: #d9d9d9;
    background: #fafafa;
  }

  &.is-readonly {
    cursor: default;
  }

  .area-value {
    white-space: pre-wrap;
    word-break: break-word;
    line-height: 1.8;
    
    &.placeholder {
      color: #bbb;
      font-style: italic;
    }
  }

  .area-input {
    width: 100%;
    border: 2px solid #1890ff;
    background: #e6f7ff;
    padding: 5px 10px;
    font-size: inherit;
    font-family: inherit;
    line-height: 1.8;
    outline: none;
    resize: vertical;
    border-radius: 4px;

    &:focus {
      background: #bae7ff;
    }
  }

  &.is-editing {
    padding: 0;
    border-color: transparent;
  }
}
</style>
