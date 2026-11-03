// 保留原有 Markdown 子集；通过 textContent 构造节点，不解析模型返回的 HTML。
function appendInlineMarkdown(parent, source) {
  const pattern =
    /(`[^`]+`|\*\*[^*]+\*\*|~~[^~]+~~|\*[^*]+\*|\[[^\]]+\]\([^\s)]+\))/g;
  let cursor = 0;
  for (const match of source.matchAll(pattern)) {
    if (match.index > cursor) {
      parent.append(document.createTextNode(source.slice(cursor, match.index)));
    }
    const token = match[0];
    if (token.startsWith("`")) {
      const code = document.createElement("code");
      code.textContent = token.slice(1, -1);
      parent.append(code);
    } else if (token.startsWith("**")) {
      const strong = document.createElement("strong");
      strong.textContent = token.slice(2, -2);
      parent.append(strong);
    } else if (token.startsWith("~~")) {
      const deleted = document.createElement("del");
      deleted.textContent = token.slice(2, -2);
      parent.append(deleted);
    } else if (token.startsWith("*")) {
      const emphasis = document.createElement("em");
      emphasis.textContent = token.slice(1, -1);
      parent.append(emphasis);
    } else {
      const linkMatch = token.match(/^\[([^\]]+)]\(([^\s)]+)\)$/);
      const url = linkMatch?.[2] || "";
      if (/^(https?:|mailto:)/i.test(url)) {
        const link = document.createElement("a");
        link.href = url;
        link.target = "_blank";
        link.rel = "noopener noreferrer";
        link.textContent = linkMatch[1];
        parent.append(link);
      } else {
        parent.append(document.createTextNode(token));
      }
    }
    cursor = match.index + token.length;
  }
  if (cursor < source.length) {
    parent.append(document.createTextNode(source.slice(cursor)));
  }
}

function isTableDivider(line) {
  const cells = line
    .trim()
    .replace(/^\||\|$/g, "")
    .split("|");
  return (
    cells.length > 1 && cells.every((cell) => /^\s*:?-{3,}:?\s*$/.test(cell))
  );
}

function tableCells(line) {
  return line
    .trim()
    .replace(/^\||\|$/g, "")
    .split("|")
    .map((cell) => cell.trim());
}

export function renderMarkdown(markdown) {
  const fragment = document.createDocumentFragment();
  const lines = String(markdown || "")
    .replace(/\r\n?/g, "\n")
    .split("\n");
  let index = 0;

  while (index < lines.length) {
    const line = lines[index];
    if (!line.trim()) {
      index++;
      continue;
    }

    const fence = line.match(/^\s*```\s*([\w.+-]*)\s*$/);
    if (fence) {
      const block = document.createElement("div");
      block.className = "markdown-code-block";
      if (fence[1]) {
        const language = document.createElement("div");
        language.className = "markdown-code-language";
        language.textContent = fence[1];
        block.append(language);
      }
      const pre = document.createElement("pre");
      const code = document.createElement("code");
      const content = [];
      index++;
      while (index < lines.length && !/^\s*```\s*$/.test(lines[index])) {
        content.push(lines[index++]);
      }
      if (index < lines.length) index++;
      code.textContent = content.join("\n");
      pre.append(code);
      block.append(pre);
      fragment.append(block);
      continue;
    }

    const heading = line.match(/^(#{1,6})\s+(.+)$/);
    if (heading) {
      const level = Math.min(heading[1].length + 1, 6);
      const element = document.createElement(`h${level}`);
      appendInlineMarkdown(element, heading[2].trim());
      fragment.append(element);
      index++;
      continue;
    }

    if (/^\s*(---+|___+|\*\*\*+)\s*$/.test(line)) {
      fragment.append(document.createElement("hr"));
      index++;
      continue;
    }

    if (
      index + 1 < lines.length &&
      line.includes("|") &&
      isTableDivider(lines[index + 1])
    ) {
      const tableWrap = document.createElement("div");
      tableWrap.className = "markdown-table-wrap";
      const table = document.createElement("table");
      const thead = document.createElement("thead");
      const headRow = document.createElement("tr");
      tableCells(line).forEach((value) => {
        const cell = document.createElement("th");
        appendInlineMarkdown(cell, value);
        headRow.append(cell);
      });
      thead.append(headRow);
      table.append(thead);
      index += 2;
      const tbody = document.createElement("tbody");
      while (
        index < lines.length &&
        lines[index].includes("|") &&
        lines[index].trim()
      ) {
        const row = document.createElement("tr");
        tableCells(lines[index]).forEach((value) => {
          const cell = document.createElement("td");
          appendInlineMarkdown(cell, value);
          row.append(cell);
        });
        tbody.append(row);
        index++;
      }
      table.append(tbody);
      tableWrap.append(table);
      fragment.append(tableWrap);
      continue;
    }

    if (/^\s*>\s?/.test(line)) {
      const quote = document.createElement("blockquote");
      while (index < lines.length && /^\s*>\s?/.test(lines[index])) {
        if (quote.childNodes.length) quote.append(document.createElement("br"));
        appendInlineMarkdown(quote, lines[index].replace(/^\s*>\s?/, ""));
        index++;
      }
      fragment.append(quote);
      continue;
    }

    const unordered = line.match(/^\s*[-+*]\s+(.+)$/);
    const ordered = line.match(/^\s*\d+[.)]\s+(.+)$/);
    if (unordered || ordered) {
      const list = document.createElement(ordered ? "ol" : "ul");
      const matcher = ordered ? /^\s*\d+[.)]\s+(.+)$/ : /^\s*[-+*]\s+(.+)$/;
      while (index < lines.length) {
        const itemMatch = lines[index].match(matcher);
        if (!itemMatch) break;
        const item = document.createElement("li");
        appendInlineMarkdown(item, itemMatch[1]);
        list.append(item);
        index++;
      }
      fragment.append(list);
      continue;
    }

    const paragraph = document.createElement("p");
    while (index < lines.length && lines[index].trim()) {
      const nextLine = lines[index];
      if (paragraph.childNodes.length)
        paragraph.append(document.createElement("br"));
      appendInlineMarkdown(paragraph, nextLine.trim());
      index++;
      if (
        index < lines.length &&
        (/^(#{1,6})\s+/.test(lines[index]) ||
          /^\s*```/.test(lines[index]) ||
          /^\s*>\s?/.test(lines[index]) ||
          /^\s*([-+*]|\d+[.)])\s+/.test(lines[index]))
      )
        break;
    }
    fragment.append(paragraph);
  }
  return fragment;
}
