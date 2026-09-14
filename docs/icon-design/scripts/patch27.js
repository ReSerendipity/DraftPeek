const fs = require('fs');
let h = fs.readFileSync('C:/Users/Doro/.qwenworkcn/workspace/mtpvei2btqrqc5zn/patch26.js', 'utf8');
const oldBlock = `const marker = '</section>\\n\\n<section id="showcase"';
if (!h.includes(marker)) { console.error('marker MISS'); process.exit(1); }
h = h.replace(marker, `
  <div class="family-hd" data-component="family-header"><h3>曲线家族 · 三种弯线美化</h3><span class="caps mono">CURVE FAMILY · 密度 / 渐细 / 构图</span></div>
  \${cards.join('\\n')}` + marker);`;
if (!h.includes('const marker')) { console.error('marker block MISS'); process.exit(1); }
const newBlock = `const anchor = h.indexOf('</article>', h.indexOf('id="s-g34o"'));
if (anchor < 0) { console.error('g34o article MISS'); process.exit(1); }
const close = anchor + '</article>'.length;
const family = \`
  <div class="family-hd" data-component="family-header"><h3>曲线家族 · 三种弯线美化</h3><span class="caps mono">CURVE FAMILY · 密度 / 渐细 / 构图</span></div>
  \${cards.join('\\n')}\`;
h = h.slice(0, close) + family + h.slice(close);`;
h = h.replace(oldBlock, newBlock);
fs.writeFileSync('C:/Users/Doro/.qwenworkcn/workspace/mtpvei2btqrqc5zn/patch26.js', h);
console.log('patch26 insertion anchor switched to g34o article');
