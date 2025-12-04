<template>
  <span 
    class="editable-field"
    :class="{ 
      'is-editing': isEditing, 
      'is-readonly': readonly,
      'is-empty': !modelValue 
    }"
    @dblclick="startEdit"
  >
    <template v-if="!isEditing">
      <span class="field-value" :class="{ placeholder: !modelValue }">
        {{ displayValue }}
      </span>
      <span class="field-underline"></span>
    </template>
    <template v-else>
      <input
        ref="inputRef"
        :type="type"
        :value="modelValue"
        @input="onInput"
        @blur="finishEdit"
        @keydown.enter="finishEdit"
        @keydown.esc="cancelEdit"
        class="field-input"
      />
    </template>
  </span>
</template>

<script setup>
import { ref, computed, nextTick } from 'vue'

const props = defineProps({
  modelValue: { type: [String, Number], default: '' },
  readonly: { type: Boolean, default: false },
  placeholder: { type: String, default: '点击编辑' },
  type: { type: String, default: 'text' }
})

const emit = defineEmits(['update:modelValue'])

const isEditing = ref(false)
const inputRef = ref(null)
const originalValue = ref('')

const displayValue = computed(() => {
  return props.modelValue || props.placeholder
})

const startEdit = () => {
  if (props.readonly) return
  originalValue.value = props.modelValue
  isEditing.value = true
  nextTick(() => {
    inputRef.value?.focus()
    inputRef.value?.select()
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
.editable-field {
  display: inline-flex;
  align-items: center;
  position: relative;
  min-width: 50px;
  cursor: text;

  &:not(.is-readonly):hover {
    .field-underline {
      border-color: #1890ff;
    }
  }

  &.is-readonly {
    cursor: default;
  }

  .field-value {
    padding: 2px 5px;
    
    &.placeholder {
      color: #bbb;
      font-style: italic;
    }
  }

  .field-underline {
    position: absolute;
    bottom: 0;
    left: 0;
    right: 0;
    border-bottom: 1px solid #333;
    transition: border-color 0.2s;
  }

  .field-input {
    border: none;
    border-bottom: 2px solid #1890ff;
    background: #e6f7ff;
    padding: 2px 5px;
    font-size: inherit;
    font-family: inherit;
    outline: none;
    min-width: 50px;
    width: 100%;

    &:focus {
      background: #bae7ff;
    }
  }

  &.is-editing {
    .field-underline {
      display: none;
    }
  }
}
</style>
