import { marked } from 'marked';

export const markdownContent = (markdown: boolean, value: Date | string | undefined) => {
  if (markdown && typeof value === "string") {
    return marked(value);
  }
  // Handle Date objects and other types
  const displayValue = value?.toString() || '';
  return `<span>${displayValue}</span>`;
}
