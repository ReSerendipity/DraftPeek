const fs = require('fs');
const P = 'C:/Users/Doro/.qwenworkcn/workspace/mtpvei2btqrqc5zn/gen8.js';
let h = fs.readFileSync(P, 'utf8');

// 1) locate the strip template literal appended at end of specsHtml
const startMark = " + `\n  <div class=\"family-hd\" data-component=\"family-header\"><h3>G34O · 强调弦色对比</h3>";
const s = h.indexOf(startMark);
if (s < 0) { console.error('strip start MISS'); process.exit(1); }
const endMark = '`;\n';
const e = h.indexOf(endMark, s + 10);
if (e < 0) { console.error('strip end MISS'); process.exit(1); }
const stripLit = h.slice(s + 3, e + 2); // the backtick literal itself
// remove the appended strip from specsHtml (replace ` + \`...\`;` with `;`)
h = h.slice(0, s) + ';' + h.slice(e + endMark.length);

// 2) define stripHtml const before specsHtml
h = h.replace('const specsHtml =', 'const stripHtml = ' + stripLit + ';\nconst specsHtml =');

// 3) inject strip right after the FIRST VARIANTS family (G34 family)
const mapOld = "+ VARIANTS.map(([title, sub, ids]) => '<div class=\"family-hd\" data-component=\"family-header\"><h3>' + title + '</h3><span class=\"caps mono\">' + sub + '</span></div>'\n    + ids.map(id => card(marks.find(m => m.id === id))).join('\\n')).join('\\n');";
const mapNew = "+ VARIANTS.map(([title, sub, ids], vi) => '<div class=\"family-hd\" data-component=\"family-header\"><h3>' + title + '</h3><span class=\"caps mono\">' + sub + '</span></div>'\n    + ids.map(id => card(marks.find(m => m.id === id))).join('\\n') + (vi === 0 ? stripHtml : '')).join('\\n');";
if (!h.includes(mapOld)) { console.error('map anchor MISS'); process.exit(1); }
h = h.replace(mapOld, mapNew);

fs.writeFileSync(P, h);
console.log('strip relocated: now directly below G34 family');
