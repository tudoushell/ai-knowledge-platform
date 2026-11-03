<script setup>
import { ref, watch } from "vue";
import { renderMarkdown } from "../utils/markdown.js";

const props = defineProps({ content: { type: String, default: "" } });
const container = ref(null);
watch(
  [container, () => props.content],
  () => {
    container.value?.replaceChildren(renderMarkdown(props.content));
  },
  { flush: "post" },
);
</script>

<template>
  <div ref="container" class="markdown-body" />
</template>
