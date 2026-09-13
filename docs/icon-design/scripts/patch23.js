const fs = require('fs');
let h = fs.readFileSync('C:/Users/Doro/.qwenworkcn/workspace/mtpvei2btqrqc5zn/patch22.js', 'utf8');
const anchor = "let h = fs.readFileSync(P, 'utf8');";
if (!h.includes(anchor)) { console.error('MISS'); process.exit(1); }
const helper = "\nconst r = (x, y) => { if (!h.includes(x)) { console.error('MISS:', String(x).slice(0, 70)); process.exitCode = 1; } else h = h.split(x).join(y); };";
h = h.replace(anchor, anchor + helper);
fs.writeFileSync('C:/Users/Doro/.qwenworkcn/workspace/mtpvei2btqrqc5zn/patch22.js', h);
console.log('helper added');
