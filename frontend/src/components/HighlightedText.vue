<script setup>
import { computed } from "vue";

const props = defineProps({ text: { default: "" }, query: { default: "" } });
const parts = computed(() => {
  const text = String(props.text || "");
  const query = String(props.query || "").trim();
  if (!query) return [{ text, matched: false }];
  const result = [];
  const lower = text.toLocaleLowerCase();
  const keyword = query.toLocaleLowerCase();
  let cursor = 0;
  let index = lower.indexOf(keyword);
  while (index !== -1) {
    if (index > cursor)
      result.push({ text: text.slice(cursor, index), matched: false });
    result.push({
      text: text.slice(index, index + query.length),
      matched: true,
    });
    cursor = index + query.length;
    index = lower.indexOf(keyword, cursor);
  }
  if (cursor < text.length)
    result.push({ text: text.slice(cursor), matched: false });
  return result;
});
</script>

<template>
  <template v-for="(part, index) in parts" :key="index"
    ><mark v-if="part.matched" class="keyword-highlight">{{ part.text }}</mark
    ><template v-else>{{ part.text }}</template></template
  >
</template>
