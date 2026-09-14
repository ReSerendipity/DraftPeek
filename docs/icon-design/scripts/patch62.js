const fs = require('fs');
const P = 'C:/Users/Doro/.qwenworkcn/workspace/mtpvei2btqrqc5zn/gen8.js';
let h = fs.readFileSync(P, 'utf8');
const oldStr = "    + ids.map(id => card(marks.find(m => m.id === id))).join('\\n') + (vi === 0 ? stripHtml : '')).join('\\n');";
const newStr = "    + ids.map(id => card(marks.find(m => m.id === id)) + (id === 'g34o' ? stripHtml : '')).join('\\n')).join('\\n');";
if (!h.includes(oldStr)) { console.error('anchor MISS'); process.exit(1); }
h = h.replace(oldStr, newStr);
fs.writeFileSync(P, h);
console.log('strip anchored to g34o card');
