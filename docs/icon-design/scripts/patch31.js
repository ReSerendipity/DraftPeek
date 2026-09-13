const fs = require('fs');
let h = fs.readFileSync('C:/Users/Doro/.qwenworkcn/workspace/mtpvei2btqrqc5zn/patch30.js', 'utf8');
// remove the static card-insertion block: from "const U = id" line through the marker-check exit
const s = h.indexOf('const U = id =>');
const e = h.indexOf("// --- marks entries + matrix rows ---");
if (s < 0 || e < 0 || e < s) { console.error('block bounds MISS', s, e); process.exit(1); }
h = h.slice(0, s) + h.slice(e);
fs.writeFileSync('C:/Users/Doro/.qwenworkcn/workspace/mtpvei2btqrqc5zn/patch30.js', h);
console.log('patch30 simplified: marks entries only (cards auto-render)');
