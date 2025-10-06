// Regex to match handle identifiers (e.g., 10378.1/1795071, 10.234/5678, 102.00/1234)
// Matches patterns like: number.number/number or number/number
const handleRegex = /\((10[2]?[\d.]*\/[\w.]+)\)/g;

// Function to replace handle identifiers in text with hyperlinks
// 'text' is the input string containing handle identifiers
// 'baseUrl' is the base URL to prepend to the handle (default is a sample URL)

export const hyperlinkFlexibleHandles = (text: string, baseUrl: string = 'https://static.prod.raid.org.au/raids/') => {
    console.log("Text to hyperlink:", text);
    console.log("Base URL:", baseUrl);
 return text.replace(handleRegex, (match, handle) => {
    // 'handle' is the captured group without parentheses
    return `(<a href="${baseUrl}${handle}" target="_blank">${handle}</a>)`;
  });
}
