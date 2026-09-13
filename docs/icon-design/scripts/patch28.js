const fs = require('fs');
const P = 'C:/Users/Doro/.qwenworkcn/workspace/mtpvei2btqrqc5zn/patch26.js';
let lines = fs.readFileSync(P, 'utf8').split('\n');
const startIdx = lines.findIndex(l => l.includes('const marker'));
const endIdx = lines.findIndex(l => l.includes("+ marker);"));
if (startIdx < 0 || endIdx < 0 || endIdx < startIdx) { console.error('block not found', startIdx, endIdx); process.exit(1); }
const replacement = [
  "const anchor = h.indexOf('</article>', h.indexOf('id=\"s-g34o\"'));",
  "if (anchor < 0) { console.error('g34o article MISS'); process.exit(1); }",
  "const close = anchor + '</article>'.length;",
  "const family = [",
  "  '',",
  "  '  <div class=\"family-hd\" data-component=\"family-header\"><h3>曲线家族 · 三种弯线美化</h3><span class=\"caps mono\">CURVE FAMILY</span></div>',",
  "].concat(cards).join('\\n');",
  "h = h.slice(0, close) + family + h.slice(close);",
];
lines = lines.slice(0, startIdx).concat(replacement, lines.slice(endIdx + 1));
fs.writeFileSync(P, lines.join('\n'));
console.log('patch26 anchor switched to g34o article end');
