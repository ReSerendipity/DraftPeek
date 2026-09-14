const fs = require('fs');
const P = 'C:/Users/Doro/.qwenworkcn/workspace/mtpvei2btqrqc5zn/gen8.js';
let h = fs.readFileSync(P, 'utf8');

// 1) remove static G34-variant family block (header + 3 cards)
let s = h.indexOf('<div class="family-hd" data-component="family-header"><h3>G34 包络精修变体');
if (s < 0) { console.error('G34 family block MISS'); process.exit(1); }
const lastG34o = h.indexOf('</article>', h.indexOf('id="s-g34o"'));
const g34End = lastG34o + '</article>'.length;
h = h.slice(0, s) + h.slice(g34End);

// 2) remove static curve family block (header + 3 cards)
s = h.indexOf('<div class="family-hd" data-component="family-header"><h3>曲线家族');
if (s < 0) { console.error('curve family block MISS'); process.exit(1); }
const lastG42 = h.indexOf('</article>', h.indexOf('id="s-g42"'));
const cEnd = lastG42 + '</article>'.length;
h = h.slice(0, s) + h.slice(cEnd);

// 3) build grouped specs html before the html template
const snippet = `
const VARIANTS = [
  ['G34 包络精修变体', 'ENVELOPE VARIANTS · 排布 / 密度 / 构图', ['g34r', 'g34x', 'g34o']],
  ['曲线家族 · 三种弯线美化', 'CURVE FAMILY · 蓬瓣 / 渐细 / 内旋', ['g40', 'g41', 'g42']],
];
const specsHtml = marks.filter(m => !VARIANTS.some(v => v[2].includes(m.id))).map(card).join('\\n')
  + VARIANTS.map(([title, sub, ids]) => '<div class="family-hd" data-component="family-header"><h3>' + title + '</h3><span class="caps mono">' + sub + '</span></div>'
    + ids.map(id => card(marks.find(m => m.id === id))).join('\\n')).join('\\n');
`;
const htmlAnchor = 'const html = `<!doctype html>';
if (!h.includes(htmlAnchor)) { console.error('html anchor MISS'); process.exit(1); }
h = h.replace(htmlAnchor, snippet + '\n' + htmlAnchor);

// 4) swap placeholder
const ph = "${marks.map(card).join('\\n')}";
if (!h.includes(ph)) { console.error('placeholder MISS'); process.exit(1); }
h = h.replace(ph, '${specsHtml}');

fs.writeFileSync(P, h);
console.log('specs grouped by family at generation time');
