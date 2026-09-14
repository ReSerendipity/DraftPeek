const fs = require('fs');
const P = 'C:/Users/Doro/.qwenworkcn/workspace/mtpvei2btqrqc5zn/patch20.js';
let h = fs.readFileSync(P, 'utf8');
const oldLine = "const dS = h.indexOf('d=\"', a) + 3;";
const newLine = "const dS = h.indexOf('d=\"', h.indexOf('<path', a)) + 3;";
if (!h.includes(oldLine)) { console.error('line MISS'); process.exit(1); }
h = h.replace(oldLine, newLine);
fs.writeFileSync(P, h);
console.log('patch20 dS anchor fixed');
