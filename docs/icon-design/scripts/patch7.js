const fs = require('fs');
const P = 'C:/Users/Doro/.qwenworkcn/workspace/mtpvei2btqrqc5zn/gen7.js';
let h = fs.readFileSync(P, 'utf8');
const r = (a, b) => { if (!h.includes(a)) { console.error('MISS:', a.slice(0, 70)); process.exitCode = 1; } else h = h.split(a).join(b); };

r('[1,2,3,4,5,6].slice(0,6).map((_,i)=>', '[0,1,2,3,4].map(i=>');
r('<rect x="44" y="36" width="14" height="2.6" rx="1.3" fill="var(--m4)"/>', '<rect x="48.5" y="36.5" width="9" height="2.6" rx="1.3" fill="var(--m4)"/>');
r('<rect x="44" y="41" width="14" height="2.6" rx="1.3" fill="var(--m5)"/>', '<rect x="48.5" y="41.5" width="9" height="2.6" rx="1.3" fill="var(--m5)"/>');
r('<rect x="44" y="46" width="10" height="2.6" rx="1.3" fill="var(--m3)"/>', '<rect x="48.5" y="46.5" width="6.5" height="2.6" rx="1.3" fill="var(--m3)"/>');

fs.writeFileSync(P, h);
console.log('patched gen7.js');
