const fs = require('fs');
let h = fs.readFileSync('C:/Users/Doro/.qwenworkcn/workspace/mtpvei2btqrqc5zn/audit16.js', 'utf8');
const oldLine = 'for (const s of body.matchAll(/stroke-width="([\\d.]+)"/g)) { worst += num(s[1]) / 2; console.log(\'   strokeAdd\', s[1], \'\u2192 worst\', worst.toFixed(2)); }';
const newLine = 'for (const s of body.matchAll(/stroke-width="([\\d.]+)"/g)) worst = Math.max(worst, num(s[1]) / 2);';
if (!h.includes(oldLine)) { console.error('line MISS'); process.exit(1); }
h = h.replace(oldLine, newLine);
fs.writeFileSync('C:/Users/Doro/.qwenworkcn/workspace/mtpvei2btqrqc5zn/audit16.js', h);
console.log('audit16 stroke accumulation fixed');
